"""
Script để ingest documents trực tiếp vào vectorstore (không cần service chạy)
Cách sử dụng:
    python ingest_docs_direct.py
"""
import os
import sys

# Thêm thư mục app vào path để import
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'app'))

# Import các hàm từ main.py
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import DirectoryLoader, TextLoader
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Config
GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY", "")
CHROMA_DIR = os.getenv("CHROMA_DIR", "./data/chroma")
DOCS_DIR = os.getenv("DOCS_DIR", "./data/docs")

def load_and_split(paths=None):
    """Load và split documents"""
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=1200, chunk_overlap=150)
    documents = []
    if paths:
        for p in paths:
            if os.path.isdir(p):
                documents.extend(DirectoryLoader(p, glob='**/*', loader_cls=TextLoader, loader_kwargs={'encoding': 'utf-8'}, show_progress=True).load())
            elif os.path.isfile(p):
                documents.extend(TextLoader(p, encoding='utf-8').load())
    else:
        if os.path.exists(DOCS_DIR):
            documents.extend(DirectoryLoader(DOCS_DIR, glob='**/*', loader_cls=TextLoader, loader_kwargs={'encoding': 'utf-8'}, show_progress=True).load())
        else:
            print(f"❌ Thư mục {DOCS_DIR} không tồn tại!")
            return []
    return text_splitter.split_documents(documents)

def ingest_documents(paths=None):
    """Ingest documents vào vectorstore"""
    try:
        # Kiểm tra API key
        if not GOOGLE_API_KEY:
            print("❌ GOOGLE_API_KEY chưa được cấu hình!")
            print("   Vui lòng tạo file .env và thêm GOOGLE_API_KEY=your_key")
            return None

        print("🔄 Đang khởi tạo embeddings...")
        embeddings = GoogleGenerativeAIEmbeddings(
            model="models/embedding-001",
            google_api_key=GOOGLE_API_KEY
        )

        print("🔄 Đang khởi tạo vectorstore...")
        os.makedirs(CHROMA_DIR, exist_ok=True)
        vectorstore = Chroma(embedding_function=embeddings, persist_directory=CHROMA_DIR)

        print("🔄 Đang load và split documents...")
        if paths:
            print(f"   Paths: {paths}")
        else:
            print(f"   Từ thư mục: {DOCS_DIR}")

        docs = load_and_split(paths)

        if not docs:
            print("❌ Không tìm thấy documents để ingest!")
            return None

        print(f"   Đã load {len(docs)} documents")
        print(f"   Đang split thành chunks...")

        print("🔄 Đang thêm vào vectorstore...")
        vectorstore.add_documents(docs)
        vectorstore.persist()

        print(f"\n✅ Ingest thành công!")
        print(f"   - Số chunks đã index: {len(docs)}")
        print(f"   - Thư mục lưu: {CHROMA_DIR}")
        return len(docs)

    except Exception as e:
        print(f"❌ Lỗi khi ingest: {e}")
        import traceback
        traceback.print_exc()
        return None

if __name__ == "__main__":
    print("=" * 60)
    print("📚 INGEST DOCUMENTS TRỰC TIẾP VÀO VECTORSTORE")
    print("=" * 60)
    print()

    # Kiểm tra arguments
    if len(sys.argv) > 1:
        # Có paths được chỉ định
        paths = sys.argv[1:]
        print(f"📁 Ingest các file/folder: {paths}")
        result = ingest_documents(paths=paths)
    else:
        # Ingest tất cả file trong DOCS_DIR
        print(f"📁 Ingest tất cả file trong {DOCS_DIR}")
        result = ingest_documents()

    print()
    if result:
        print("✅ Hoàn tất! Bây giờ bạn có thể hỏi về thông tin siêu thị.")
        print()
        print("💡 Ví dụ câu hỏi:")
        print("   - 'Địa chỉ siêu thị ở đâu?'")
        print("   - 'Phí giao hàng là bao nhiêu?'")
        print("   - 'Siêu thị mở cửa lúc mấy giờ?'")
        print("   - 'Siêu thị có giao hàng hỏa tốc không?'")
    else:
        print("❌ Ingest thất bại. Vui lòng kiểm tra lại.")

