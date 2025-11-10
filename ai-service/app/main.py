from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List, Dict, Any, Tuple
import os
import re
import time
from collections import defaultdict

# LangChain / LLM & tools
from langchain_google_genai import ChatGoogleGenerativeAI, GoogleGenerativeAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain.text_splitter import RecursiveCharacterTextSplitter
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
MYSQL_URL = os.getenv("MYSQL_URL", "mysql+pymysql://reader:reader@localhost:3306/product_db")
# Gemini model name - use names from /gemini/models endpoint (without "models/" prefix)
# Recommended: gemini-2.5-flash (fast, stable) or gemini-2.5-pro (more powerful)
MODEL_NAME = os.getenv("MODEL_NAME", "gemini-2.5-flash")

app = FastAPI(title="Smart Retail AI Service", version="0.1.0")

# CORS middleware để frontend có thể gọi API
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Cho phép tất cả origins (có thể giới hạn trong production)
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# In-memory conversation storage (có thể thay bằng Redis/DB cho production)
conversation_memories: Dict[str, ConversationBufferMemory] = defaultdict(lambda: ConversationBufferMemory(return_messages=True))


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
    embeddings = get_embeddings()
    os.makedirs(CHROMA_DIR, exist_ok=True)
    return Chroma(embedding_function=embeddings, persist_directory=CHROMA_DIR)


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


def retrieve_context(question: str, top_k: int) -> str:
    vs = ensure_vectorstore()
    retriever = vs.as_retriever(search_type="similarity", search_kwargs={"k": top_k})
    docs = retriever.get_relevant_documents(question)
    snippets = [d.page_content for d in docs]
    return "\n\n".join(snippets)


def build_sql_agent():
    llm = get_llm()
    # Kết nối database, include_tables=None = include all accessible tables
    db = SQLDatabase.from_uri(MYSQL_URL, include_tables=None)

    # Tạo agent với handle_parsing_errors để tránh lỗi parse output từ Gemini
    agent = create_sql_agent(
        llm=llm,
        db=db,
        verbose=False,
        handle_parsing_errors=True,  # Fix lỗi parsing output từ Gemini
        agent_executor_kwargs={"handle_parsing_errors": True}  # Thêm layer bảo vệ
    )
    return agent


def validate_sql_response(response: str, question: str) -> str:
    """
    Validate và làm sạch response từ SQL agent để đảm bảo chỉ dùng dữ liệu từ DB.
    Loại bỏ các câu trả lời suy đoán hoặc không có cơ sở từ DB.
    Nhưng cho phép các từ ngữ giao tiếp tự nhiên.
    """
    response_lower = response.lower()
    response_stripped = response.strip()

    # Các từ khóa cho thấy response có thể là suy đoán (loại trừ các từ giao tiếp tự nhiên)
    speculation_keywords = [
        "có lẽ", "có khả năng", "ước tính", "khoảng",
        "thường", "thông thường", "theo kiến thức",
        "dựa trên kinh nghiệm", "theo quy luật", "thông thường là"
    ]

    # Các từ ngữ giao tiếp tự nhiên được phép
    allowed_natural_phrases = [
        "theo thông tin hệ thống", "theo dữ liệu", "hiện tại",
        "vâng", "có", "dạ", "xin lỗi", "chúng tôi", "bạn"
    ]

    # Kiểm tra xem response có chứa từ khóa suy đoán không (loại trừ các cụm từ tự nhiên được phép)
    has_speculation = False
    for keyword in speculation_keywords:
        if keyword in response_lower:
            # Kiểm tra xem có phải là cụm từ tự nhiên được phép không
            is_allowed = any(phrase in response_lower for phrase in allowed_natural_phrases)
            if not is_allowed:
                has_speculation = True
                break

    # Nếu có suy đoán và không có số liệu cụ thể, cảnh báo
    if has_speculation:
        # Kiểm tra xem có số liệu cụ thể không (số, giá, số lượng)
        has_concrete_data = bool(re.search(r'\d+', response))
        if not has_concrete_data:
            return "Xin lỗi, hiện tại chúng tôi không tìm thấy thông tin này trong hệ thống."

    # Nếu response quá ngắn (chỉ là tên sản phẩm đơn thuần), cải thiện nó
    if len(response_stripped) < 30:
        # Kiểm tra xem có phải chỉ là tên sản phẩm không
        question_lower = question.lower()
        if any(word in question_lower for word in ["có", "còn", "bán", "giá", "tồn"]):
            # Nếu câu hỏi là về sản phẩm và response chỉ là tên, cải thiện
            if not re.search(r'\d+|VNĐ|đồng|giá|tồn|kho|còn|hàng', response_lower):
                # Có thể là tên sản phẩm đơn thuần, nhưng không cần sửa vì agent đã được hướng dẫn
                pass

    return response


