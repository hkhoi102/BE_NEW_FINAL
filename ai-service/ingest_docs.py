"""
Script để ingest documents vào vectorstore
Cách sử dụng:
    python ingest_docs.py
"""
import requests
import json
import sys

# URL của AI service (thay đổi nếu cần)
BASE_URL = "http://localhost:8000"

def ingest_documents(paths=None):
    """
    Ingest documents vào vectorstore

    Args:
        paths: List đường dẫn file/folder cần ingest.
               Nếu None, sẽ ingest tất cả file trong DOCS_DIR
    """
    url = f"{BASE_URL}/ingest"

    if paths:
        payload = {"paths": paths}
    else:
        # Nếu không có paths, sẽ ingest tất cả trong DOCS_DIR
        payload = {}

    try:
        print(f"🔄 Đang gửi request đến {url}...")
        if payload:
            print(f"   Paths: {paths}")
        else:
            print(f"   Sẽ ingest tất cả file trong data/docs")

        response = requests.post(url, json=payload, timeout=60)
        response.raise_for_status()
        result = response.json()

        print(f"\n✅ Ingest thành công!")
        print(f"   - Số chunks đã index: {result.get('indexed_chunks', 0)}")
        print(f"   - Thư mục lưu: {result.get('persist_directory', 'N/A')}")
        return result
    except requests.exceptions.ConnectionError:
        print(f"❌ Không thể kết nối đến {BASE_URL}")
        print(f"   Vui lòng đảm bảo AI service đang chạy!")
        return None
    except requests.exceptions.RequestException as e:
        print(f"❌ Lỗi khi ingest: {e}")
        if hasattr(e, 'response') and e.response is not None:
            try:
                error_detail = e.response.json()
                print(f"   Chi tiết: {error_detail}")
            except:
                print(f"   Response: {e.response.text}")
        return None

if __name__ == "__main__":
    print("=" * 60)
    print("📚 INGEST DOCUMENTS VÀO VECTORSTORE")
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
        print("📁 Ingest tất cả file trong data/docs")
        result = ingest_documents()

    print()
    if result:
        print("✅ Hoàn tất! Bây giờ bạn có thể hỏi về thông tin siêu thị.")
        print()
        print("💡 Ví dụ câu hỏi:")
        print("   - 'Địa chỉ siêu thị ở đâu?'")
        print("   - 'Phí giao hàng là bao nhiêu?'")
        print("   - 'Siêu thị mở cửa lúc mấy giờ?'")
    else:
        print("❌ Ingest thất bại. Vui lòng kiểm tra lại.")
