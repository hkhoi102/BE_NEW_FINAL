from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List, Dict, Any, Tuple
import os
import re
import time
from collections import defaultdict
from contextlib import asynccontextmanager

# LangChain / LLM & tools
from langchain_google_genai import ChatGoogleGenerativeAI, GoogleGenerativeAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import DirectoryLoader, TextLoader
from langchain_community.utilities import SQLDatabase
from langchain_community.agent_toolkits.sql.base import create_sql_agent
from langchain.prompts import PromptTemplate, ChatPromptTemplate, MessagesPlaceholder
from langchain.memory import ConversationBufferMemory
from langchain.chains import ConversationChain
from langchain_core.messages import HumanMessage, AIMessage

# Load environment variables from .env if present
try:
	from dotenv import load_dotenv
	load_dotenv()
except Exception:
	pass

# Basic config via env vars
# Google Gemini API key
GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY", "")
# Fallback to OpenAI if needed (for backward compatibility)
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
# Use Gemini by default, fallback to OpenAI if GOOGLE_API_KEY not set
USE_GEMINI = os.getenv("USE_GEMINI", "true").lower() == "true" and bool(GOOGLE_API_KEY)

CHROMA_DIR = os.getenv("CHROMA_DIR", "./data/chroma")
DOCS_DIR = os.getenv("DOCS_DIR", "./data/docs")
# Cách 1: cross-database, kết nối vào 1 schema có quyền (vd product_db) và query db.table
MYSQL_URL = os.getenv("MYSQL_URL", "mysql+pymysql://reader:reader@172.17.0.1:3306/product_db")
# Gemini model name - use names from /gemini/models endpoint (without "models/" prefix)
# Recommended: gemini-2.5-flash (fast, stable) or gemini-2.5-pro (more powerful)
MODEL_NAME = os.getenv("MODEL_NAME", "gemini-2.5-flash")

# Globals to be initialized once during app startup
GLOBALS: Dict[str, Any] = {}