def is_sql_question_using_embeddings(question: str) -> bool:
    """Dùng vector embedding để nhận diện SQL question (semantic similarity)"""
    try:
        embeddings = get_embeddings()

        # Các câu hỏi SQL mẫu (examples)
        sql_examples = [
            "Có bao nhiêu sản phẩm trong hệ thống?",
            "Số lượng tồn kho hiện tại là bao nhiêu?",
            "Doanh thu tháng này là bao nhiêu?",
            "Sản phẩm nào bán chạy nhất?",
            "Giá bán của sản phẩm này là bao nhiêu?",
            "Có bao nhiêu đơn hàng trong tháng?",
            "Tổng số khách hàng là bao nhiêu?",
            "Thống kê doanh số theo tháng",
            "Báo cáo tồn kho hiện tại",
            "Sản phẩm này còn hàng không?",
            "Còn hàng không?",
            "Kiểm tra tồn kho sản phẩm",
            "Sản phẩm có còn trong kho không?"
        ]

        # Embed câu hỏi hiện tại
        question_embedding = embeddings.embed_query(question)

        # Embed các câu hỏi mẫu và tính similarity
        max_similarity = 0.0
        for example in sql_examples:
            example_embedding = embeddings.embed_query(example)
            # Tính cosine similarity (simplified)
            similarity = sum(a * b for a, b in zip(question_embedding, example_embedding)) / (
                (sum(a*a for a in question_embedding) ** 0.5) *
                (sum(b*b for b in example_embedding) ** 0.5)
            )
            max_similarity = max(max_similarity, similarity)

        # Threshold: nếu similarity > 0.7 thì là SQL question
        return max_similarity > 0.7
    except Exception:
        # Fallback nếu embeddings lỗi
        return False


def is_sql_question_using_llm(question: str) -> bool:
    """Dùng LLM để phân loại câu hỏi có phải SQL không (semantic understanding)"""
    try:
        llm = get_llm()
        classification_prompt = f"""Phân loại câu hỏi sau có phải là câu hỏi về DỮ LIỆU/SỐ LIỆU cần truy vấn database không?
Câu hỏi: "{question}"

Trả lời CHỈ một từ: "YES" nếu là câu hỏi về dữ liệu/số liệu (ví dụ: số lượng, tồn kho, doanh thu, giá bán, đơn hàng, thống kê, báo cáo)
Hoặc "NO" nếu là câu hỏi về chính sách, hướng dẫn, thông tin chung không cần query database.

Trả lời:"""
        response = llm.invoke(classification_prompt)
        answer = response.content.strip().upper() if hasattr(response, "content") else str(response).strip().upper()
        return "YES" in answer or "CÓ" in answer
    except Exception:
        # Fallback to keyword matching if LLM fails
        return False


def is_sql_question_using_keywords(q: str) -> bool:
    """Nhận diện SQL question bằng keywords (fallback)"""
    ql = q.lower()
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
        "còn lại", "available", "availability", "còn sẵn"
    ]
    return any(k in ql for k in sql_keywords)