@asynccontextmanager
async def lifespan(app: FastAPI):
	# Initialize heavy resources once
	llm = get_llm()
	embeddings = get_embeddings()
	os.makedirs(CHROMA_DIR, exist_ok=True)
	vectorstore = Chroma(embedding_function=embeddings, persist_directory=CHROMA_DIR)
	retriever = vectorstore.as_retriever(search_type="similarity", search_kwargs={"k": 4})

	# Fixed system prompt for SQL Agent
	AGENT_PREFIX = """
Bạn là trợ lý thân thiện cho hệ thống siêu thị. Trả lời câu hỏi một cách tự nhiên, thân thiện như đang nói chuyện với khách hàng.

🚨 QUY TẮC - QUAN TRỌNG NHẤT:
1. Query SQL 1 lần
2. Có kết quả → Format thành câu trả lời bằng tiếng Việt
3. Dùng "Final Answer:" để kết thúc
4. KHÔNG query lại hoặc kiểm tra thêm

VÍ DỤ 1 - Hỏi giá:
Question: cửa hàng có redbull không
Thought: Tôi cần query giá RedBull
Action: sql_db_query
Action Input: SELECT p.name, u.name, pl.price FROM product_db.products p...
Observation: [('RedBull', 'Lon', 12000)]
Thought: I now know the final answer
Final Answer: Vâng, cửa hàng có RedBull (Lon): 12.000 VNĐ ạ!

VÍ DỤ 2 - Hỏi khuyến mãi mua X tặng Y:
Question: mua x tặng y có gì
Thought: Cần query khuyến mãi với JOIN để lấy tên sản phẩm
Action: sql_db_query
Action Input: SELECT ph.name, pd.condition_quantity, CONCAT(p_cond.name, ' (', u_cond.name, ')'), pd.free_quantity, CONCAT(p_gift.name, ' (', u_gift.name, ')') FROM promotion_db.promotion_headers ph JOIN...
Observation: [('KM tháng 11', 2, 'Coca-Cola (Lon)', 1, 'Coca-Cola (Lon)')]
Thought: I now know the final answer
Final Answer: Dạ, chương trình có ưu đãi: Mua 2 Coca-Cola (Lon) tặng 1 Coca-Cola (Lon) ạ!

VÍ DỤ 3 - Sản phẩm còn hàng + khuyến mãi:
Question: sản phẩm nào còn hàng và đang khuyến mãi
Thought: Cần JOIN promotion + inventory để tìm sản phẩm vừa có KM vừa còn hàng
Action: sql_db_query
Action Input: SELECT DISTINCT p.name, u.name, SUM(sb.available_quantity) FROM promotion_db.promotion_details pd JOIN...
Observation: [('Coca-Cola', 'Lon', 100), ('RedBull', 'Lon', 50)]
Thought: I now know the final answer
Final Answer: Hiện có 2 sản phẩm còn hàng đang khuyến mãi: Coca-Cola (Lon) còn 100, RedBull (Lon) còn 50 ạ!

🛒 HIỂU Ý KHÁCH HÀNG - QUAN TRỌNG:
- Câu hỏi "có [sản phẩm] không?" hoặc "cửa hàng có [sản phẩm] không?" → Khách hỏi về GIÁ BÁN
- Câu hỏi "còn [sản phẩm] không?" hoặc "còn hàng không?" → Khách hỏi về TỒN KHO
- VÍ DỤ: "cửa hàng có redbull không" = hỏi GIÁ, "còn redbull không" = hỏi TỒN KHO

📚 CÁC DATABASE VÀ TABLES CÓ SẴN (LUÔN DÙNG LOWERCASE):
1. product_db: products, product_categories, product_units, units, price_lists, price_headers, barcode_mapping
2. order_db: orders, order_details, return_orders, return_details
3. inventory_db: stock_balance, warehouses, stock_locations, inventory, stock_documents, stock_lots
4. promotion_db: promotion_headers, promotion_lines, promotion_details

💡 CÁCH DÙNG:
- Luôn prefix tên table bằng database_name.table_name (vd: product_db.products, promotion_db.promotion_headers)
- ⚠️ BẮT BUỘC dùng LOWERCASE cho tên database và table (product_db KHÔNG phải PRODUCT_DB)
- VÍ DỤ ĐÚNG: SELECT * FROM product_db.products WHERE name LIKE '%RedBull%'
- VÍ DỤ SAI: SELECT * FROM PRODUCT_DB.products (SAI CASE!)

⚡ QUAN TRỌNG - LUỒNG XỬ LÝ:
1. Khi tìm sản phẩm: Query trực tiếp product_db.products với LIKE '%tên%'
2. Khi kiểm tra tồn kho: JOIN ngay product_db.products → product_db.product_units → inventory_db.stock_balance trong 1 query duy nhất
3. KHÔNG cần dùng sql_db_schema với prefix database (sẽ lỗi)
4. Khi có kết quả từ query đầu tiên, HÃY DỪNG và trả lời ngay, ĐỪNG query thêm

🚨 QUY TẮC NGHIÊM NGẶT VỀ DỮ LIỆU:
1. CHỈ sử dụng dữ liệu TRỰC TIẾP từ kết quả truy vấn SQL
2. KHÔNG BAO GIỜ thêm thông tin, suy luận, hoặc dùng kiến thức ngoài kết quả DB
3. Nếu SQL trả về RỖNG/NULL → Trả lời một cách thân thiện: "Xin lỗi, hiện tại chúng tôi không có sản phẩm này trong hệ thống" hoặc "Hiện tại không tìm thấy thông tin này"
4. Nếu SQL có dữ liệu → Trả lời ĐÚNG với số liệu trong kết quả, KHÔNG làm tròn, KHÔNG ước lượng

💬 HƯỚNG DẪN TRẢ LỜI TỰ NHIÊN:
- Trả lời như đang nói chuyện với khách hàng, thân thiện, lịch sự
- Sử dụng ngôn ngữ tự nhiên, không quá kỹ thuật
- Thêm các từ ngữ giao tiếp như "Vâng", "Có", "Dạ", "Hiện tại", "Theo thông tin hệ thống"
- Nếu có nhiều sản phẩm, liệt kê một cách có tổ chức và dễ đọc

🎁 KHUYẾN MÃI - BẮT BUỘC PHẢI JOIN ĐỂ LẤY TÊN SẢN PHẨM:
Khi hỏi về khuyến mãi, PHẢI dùng query này để lấy tên sản phẩm cụ thể:

SELECT
  ph.name AS promo_name,
  pl.type,
  pd.discount_percent,
  pd.condition_quantity,
  pd.free_quantity,
  CONCAT(p_cond.name, ' (', u_cond.name, ')') AS condition_product,
  CONCAT(p_gift.name, ' (', u_gift.name, ')') AS gift_product
FROM promotion_db.promotion_headers ph
JOIN promotion_db.promotion_lines pl ON pl.promotion_header_id = ph.id
LEFT JOIN promotion_db.promotion_details pd ON pd.promotion_line_id = pl.id
LEFT JOIN product_db.product_units pu_cond ON pd.condition_product_unit_id = pu_cond.id
LEFT JOIN product_db.products p_cond ON pu_cond.product_id = p_cond.id
LEFT JOIN product_db.units u_cond ON pu_cond.unit_id = u_cond.id
LEFT JOIN product_db.product_units pu_gift ON pd.gift_product_unit_id = pu_gift.id
LEFT JOIN product_db.products p_gift ON pu_gift.product_id = p_gift.id
LEFT JOIN product_db.units u_gift ON pu_gift.unit_id = u_gift.id
WHERE ph.active = TRUE AND CURRENT_DATE BETWEEN ph.start_date AND ph.end_date
LIMIT 20

✅ Trả lời format: "Mua {condition_quantity} {condition_product} tặng {free_quantity} {gift_product}"
VD: "Mua 2 Coca-Cola (Lon) tặng 1 Coca-Cola (Lon)"

🚨 BẮT BUỘC VỀ PRODUCT_UNIT_ID:
- TUYỆT ĐỐI KHÔNG BAO GIỜ hiển thị số ID trong câu trả lời cho người dùng
- Khi có condition_product_unit_id, gift_product_unit_id, hoặc bất kỳ product_unit_id nào:
  → PHẢI JOIN sang product_db.product_units → product_db.products + product_db.units để lấy tên sản phẩm và đơn vị
  → Hiển thị format: "Tên sản phẩm (Tên đơn vị)" thay vì ID

📦 TỒN KHO - Query đơn giản:
- SELECT p.name, u.name AS unit, SUM(sb.quantity) AS qty
- FROM product_db.products p JOIN product_db.product_units pu ON p.id = pu.product_id
- JOIN product_db.units u ON pu.unit_id = u.id
- LEFT JOIN inventory_db.stock_balance sb ON pu.id = sb.product_unit_id
- WHERE p.name LIKE '%tên_sản_phẩm%' GROUP BY p.name, u.name LIMIT 20

💰 GIÁ BÁN - Query đơn giản:
- SELECT p.name, u.name AS unit, pl.price
- FROM product_db.products p JOIN product_db.product_units pu ON p.id = pu.product_id
- JOIN product_db.units u ON pu.unit_id = u.id
- JOIN product_db.price_lists pl ON pl.product_unit_id = pu.id AND pl.active = TRUE
- WHERE p.name LIKE '%tên_sản_phẩm%' LIMIT 20

🎯 SẢN PHẨM CÒN HÀNG + KHUYẾN MÃI (query từng bước):
Bước 1: Tìm sản phẩm trong khuyến mãi đang active
Bước 2: JOIN với stock_balance để check available_quantity > 0
Bước 3: Format: "Tên (đơn vị): còn X [đơn vị]"

⚠️ QUY TẮC:
- Query 1 lần → Có kết quả → Format → Trả lời → XONG
- Format giá: "Tên (đơn vị): 10.000 VNĐ" (có dấu chấm phân cách)
- Trả lời tiếng Việt, thân thiện như đang nói chuyện với khách hàng
""".strip()

	# Build SQL Agent once and wrap with a fixed prefix
	db = SQLDatabase.from_uri(MYSQL_URL, include_tables=None)

	agent = create_sql_agent(
		llm=llm,
		db=db,
		verbose=True,  # Bật để debug
		handle_parsing_errors=True,
		max_iterations=25,  # Tăng lên 25 cho query rất phức tạp (promotion + inventory)
		max_execution_time=120,  # Tăng lên 120s (2 phút)
		agent_executor_kwargs={
			"handle_parsing_errors": True,
			"return_intermediate_steps": False  # Giảm overhead
		}
	)

	class PrefixedAsyncAgent:
		def __init__(self, underlying, prefix: str):
			self._underlying = underlying
			self._prefix = prefix
		async def ainvoke(self, inputs: Dict[str, Any]) -> Any:
			# Thêm prefix vào input để hướng dẫn agent
			user_q = inputs.get("input", "")
			enhanced_input = f"{self._prefix}\n\n===== QUESTION =====\n{user_q}"
			return await self._underlying.ainvoke({"input": enhanced_input})

	sql_agent = PrefixedAsyncAgent(agent, AGENT_PREFIX)

	GLOBALS["llm"] = llm
	GLOBALS["embeddings"] = embeddings
	GLOBALS["vectorstore"] = vectorstore
	GLOBALS["retriever"] = retriever
	GLOBALS["db"] = db
	GLOBALS["sql_agent"] = sql_agent
	GLOBALS["agent_prefix"] = AGENT_PREFIX

	yield

app = FastAPI(title="Smart Retail AI Service", version="0.1.0", lifespan=lifespan)

# CORS middleware để frontend có thể gọi API
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Cho phép tất cả origins (có thể giới hạn trong production)
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# In-memory conversation storage with size limit (tối ưu memory)
def create_limited_memory():
    """Tạo memory với giới hạn messages để tránh memory leak."""
    from langchain.memory import ConversationBufferWindowMemory
    # Chỉ giữ 10 messages gần nhất (5 cặp hỏi-đáp) để giảm memory và latency
    return ConversationBufferWindowMemory(k=10, return_messages=True)

conversation_memories: Dict[str, ConversationBufferMemory] = defaultdict(create_limited_memory)


class ChatRequest(BaseModel):
    question: str
    user_id: Optional[str] = None
    top_k: int = 4
    conversation_history: Optional[List[Dict[str, str]]] = None  # [{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]


class IngestRequest(BaseModel):
    paths: Optional[List[str]] = None  # if None, use DOCS_DIR


def get_llm():
    """Get LLM - Gemini or OpenAI based on USE_GEMINI flag"""
    if USE_GEMINI:
        if not GOOGLE_API_KEY:
            raise RuntimeError("GOOGLE_API_KEY is not set. Set GOOGLE_API_KEY in .env file.")

        # Model name from API has prefix "models/", but langchain expects without prefix
        # Remove "models/" prefix if present
        model_name = MODEL_NAME
        if model_name.startswith("models/"):
            model_name = model_name.replace("models/", "")

        return ChatGoogleGenerativeAI(
            model=model_name,
            temperature=0,
            google_api_key=GOOGLE_API_KEY
        )
    else:
        # Fallback to OpenAI (backward compatibility)
        if not OPENAI_API_KEY:
            raise RuntimeError("OPENAI_API_KEY is not set")
        from langchain_openai import ChatOpenAI
        return ChatOpenAI(
            model=MODEL_NAME,
            temperature=0,
            openai_api_key=OPENAI_API_KEY
        )


def get_embeddings():
    """Get embeddings - Gemini or OpenAI based on USE_GEMINI flag"""
    if USE_GEMINI:
        if not GOOGLE_API_KEY:
            raise RuntimeError("GOOGLE_API_KEY is not set. Set GOOGLE_API_KEY in .env file.")
        return GoogleGenerativeAIEmbeddings(
            model="models/embedding-001",  # Gemini embedding model
            google_api_key=GOOGLE_API_KEY
        )
    else:
        # Fallback to OpenAI embeddings (backward compatibility)
        if not OPENAI_API_KEY:
            raise RuntimeError("OPENAI_API_KEY is not set")
        from langchain_openai import OpenAIEmbeddings
        return OpenAIEmbeddings(openai_api_key=OPENAI_API_KEY)


def ensure_vectorstore() -> Chroma:
	embeddings = GLOBALS.get("embeddings") or get_embeddings()
	os.makedirs(CHROMA_DIR, exist_ok=True)
	return GLOBALS.get("vectorstore") or Chroma(embedding_function=embeddings, persist_directory=CHROMA_DIR)