def is_sql_question(q: str) -> bool:
    """Nhận diện SQL question - dùng vector embedding (semantic) + LLM + keywords"""
    # Fast path 1: keyword matching trước (nhanh nhất)
    if is_sql_question_using_keywords(q):
        return True

    # Semantic path 1: dùng vector embedding để so sánh với các câu hỏi SQL mẫu
    # (hiểu từ đồng nghĩa, liên quan qua vector similarity)
    try:
        if is_sql_question_using_embeddings(q):
            return True
    except Exception:
        pass

    # Semantic path 2: dùng LLM để phân loại (backup nếu embedding không chắc chắn)
    try:
        return is_sql_question_using_llm(q)
    except Exception:
        # Nếu tất cả đều lỗi, fallback về keyword
        return False


def is_quota_error(error: Exception) -> Tuple[bool, Optional[str]]:
    """
    Kiểm tra xem lỗi có phải là quota error không
    Returns: (is_quota_error, retry_after_seconds_str)
    """
    error_str = str(error).lower()
    error_type = type(error).__name__

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


def format_quota_error_message(retry_after: Optional[str] = None) -> str:
    """Tạo thông báo lỗi quota thân thiện với người dùng"""
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
def chat(req: ChatRequest) -> Dict[str, Any]:
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

    # Thêm context về database vào câu hỏi SQL để agent hiểu rõ hơn
    enhanced_question = question
    if is_sql_question(question):
        enhanced_question = f"""Bạn là trợ lý thân thiện cho hệ thống siêu thị. Trả lời câu hỏi một cách tự nhiên, thân thiện như đang nói chuyện với khách hàng.

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

📝 VÍ DỤ CÁCH TRẢ LỜI:
Câu hỏi: "Có 7 up không?"
→ Nếu có: "Vâng, chúng tôi có sản phẩm 7 Up. Hiện tại có các loại: 7 Up (lon): 10.000 VNĐ, 7 Up (chai 500ml): 15.000 VNĐ"
→ Nếu không: "Xin lỗi, hiện tại chúng tôi không có sản phẩm 7 Up trong hệ thống. Bạn có muốn xem các sản phẩm tương tự không?"

Câu hỏi: "Giá bán của Coca Cola là bao nhiêu?"
→ "Theo thông tin hệ thống, giá bán của Coca Cola (lon): 12.000 VNĐ, Coca Cola (chai 500ml): 18.000 VNĐ"

Câu hỏi: "Còn hàng không?"
→ "Vâng, sản phẩm này vẫn còn hàng. Hiện tại còn 150 đơn vị trong kho"

Câu hỏi: {question}

Lưu ý: Bạn có thể truy vấn từ nhiều database bằng cú pháp database_name.table_name:

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

QUAN TRỌNG về tồn kho:
- Để kiểm tra tồn kho, query từ inventory_db.stock_balance
- stock_balance.product_unit_id liên kết với product_db.product_units.id
- Câu hỏi "còn hàng không" nghĩa là kiểm tra quantity > 0 hoặc available_quantity > 0
- Khi JOIN với products, dùng: product_db.product_units JOIN product_db.products ON product_units.product_id = products.id
 - Khi trả lời về tồn kho, luôn hiển thị số lượng cụ thể (quantity, available_quantity) và đơn vị từ units.name

QUAN TRỌNG về GIÁ BÁN:
- Luôn lấy giá ĐANG HOẠT ĐỘNG: product_db.price_lists.active = TRUE
- Nếu có liên kết price_headers, chỉ lấy những header active = TRUE và ngày hiện tại nằm trong khoảng [start_date, end_date]
- Nếu có nhiều bản ghi, ưu tiên bản giá mới nhất theo price_lists.created_at DESC
- Gợi ý mẫu truy vấn giá theo product_unit_id:
  SELECT pl.price
  FROM product_db.price_lists pl
  LEFT JOIN product_db.price_headers ph ON pl.price_header_id = ph.id
  WHERE pl.product_unit_id = ? AND pl.active = TRUE
    AND (ph.id IS NULL OR (ph.active = TRUE AND CURRENT_DATE BETWEEN ph.start_date AND ph.end_date))
  ORDER BY pl.created_at DESC
  LIMIT 1

⚠️ NHẮC LẠI QUY TẮC:
- Chỉ dùng SELECT với LIMIT. Trả lời bằng tiếng Việt, tự nhiên, thân thiện.
- Khi format số liệu, dùng ĐÚNG giá trị từ DB, không làm tròn trừ khi được yêu cầu.
- TUYỆT ĐỐI KHÔNG thêm thông tin ngoài kết quả SQL, nhưng có thể thêm các từ ngữ giao tiếp tự nhiên.
- Luôn trả lời như đang nói chuyện với khách hàng, không chỉ liệt kê dữ liệu khô khan.

QUAN TRỌNG: Luôn chỉ rõ đơn vị tính trong câu trả lời với format rõ ràng:
- Sản phẩm: "Tên sản phẩm (đơn vị): giá VNĐ"
  Ví dụ: "7 Up (lon): 10.000 VNĐ", "Coca Cola (chai 500ml): 15.000 VNĐ"
- Số lượng: "150 sản phẩm", "50 đơn vị", "25 mặt hàng"
- Giá tiền: "50.000 VNĐ", "1.500.000 đồng", "2 triệu VNĐ" (luôn có dấu chấm phân cách hàng nghìn)
- Tồn kho: "100 kg", "50 lít", "200 thùng"
- Doanh thu: "10 triệu VNĐ", "500.000 đồng"
- Thời gian: "30 ngày", "3 tháng", "1 năm"

Format chuẩn cho danh sách sản phẩm:
- Mỗi sản phẩm: "Tên (đơn vị): giá VNĐ"
- Ví dụ: "7 Up (lon): 10.000 VNĐ", "Pepsi (chai): 12.000 VNĐ"

🎯 LƯU Ý CUỐI CÙNG:
- Trả lời TỰ NHIÊN, THÂN THIỆN như đang nói chuyện với khách hàng
- Không chỉ trả về tên sản phẩm đơn thuần, hãy thêm thông tin hữu ích (giá, tồn kho nếu có)
- Sử dụng ngôn ngữ giao tiếp, ví dụ: "Vâng, chúng tôi có...", "Theo thông tin hệ thống...", "Hiện tại..."
- Luôn format rõ ràng, dễ đọc, chuyên nghiệp nhưng vẫn tự nhiên."""

    try:
        if is_sql_question(question):
            try:
                agent = build_sql_agent()
                sql_answer = agent.invoke({"input": enhanced_question})
                answer_text = sql_answer["output"] if isinstance(sql_answer, dict) else sql_answer

                # Validate response để đảm bảo chỉ dùng dữ liệu từ DB
                answer_text = validate_sql_response(answer_text, question)

                # Lưu vào memory
                memory.chat_memory.add_user_message(question)
                memory.chat_memory.add_ai_message(answer_text)

                return {"answer": answer_text, "route": "sql", "conversation_id": user_id}
            except Exception as sql_error:
                # Kiểm tra xem có phải lỗi quota không
                is_quota, retry_after = is_quota_error(sql_error)
                if is_quota:
                    error_msg = format_quota_error_message(retry_after)
                    return {
                        "answer": error_msg,
                        "route": "quota_error",
                        "error": "quota_exceeded",
                        "retry_after": retry_after,
                        "conversation_id": user_id
                    }

                # Nếu không phải quota error, fallback về LLM trực tiếp
                error_msg = str(sql_error)
                llm = get_llm()

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