def load_and_split(paths: Optional[List[str]] = None):
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=1200, chunk_overlap=150)
    documents = []
    if paths:
        for p in paths:
            if os.path.isdir(p):
                documents.extend(DirectoryLoader(p, glob='**/*', loader_cls=TextLoader, show_progress=True).load())
            elif os.path.isfile(p):
                documents.extend(TextLoader(p).load())
    else:
        documents.extend(DirectoryLoader(DOCS_DIR, glob='**/*', loader_cls=TextLoader, show_progress=True).load())
    return text_splitter.split_documents(documents)


def upsert_documents(paths: Optional[List[str]] = None) -> int:
    docs = load_and_split(paths)
    vs = ensure_vectorstore()
    vs.add_documents(docs)
    vs.persist()
    return len(docs)


async def retrieve_context(question: str, top_k: int) -> str:
	"""Retrieve context using cached retriever from GLOBALS."""
	retriever = GLOBALS.get("retriever")
	if not retriever:
		vs = ensure_vectorstore()
		retriever = vs.as_retriever(search_type="similarity", search_kwargs={"k": top_k})
	# Use ainvoke instead of deprecated aget_relevant_documents
	docs = await retriever.ainvoke(question)
	snippets = [d.page_content for d in docs]
	return "\n\n".join(snippets)


def build_sql_agent():
	# SQL Agent is initialized once in lifespan and stored in GLOBALS
	return GLOBALS.get("sql_agent")


def validate_sql_response(response: str, question: str) -> str:
    """
    Validate nhanh response từ SQL agent.
    Chỉ check các pattern suy đoán rõ ràng, trust agent prompt để xử lý phần còn lại.
    """
    response_lower = response.lower()

    # Quick check: nếu có từ suy đoán rõ ràng và không có số liệu
    speculation_patterns = ["có lẽ", "có khả năng", "ước tính", "theo kiến thức", "dựa trên kinh nghiệm"]
    has_speculation = any(p in response_lower for p in speculation_patterns)
    has_data = bool(re.search(r'\d+', response))

    if has_speculation and not has_data:
        return "Xin lỗi, hiện tại chúng tôi không tìm thấy thông tin này trong hệ thống."

    return response


def is_sql_question_using_embeddings(question: str) -> bool:
	"""[Removed] Network-based embedding routing disabled."""
	return False


def is_sql_question_using_llm(question: str) -> bool:
	"""[Removed] LLM-based routing disabled."""
	return False


def is_sql_question_using_keywords(q: str) -> bool:
    """Nhận diện SQL question bằng keywords với ưu tiên cao cho câu hỏi về sản phẩm"""
    ql = q.lower()

    # ⚡ PRIORITY 1: Câu hỏi ngắn với pattern "có ... không" hoặc "còn ... không" → LUÔN LÀ SQL
    # VD: "có redbull không", "còn coca không", "pepsi còn không"
    short_question_patterns = ["có ", "còn ", "bán ", "giá ", "price"]
    word_count = len(q.split())
    if word_count <= 10 and any(p in ql for p in short_question_patterns):
        return True

    # ⚡ PRIORITY 2: Keywords chính xác
    sql_keywords = [
        "doanh số", "sales", "doanh thu", "revenue", "lợi nhuận", "profit",
        "tồn kho", "inventory", "stock", "kho", "giá", "giá bán", "bao nhiêu tiền", "bao nhiêu vnđ", "bao nhiêu vnd", "price",
        "đơn hàng", "orders", "số lượng", "quantity", "tháng", "quý", "năm",
        "có bao nhiêu", "how many", "how much", "count", "tổng", "total",
        "sản phẩm", "product", "khách hàng", "customer", "bán chạy", "best seller",
        "trong hệ thống", "in the system", "hiện tại", "current", "hiện có",
        "thống kê", "statistics", "báo cáo", "report", "phân tích", "analysis",
        "còn hàng", "có hàng", "hàng còn", "còn không", "còn tồn", "còn trong kho",
        "kiểm tra", "check", "kiểm tra tồn", "kiểm tra kho", "còn bao nhiêu",
        "còn lại", "available", "availability", "còn sẵn",
        "khuyến mãi", "khuyến mại", "ưu đãi", "promotion", "giảm giá", "voucher", "mã giảm giá", "chương trình",
        "đang active", "đang áp dụng", "end_date", "start_date",
        "mua x tặng y", "mua tặng", "tặng kèm", "buy x get y", "mua bao nhiêu tặng", "combo",
        # Câu hỏi về sản phẩm
        "cửa hàng có", "shop có", "siêu thị có", "bán không", "có bán",
        "có sản phẩm", "có mặt hàng", "có loại", "có món", "có mã",
        # Tên sản phẩm phổ biến
        "nước ngọt", "nước suối", "bia", "rượu", "snack", "bánh", "kẹo", "sữa", "cà phê", "trà"
    ]
    return any(k in ql for k in sql_keywords)


def is_sql_question(q: str) -> bool:
	"""Nhận diện SQL question bằng từ khóa (đơn giản, nhanh, không network)."""
	return is_sql_question_using_keywords(q)


def detect_sql_category(q: str) -> str:
	"""
	Xác định loại câu hỏi để hướng agent chọn đúng database/tables.
	Trả về một trong: PRODUCT, PROMOTION, INVENTORY, PRICE, ORDER, GENERIC
	"""
	ql = q.lower()
	# Promotion first to disambiguate words like "giảm giá" vs price
	if any(k in ql for k in ["khuyến mãi", "khuyến mại", "ưu đãi", "promotion", "voucher", "mã giảm giá", "chương trình", "đang active", "đang áp dụng"]):
		return "PROMOTION"
	# Inventory related
	if any(k in ql for k in ["tồn kho", "inventory", "stock", "còn hàng", "có hàng", "hàng còn", "còn không", "còn tồn", "còn trong kho", "kiểm tra tồn", "kiểm tra kho", "available", "availability"]):
		return "INVENTORY"
	# Price related
	if any(k in ql for k in ["giá bán", "giá", "bao nhiêu tiền", "bao nhiêu vnđ", "bao nhiêu vnd", "price"]):
		return "PRICE"
	# Order related
	if any(k in ql for k in ["đơn hàng", "orders", "order", "trả hàng", "return"]):
		return "ORDER"
	# Product catch-all
	if any(k in ql for k in ["sản phẩm", "product", "bán chạy", "best seller", "mặt hàng", "mã vạch", "barcode"]):
		return "PRODUCT"
	return "GENERIC"


def is_quota_error(error: Exception) -> Tuple[bool, Optional[str]]:
    """
    Kiểm tra xem lỗi có phải là quota/overload error không
    Returns: (is_quota_error, retry_after_seconds_str)
    """
    error_str = str(error).lower()
    error_type = type(error).__name__

    # Check 503 ServiceUnavailable (model overloaded)
    if "ServiceUnavailable" in error_type or "503" in error_str or "overloaded" in error_str:
        return True, "10"  # Retry after 10 seconds for overload

    # Check ResourceExhausted exception
    if "ResourceExhausted" in error_type or "resourceexhausted" in error_str:
        # Parse retry delay từ error message
        retry_match = re.search(r'retry in (\d+\.?\d*)s', error_str, re.IGNORECASE)
        if retry_match:
            retry_after = retry_match.group(1)
            return True, retry_after
        return True, "30"  # Default 30 seconds

    # Check 429 status code
    if "429" in error_str or "quota" in error_str:
        retry_match = re.search(r'retry in (\d+\.?\d*)s', error_str, re.IGNORECASE)
        if retry_match:
            retry_after = retry_match.group(1)
            return True, retry_after
        return True, "30"

    # Check quota exceeded message
    if "exceeded" in error_str and ("quota" in error_str or "limit" in error_str):
        retry_match = re.search(r'retry in (\d+\.?\d*)s', error_str, re.IGNORECASE)
        if retry_match:
            retry_after = retry_match.group(1)
            return True, retry_after
        return True, "30"

    return False, None