QUAN TRỌNG về tồn kho:
- Để kiểm tra tồn kho, query từ inventory_db.stock_balance
- stock_balance.product_unit_id liên kết với product_db.product_units.id
- Câu hỏi "còn hàng không" nghĩa là kiểm tra quantity > 0 hoặc available_quantity > 0
- Khi JOIN với products, dùng: product_db.product_units JOIN product_db.products ON product_units.product_id = products.id
- Khi trả lời về tồn kho, luôn hiển thị số lượng cụ thể (quantity, available_quantity) và đơn vị từ units.name

QUAN TRỌNG: Luôn chỉ rõ đơn vị tính trong câu trả lời với format rõ ràng:
- Sản phẩm: "Tên sản phẩm (đơn vị): giá VNĐ"
  Ví dụ: "7 Up (lon): 10.000 VNĐ", "Coca Cola (chai 500ml): 15.000 VNĐ"
- Số lượng: "150 sản phẩm", "50 đơn vị", "25 mặt hàng"
- Giá tiền: "50.000 VNĐ", "1.500.000 đồng" (luôn có dấu chấm phân cách hàng nghìn)
- Tồn kho: "100 kg", "50 lít", "200 thùng"
- Doanh thu: "10 triệu VNĐ", "500.000 đồng"
- Thời gian: "30 ngày", "3 tháng", "1 năm"

Format chuẩn cho danh sách sản phẩm: "Tên (đơn vị): giá VNĐ"
Luôn format rõ ràng, dễ đọc, chuyên nghiệp."""),
                    MessagesPlaceholder(variable_name="history"),
                    ("human", "{question}\n\nLỗi SQL Agent: {error}")
                ])

                prompt = prompt_template.format_messages(
                    question=question,
                    error=error_msg[:200],
                        history=messages[-5:] if len(messages) > 5 else messages
                )

                try:
                    response = llm.invoke(prompt)
                    text = response.content if hasattr(response, "content") else str(response)
                except Exception as llm_error:
                    # Kiểm tra lại quota error trong LLM call
                    is_quota, retry_after = is_quota_error(llm_error)
                    if is_quota:
                        error_msg = format_quota_error_message(retry_after)
                        return {
                            "answer": error_msg,
                            "route": "quota_error",
                            "error": "quota_exceeded",
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
                llm = get_llm()
                context = retrieve_context(question, top_k=req.top_k)

                # Tạo prompt với conversation history
                messages = memory.chat_memory.messages if hasattr(memory.chat_memory, 'messages') else []

                # Build prompt với context và history
                prompt_template = ChatPromptTemplate.from_messages([
                    ("system", """Bạn là trợ lý cho hệ thống siêu thị. Dựa trên ngữ cảnh và lịch sử hội thoại, trả lời rõ ràng, ngắn gọn.

QUAN TRỌNG: Luôn chỉ rõ đơn vị tính trong câu trả lời với format rõ ràng:
- Sản phẩm: "Tên sản phẩm (đơn vị): giá VNĐ"
  Ví dụ: "7 Up (lon): 10.000 VNĐ", "Coca Cola (chai 500ml): 15.000 VNĐ"
- Số lượng: "150 sản phẩm", "50 đơn vị", "25 mặt hàng"
- Giá tiền: "50.000 VNĐ", "1.500.000 đồng" (luôn có dấu chấm phân cách hàng nghìn)
- Tồn kho: "100 kg", "50 lít", "200 thùng"
- Doanh thu: "10 triệu VNĐ", "500.000 đồng"
- Thời gian: "30 ngày", "3 tháng", "1 năm"

Format chuẩn cho danh sách sản phẩm: "Tên (đơn vị): giá VNĐ"
Luôn format rõ ràng, dễ đọc, chuyên nghiệp."""),
                    MessagesPlaceholder(variable_name="history"),
                    ("human", """Ngữ cảnh từ tài liệu:
{context}