def format_quota_error_message(retry_after: Optional[str] = None, is_overload: bool = False) -> str:
    """Tạo thông báo lỗi quota/overload thân thiện với người dùng"""

    if is_overload:
        base_msg = """⚠️ **API đang quá tải**

Google Gemini API hiện đang bị quá tải do có quá nhiều người sử dụng cùng lúc.

Vui lòng:
- Đợi 10-30 giây rồi thử lại
- Hoặc thử lại sau vài phút"""
    else:
        base_msg = """⚠️ **Đã vượt quá giới hạn API**

Hiện tại bạn đã sử dụng hết quota miễn phí của Google Gemini API (10 requests/phút).

Vui lòng:
- Đợi khoảng 1 phút rồi thử lại
- Hoặc nâng cấp lên plan có trả phí để có quota cao hơn
- Kiểm tra usage tại: https://ai.dev/usage?tab=rate-limit"""

    if retry_after:
        try:
            seconds = float(retry_after)
            minutes = int(seconds // 60)
            remaining_seconds = int(seconds % 60)
            if minutes > 0:
                wait_time = f"{minutes} phút {remaining_seconds} giây"
            else:
                wait_time = f"{remaining_seconds} giây"
            base_msg += f"\n\n⏱️ Vui lòng đợi **{wait_time}** trước khi thử lại."
        except:
            base_msg += f"\n\n⏱️ Vui lòng đợi khoảng {retry_after} giây trước khi thử lại."

    return base_msg


RAG_PROMPT = PromptTemplate.from_template(
    """
    Bạn là trợ lý cho hệ thống siêu thị. Dựa trên ngữ cảnh sau, trả lời rõ ràng, ngắn gọn.
    Nếu thông tin không có trong ngữ cảnh, hãy nói bạn không chắc và đề xuất bước tiếp theo.

    [Ngữ cảnh]
    {context}

    [Câu hỏi]
    {question}
    """
)


@app.post("/ingest")
def ingest(req: IngestRequest) -> Dict[str, Any]:
    try:
        count = upsert_documents(req.paths)
        return {"indexed_chunks": count, "persist_directory": CHROMA_DIR}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/chat")
async def chat(req: ChatRequest) -> Dict[str, Any]:
    question = req.question.strip()
    user_id = req.user_id or "default"

    # Lấy hoặc tạo conversation memory cho user
    memory = conversation_memories[user_id]

    # Nếu có conversation_history từ client, load vào memory
    if req.conversation_history:
        memory.chat_memory.clear()
        for msg in req.conversation_history:
            role = msg.get("role", "")
            content = msg.get("content", "")
            if role == "user":
                memory.chat_memory.add_user_message(content)
            elif role == "assistant":
                memory.chat_memory.add_ai_message(content)

    if is_sql_question(question):
        # Route SQL questions to the SQL agent
        try:
                    agent = GLOBALS.get("sql_agent") or build_sql_agent()
                    category = detect_sql_category(question)
                    # Thêm hint để agent chọn đúng DB/tables
                    sql_input = f"[CATEGORY: {category}] {question}"
                    sql_answer = await agent.ainvoke({"input": sql_input})
                    answer_text = sql_answer["output"] if isinstance(sql_answer, dict) else sql_answer

                    # Validate response để đảm bảo chỉ dùng dữ liệu từ DB
                    answer_text = validate_sql_response(answer_text, question)

                    # Lưu vào memory
                    memory.chat_memory.add_user_message(question)
                    memory.chat_memory.add_ai_message(answer_text)

                    return {"answer": answer_text, "route": "sql", "conversation_id": user_id}
        except Exception as sql_error:
                # In lỗi để debug
                print(f"❌ SQL Agent Error: {type(sql_error).__name__}: {str(sql_error)}")
                import traceback
                traceback.print_exc()

                # Kiểm tra xem có phải lỗi quota/overload không
                is_quota, retry_after = is_quota_error(sql_error)
                if is_quota:
                    # Check if it's overload error
                    is_overload = "ServiceUnavailable" in type(sql_error).__name__ or "503" in str(sql_error) or "overloaded" in str(sql_error).lower()
                    error_msg = format_quota_error_message(retry_after, is_overload=is_overload)
                    return {
                        "answer": error_msg,
                        "route": "quota_error" if not is_overload else "overload_error",
                        "error": "quota_exceeded" if not is_overload else "api_overloaded",
                        "retry_after": retry_after,
                        "conversation_id": user_id
                    }

                # Nếu không phải quota error, fallback về LLM trực tiếp
                error_msg = str(sql_error)
                llm = GLOBALS.get("llm") or get_llm()

                # Dùng conversation history trong fallback
                messages = memory.chat_memory.messages if hasattr(memory.chat_memory, 'messages') else []
                prompt_template = ChatPromptTemplate.from_messages([
                    ("system", """Bạn là trợ lý SQL cho hệ thống siêu thị. Bạn có thể truy vấn từ nhiều database bằng cú pháp database_name.table_name:

=== PRODUCT_DB ===
- products: id, name, description, code, category_id, active, created_at, updated_at
- product_categories: id, name, description, active, image_url
- product_units: id, product_id, unit_id, conversion_rate, is_default, active, image_url
- units: id, name, description, is_default, active
- price_lists: id, product_unit_id, price_header_id, price, active, created_at
- price_headers: id, name, start_date, end_date, active
- barcode_mapping: id, product_unit_id, barcode

=== ORDER_DB ===
- orders: id, order_code, customer_id, total_amount, discount_amount, status, promotion_applied_id, warehouse_id, stock_location_id, outbound_document_id, shipping_address, delivery_method, phone_number, created_at
- order_details: id, order_id, product_unit_id, quantity, unit_price, subtotal, stock_lot_id
- return_orders: id, order_id, return_code, customer_id, total_amount, status, created_at
- return_details: id, return_order_id, order_detail_id, product_unit_id, quantity, unit_price, subtotal

=== INVENTORY_DB ===
- stock_balance: id, product_unit_id, stock_location_id, warehouse_id, quantity, reserved_quantity, available_quantity, last_updated_at, created_at
  * quantity: tổng số lượng tồn kho
  * reserved_quantity: số lượng đã được đặt trước (reserved)
  * available_quantity: số lượng có sẵn = quantity - reserved_quantity
- warehouses: id, name, description, address, phone, contact_person, active, created_at, updated_at
- stock_locations: id, name, description, warehouse_id, zone, aisle, rack, level, position, active, created_at, updated_at
- inventory: id, transaction_type (IMPORT/EXPORT/ADJUST/TRANSFER), quantity, transaction_date, note, reference_number, product_unit_id, stock_location_id, warehouse_id, created_at, updated_at
- stock_documents: id, type, status, reference_number, warehouse_id, stock_location_id, created_at, approved_at
- stock_lots: id, product_unit_id, warehouse_id, stock_location_id, lot_number, current_quantity, reserved_quantity, available_quantity, status, expiry_date

=== PROMOTION_DB ===
 - promotion_headers: id, name, start_date, end_date, active, created_at
 - promotion_lines: id, promotion_header_id, target_type (PRODUCT/CATEGORY/CUSTOMER), target_id, start_date, end_date, active, type
 - promotion_details: id, promotion_line_id, discount_percent, discount_amount, condition_quantity, free_quantity, condition_product_unit_id, gift_product_unit_id, min_amount, max_discount, active

QUAN TRỌNG về KHUYẾN MÃI:
- Khi có condition_product_unit_id, gift_product_unit_id → PHẢI JOIN sang product_db.product_units → product_db.products + product_db.units để lấy tên sản phẩm
- TUYỆT ĐỐI KHÔNG hiển thị ID trong câu trả lời, chỉ hiển thị "Tên sản phẩm (Đơn vị)"
- type = DISCOUNT_PERCENT → "Giảm theo %", DISCOUNT_AMOUNT → "Giảm tiền", BUY_X_GET_Y → "Mua x tặng y"
- min_amount → "Đơn tối thiểu", max_discount → "Đơn tối đa"

🛒 HIỂU Ý KHÁCH HÀNG:
- "có [sản phẩm] không?" hoặc "cửa hàng có [sản phẩm] không?" → Hỏi về GIÁ BÁN
- "còn [sản phẩm] không?" hoặc "còn hàng không?" → Hỏi về TỒN KHO
- VÍ DỤ: "cửa hàng có redbull không" = trả lời GIÁ, "còn redbull không" = trả lời TỒN KHO

QUAN TRỌNG về GIÁ BÁN:
- Query: JOIN product_db.products → product_db.product_units → product_db.price_lists (WHERE active = TRUE)
- Trả lời: "RedBull (Lon): 12.000 VNĐ" hoặc thêm tồn kho "(còn 1019 lon)"

QUAN TRỌNG về TỒN KHO:
- Query: JOIN product_db.products → product_db.product_units → inventory_db.stock_balance
- Trả lời: "Còn 1019 lon, 59 lốc trong kho"

📋 FORMAT GIÁ - BẮT BUỘC:
- Giá: "RedBull (Lon): 12.000 VNĐ" (LUÔN có dấu CHẤM: 10.000, 50.000, 1.500.000)
- KHÔNG viết: 10000, 50000 (thiếu dấu chấm)
- Danh sách: mỗi dòng "• Tên (đơn vị): giá VNĐ"

Format chuẩn: "Tên (đơn vị): giá VNĐ" với dấu chấm phân cách hàng nghìn."""),
                    MessagesPlaceholder(variable_name="history"),
                    ("human", "{question}\n\nLỗi SQL Agent: {error}")
                ])

                prompt = prompt_template.format_messages(
                    question=question,
                    error=error_msg[:200],
                    history=messages[-3:] if len(messages) > 3 else messages  # Giảm context để nhanh hơn
                )

                try:
                    response = await llm.ainvoke(prompt)
                    text = response.content if hasattr(response, "content") else str(response)
                except Exception as llm_error:
                    # Kiểm tra lại quota/overload error trong LLM call
                    is_quota, retry_after = is_quota_error(llm_error)
                    if is_quota:
                        is_overload = "ServiceUnavailable" in type(llm_error).__name__ or "503" in str(llm_error) or "overloaded" in str(llm_error).lower()
                        error_msg = format_quota_error_message(retry_after, is_overload=is_overload)
                        return {
                            "answer": error_msg,
                            "route": "quota_error" if not is_overload else "overload_error",
                            "error": "quota_exceeded" if not is_overload else "api_overloaded",
                            "retry_after": retry_after,
                            "conversation_id": user_id
                        }
                    raise

                # Lưu vào memory
                memory.chat_memory.add_user_message(question)
                memory.chat_memory.add_ai_message(text)

                return {
                    "answer": f"{text}\n\n⚠️ Lưu ý: SQL Agent gặp lỗi. Đã fallback về LLM trực tiếp.",
                    "route": "sql_fallback",
                    "error": error_msg[:200],
                    "conversation_id": user_id
                }
    else:
        # RAG cần embeddings (tốn OpenAI credits), nếu hết quota thì fallback về SQL hoặc LLM trực tiếp
        try:
            llm = GLOBALS.get("llm") or get_llm()
            context = await retrieve_context(question, top_k=req.top_k)

            # Tạo prompt với conversation history
            messages = memory.chat_memory.messages if hasattr(memory.chat_memory, 'messages') else []

            # Build prompt với context và history
            prompt_template = ChatPromptTemplate.from_messages([
                ("system", """Bạn là trợ lý cho hệ thống siêu thị. Dựa trên ngữ cảnh và lịch sử hội thoại, trả lời rõ ràng, ngắn gọn.

📋 FORMAT GIÁ - BẮT BUỘC:
- Giá sản phẩm: "Tên (đơn vị): 12.000 VNĐ" (LUÔN có dấu CHẤM phân cách hàng nghìn)
- VÍ DỤ ĐÚNG: "RedBull (Lon): 12.000 VNĐ", "Coca (Chai): 15.000 đồng"
- VÍ DỤ SAI: "RedBull (Lon): 12000 VNĐ" (thiếu dấu chấm)
- Danh sách: mỗi dòng "• Tên (đơn vị): giá VNĐ"

Format chuẩn: "Tên (đơn vị): giá VNĐ" với dấu chấm phân cách hàng nghìn."""),
                MessagesPlaceholder(variable_name="history"),
                ("human", """Ngữ cảnh từ tài liệu:
{context}

Câu hỏi: {question}""")
            ])

            prompt = prompt_template.format_messages(
                context=context,
                question=question,
                history=messages[-5:] if len(messages) > 5 else messages  # Giảm xuống 5 messages để nhanh hơn
            )

            response = await llm.ainvoke(prompt)
            text = response.content if hasattr(response, "content") else str(response)

            # Lưu vào memory
            memory.chat_memory.add_user_message(question)
            memory.chat_memory.add_ai_message(text)

            return {"answer": text, "route": "rag", "conversation_id": user_id}
        except Exception as rag_error:
            # Kiểm tra xem có phải lỗi quota/overload không
            is_quota, retry_after = is_quota_error(rag_error)
            if is_quota:
                is_overload = "ServiceUnavailable" in type(rag_error).__name__ or "503" in str(rag_error) or "overloaded" in str(rag_error).lower()
                error_msg = format_quota_error_message(retry_after, is_overload=is_overload)
                return {
                    "answer": error_msg,
                    "route": "quota_error" if not is_overload else "overload_error",
                    "error": "quota_exceeded" if not is_overload else "api_overloaded",
                    "retry_after": retry_after,
                    "conversation_id": user_id
                }

            # Nếu RAG lỗi (không phải quota), fallback về LLM trực tiếp (không cần embeddings)
            llm = GLOBALS.get("llm") or get_llm()

            # Dùng conversation history
            messages = memory.chat_memory.messages if hasattr(memory.chat_memory, 'messages') else []
            prompt_template = ChatPromptTemplate.from_messages([
                ("system", """Bạn là trợ lý cho hệ thống siêu thị. Trả lời câu hỏi dựa trên lịch sử hội thoại.

📋 FORMAT GIÁ - BẮT BUỘC:
- Giá sản phẩm: "Tên (đơn vị): 12.000 VNĐ" (LUÔN có dấu CHẤM phân cách hàng nghìn)
- VÍ DỤ ĐÚNG: "RedBull (Lon): 12.000 VNĐ", "Coca (Chai): 15.000 đồng"
- VÍ DỤ SAI: "RedBull (Lon): 12000 VNĐ" (thiếu dấu chấm)
- Danh sách: mỗi dòng "• Tên (đơn vị): giá VNĐ"

Format chuẩn: "Tên (đơn vị): giá VNĐ" với dấu chấm phân cách hàng nghìn."""),
                MessagesPlaceholder(variable_name="history"),
                ("human", "{question}")
            ])

            prompt = prompt_template.format_messages(
                question=question,
                history=messages[-5:] if len(messages) > 5 else messages  # Giảm xuống 5 messages
            )

            try:
                response = await llm.ainvoke(prompt)
                text = response.content if hasattr(response, "content") else str(response)
            except Exception as llm_error:
                # Kiểm tra lại quota/overload error trong LLM call
                is_quota, retry_after = is_quota_error(llm_error)
                if is_quota:
                    is_overload = "ServiceUnavailable" in type(llm_error).__name__ or "503" in str(llm_error) or "overloaded" in str(llm_error).lower()
                    error_msg = format_quota_error_message(retry_after, is_overload=is_overload)
                    return {
                        "answer": error_msg,
                        "route": "quota_error" if not is_overload else "overload_error",
                        "error": "quota_exceeded" if not is_overload else "api_overloaded",
                        "retry_after": retry_after,
                        "conversation_id": user_id
                    }
                raise

            # Lưu vào memory
            memory.chat_memory.add_user_message(question)
            memory.chat_memory.add_ai_message(text)

            return {
                "answer": f"{text}\n\n⚠️ Lưu ý: RAG embeddings đang tạm thời không khả dụng. Bạn có thể thử câu hỏi về SQL/dữ liệu.",
                "route": "llm_fallback",
                "conversation_id": user_id
            }



@app.get("/conversation/{user_id}/history")
def get_conversation_history(user_id: str) -> Dict[str, Any]:
    """Lấy lịch sử conversation của user"""
    memory = conversation_memories.get(user_id)
    if not memory or not hasattr(memory.chat_memory, 'messages') or len(memory.chat_memory.messages) == 0:
        return {"history": [], "user_id": user_id, "message": "No conversation history found"}

    messages = memory.chat_memory.messages
    history = []
    for msg in messages:
        if hasattr(msg, 'content'):
            # Check if it's HumanMessage or AIMessage
            if hasattr(msg, '__class__'):
                class_name = msg.__class__.__name__
                role = "user" if "Human" in class_name else "assistant"
            else:
                role = "user"  # default
            history.append({"role": role, "content": msg.content})

    return {"history": history, "user_id": user_id, "count": len(history)}


@app.delete("/conversation/{user_id}")
def clear_conversation(user_id: str) -> Dict[str, str]:
    """Xóa conversation history của user"""
    if user_id in conversation_memories:
        conversation_memories[user_id].chat_memory.clear()
        return {"message": f"Conversation history cleared for user {user_id}"}
    return {"message": f"No conversation found for user {user_id}"}


@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "ok"}