Câu hỏi: {question}""")
                ])

                prompt = prompt_template.format_messages(
                    context=context,
                    question=question,
                    history=messages[-10:] if len(messages) > 10 else messages  # Giới hạn 10 tin nhắn gần nhất
                )

                response = llm.invoke(prompt)
                text = response.content if hasattr(response, "content") else str(response)

                # Lưu vào memory
                memory.chat_memory.add_user_message(question)
                memory.chat_memory.add_ai_message(text)

                return {"answer": text, "route": "rag", "conversation_id": user_id}
            except Exception as rag_error:
                # Kiểm tra xem có phải lỗi quota không
                is_quota, retry_after = is_quota_error(rag_error)
                if is_quota:
                    error_msg = format_quota_error_message(retry_after)
                    return {
                        "answer": error_msg,
                        "route": "quota_error",
                        "error": "quota_exceeded",
                        "retry_after": retry_after,
                        "conversation_id": user_id
                    }

                # Nếu RAG lỗi (không phải quota), fallback về LLM trực tiếp (không cần embeddings)
                llm = get_llm()

                # Dùng conversation history
                messages = memory.chat_memory.messages if hasattr(memory.chat_memory, 'messages') else []
                prompt_template = ChatPromptTemplate.from_messages([
                        ("system", """Bạn là trợ lý cho hệ thống siêu thị. Trả lời câu hỏi dựa trên lịch sử hội thoại.

QUAN TRỌNG: Luôn chỉ rõ đơn vị tính trong câu trả lời với format rõ ràng:
- Sản phẩm: "Tên sản phẩm (đơn vị): giá VNĐ"
  Ví dụ: "7 Up (lon): 10.000 VNĐ", "Coca Cola (chai 500ml): 15.000 VNĐ"
- Số lượng: "150 sản phẩm", "50 đơn vị", "25 mặt hàng"
- Giá tiền: "50.000 VNĐ", "1.500.000 đồng" (luôn có dấu chấm phân cách hàng nghìn)
- Tồn kho: "100 kg", "50 lít", "200 thùng"
- Doanh thu: "10 triệu VNĐ", "500.000 đồng"
- Thời gian: "30 ngày", "3 tháng", "1 năm"

Format chuẩn cho danh sách sản phẩm: "Tên (đơn vị): giá VNĐ"
Luôn format rõ ràng, dễ đọc, chuyên nghiệp."""),
                    MessagesPlaceholder(variable_name="history"),
                    ("human", "{question}")
                ])

                prompt = prompt_template.format_messages(
                    question=question,
                    history=messages[-10:] if len(messages) > 10 else messages
                )

                try:
                    response = llm.invoke(prompt)
                    text = response.content if hasattr(response, "content") else str(response)
                except Exception as llm_error:
                    # Kiểm tra lại quota error trong LLM call
                    is_quota, retry_after = is_quota_error(llm_error)
                    if is_quota:
                        error_msg = format_quota_error_message(retry_after)
                        return {
                            "answer": error_msg,
                            "route": "quota_error",
                            "error": "quota_exceeded",
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
    except Exception as e:
        # Kiểm tra quota error ở level cuối cùng
        is_quota, retry_after = is_quota_error(e)
        if is_quota:
            error_msg = format_quota_error_message(retry_after)
            return {
                "answer": error_msg,
                "route": "quota_error",
                "error": "quota_exceeded",
                "retry_after": retry_after,
                "conversation_id": user_id
            }

        # Nếu không phải quota error, trả về lỗi chi tiết
        import traceback
        error_detail = f"{str(e)}\n\nTraceback:\n{traceback.format_exc()}"
        raise HTTPException(status_code=500, detail=error_detail)


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
        embedding_match = False
        llm_match = False

        try:
            embedding_match = is_sql_question_using_embeddings(question)
        except Exception as e:
            embedding_match = f"Error: {str(e)}"

        try:
            llm_match = is_sql_question_using_llm(question)
        except Exception as e:
            llm_match = f"Error: {str(e)}"

        final_result = is_sql_question(question)

        return {
            "question": question,
            "keyword_match": keyword_match,
            "embedding_match": embedding_match,
            "llm_match": llm_match,
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