@app.get("/schema")
def get_schema() -> Dict[str, Any]:
    """Lấy thông tin schema của database để debug"""
    try:
        db = SQLDatabase.from_uri(MYSQL_URL)
        return {
            "dialect": db.dialect,
            "tables": db.get_usable_table_names(),
            "sample_query": "SELECT table_name FROM information_schema.tables WHERE table_schema IN ('product_db', 'order_db', 'inventory_db') LIMIT 10"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/debug/is-sql")
def debug_is_sql(question: str) -> Dict[str, Any]:
    """Debug endpoint để kiểm tra xem câu hỏi có được nhận diện là SQL không"""
    try:
        keyword_match = is_sql_question_using_keywords(question)
        final_result = is_sql_question(question)

        return {
            "question": question,
            "keyword_match": keyword_match,
            "final_result": final_result,
            "matched_keywords": [k for k in [
                "doanh số", "sales", "doanh thu", "revenue", "lợi nhuận", "profit",
                "tồn kho", "inventory", "stock", "kho", "giá bán", "price",
                "đơn hàng", "orders", "số lượng", "quantity", "tháng", "quý", "năm",
                "có bao nhiêu", "how many", "how much", "count", "tổng", "total",
                "sản phẩm", "product", "khách hàng", "customer", "bán chạy", "best seller",
                "trong hệ thống", "in the system", "hiện tại", "current", "hiện có",
                "thống kê", "statistics", "báo cáo", "report", "phân tích", "analysis",
                "còn hàng", "có hàng", "hàng còn", "còn không", "còn tồn", "còn trong kho",
                "kiểm tra", "check", "kiểm tra tồn", "kiểm tra kho", "còn bao nhiêu",
                "còn lại", "available", "availability", "còn sẵn"
            ] if k in question.lower()]
        }
    except Exception as e:
        return {"error": str(e)}


@app.get("/gemini/models")
def list_gemini_models() -> Dict[str, Any]:
    """List available Gemini models (for debugging)"""
    try:
        import google.generativeai as genai
        genai.configure(api_key=GOOGLE_API_KEY)
        models = genai.list_models()
        available = []
        for model in models:
            if 'generateContent' in model.supported_generation_methods:
                available.append({
                    "name": model.name,
                    "display_name": model.display_name,
                    "description": model.description
                })
        return {
            "available_models": available,
            "current_model": MODEL_NAME,
            "tip": "Set MODEL_NAME in .env to one of the names above (REMOVE 'models/' prefix). Recommended: gemini-2.5-flash or gemini-2.5-pro"
        }
    except Exception as e:
        return {
            "error": str(e),
            "tip": "Make sure GOOGLE_API_KEY is set correctly"
        }


