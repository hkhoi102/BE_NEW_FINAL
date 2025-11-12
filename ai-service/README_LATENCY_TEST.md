# Hướng dẫn Test Latency AI Chatbot

Script này giúp đánh giá độ trễ (latency) của AI Chatbot bằng cách gửi 10 yêu cầu HTTP đồng thời (mặc định).

## ⚠️ QUAN TRỌNG: Chạy AI Service trước

**Bạn PHẢI chạy AI service trước khi test latency!**

### Cách 1: Chạy trực tiếp (Python)

```bash
cd ai-service
pip install -r requirements.txt

# Cấu hình biến môi trường (tạo file .env hoặc export)
# GOOGLE_API_KEY=your_key
# MYSQL_URL=mysql+pymysql://reader:reader@localhost:3306/product_db

# Chạy service
uvicorn app.main:app --reload --port 8000
```

### Cách 2: Chạy bằng Docker

```bash
# Từ thư mục root của project
docker-compose up ai-service

# Hoặc build và chạy riêng
cd ai-service
docker build -t smart-retail-ai .
docker run -p 8000:8000 --env-file .env smart-retail-ai
```

### Kiểm tra service đã chạy

```bash
# Kiểm tra health endpoint
curl http://localhost:8000/health

# Kết quả mong đợi: {"status":"ok"}
```

## Yêu cầu

1. **AI Service đang chạy** trên port 8000 (hoặc port bạn chỉ định)
2. Python 3.11+
3. Cài đặt dependencies:
```bash
pip install aiohttp
```

Hoặc cài đặt tất cả dependencies:
```bash
pip install -r requirements.txt
```

## Cách sử dụng

**Lưu ý:** Đảm bảo AI service đã chạy trước khi test!

### 📍 Vị trí chạy lệnh

**Bạn PHẢI chạy lệnh từ thư mục `ai-service/`:**

```bash
# Di chuyển vào thư mục ai-service
cd ai-service

# Sau đó chạy script
python test_latency.py
```

Hoặc nếu đang ở thư mục root của project:

```bash
# Chạy từ root
cd ai-service && python test_latency.py

# Hoặc
python ai-service/test_latency.py
```

### 1. Chạy với số lượng requests mặc định (10)

```bash
cd ai-service
python test_latency.py
```

### 2. Chỉ định số lượng requests

```bash
cd ai-service
python test_latency.py 30
```

### 3. Test với câu hỏi phức tạp (xử lý ngôn ngữ tự nhiên)

Script hỗ trợ nhiều loại câu hỏi khác nhau:

```bash
cd ai-service

# Câu hỏi đơn giản (SQL đơn giản)
python test_latency.py 10 simple

# Câu hỏi SQL phức tạp (JOIN nhiều bảng)
python test_latency.py 10 complex_sql

# Câu hỏi ngôn ngữ tự nhiên phức tạp
python test_latency.py 10 complex_natural_language

# Câu hỏi hỗn hợp (mặc định)
python test_latency.py 10 mixed_complexity

# Câu hỏi RAG (từ tài liệu)
python test_latency.py 10 rag_questions

# Tất cả các loại câu hỏi
python test_latency.py 10 all
```

### 4. Chỉ định URL API (nếu không chạy localhost:8000)

```bash
cd ai-service
python test_latency.py 10 mixed_complexity http://your-api-url:8000/chat
```

### 5. Các loại câu hỏi có sẵn

File `test_questions.json` chứa các loại câu hỏi:

- **simple**: Câu hỏi đơn giản, SQL đơn giản
  - VD: "cửa hàng có redbull không", "giá bán của coca là bao nhiêu"

- **complex_sql**: Câu hỏi SQL phức tạp, cần JOIN nhiều bảng
  - VD: "sản phẩm nào còn hàng và đang khuyến mãi", "mua x tặng y có gì"

- **complex_natural_language**: Câu hỏi ngôn ngữ tự nhiên phức tạp, đòi hỏi xử lý ngữ nghĩa
  - VD: "cho tôi biết những sản phẩm đang được khuyến mãi và còn hàng trong kho"
  - VD: "tôi muốn mua nước ngọt, bạn có thể cho tôi biết giá và số lượng còn lại không"

- **mixed_complexity**: Hỗn hợp các loại câu hỏi (mặc định)

- **rag_questions**: Câu hỏi cần RAG (Retrieval Augmented Generation)
  - VD: "chính sách đổi trả hàng như thế nào", "thời gian mở cửa của siêu thị"

- **all**: Tất cả các loại câu hỏi

## Kết quả

Script sẽ hiển thị:

1. **Tổng quan:**
   - Tổng số requests
   - Số requests thành công/thất bại
   - Tỷ lệ thành công

2. **Thống kê Latency:**
   - Min, Max, Average, Median
   - P95, P99 (percentile)
   - Standard deviation

3. **Chi tiết từng request:**
   - ID, Latency, Status code, Success/Failed

4. **File JSON:**
   - Kết quả được lưu vào file `latency_test_results_YYYYMMDD_HHMMSS.json`

## Ví dụ kết quả

```
============================================================
📊 KẾT QUẢ TEST LATENCY
============================================================

📈 TỔNG QUAN:
   • Tổng số requests: 10
   • Thành công: 10 (100.0%)
   • Thất bại: 0

⏱️  LATENCY (milliseconds):
   • Min:       1250.50 ms
   • Max:       3500.20 ms
   • Average:   2100.30 ms
   • Median:    2050.00 ms
   • P95:       3200.00 ms
   • P99:       3400.00 ms
   • Std Dev:   450.20 ms

⏱️  LATENCY (seconds):
   • Min:          1.251 s
   • Max:          3.500 s
   • Average:      2.100 s
   • Median:       2.050 s
   • P95:          3.200 s
   • P99:          3.400 s
```

## Cấu hình

Bạn có thể chỉnh sửa các biến trong file `test_latency.py`:

- `API_URL`: URL của API chatbot (mặc định: `http://localhost:8000/chat`)
- `NUM_REQUESTS`: Số lượng requests mặc định (mặc định: 10)
- `TEST_QUESTION`: Câu hỏi test mặc định (dùng khi không có file questions)
- `QUESTIONS_FILE`: File chứa danh sách câu hỏi (mặc định: `test_questions.json`)

### Thêm câu hỏi test mới

Chỉnh sửa file `test_questions.json` để thêm câu hỏi mới vào các category tương ứng.

## Lưu ý

- Script sử dụng `asyncio` và `aiohttp` để gửi requests đồng thời
- Timeout mặc định là 120 giây cho mỗi request
- Nếu API đang chạy trong Docker, đảm bảo port 8000 đã được expose
- Nếu test với số lượng requests lớn, có thể gặp rate limiting từ Google Gemini API

## Troubleshooting

### Lỗi kết nối
- **QUAN TRỌNG:** Đảm bảo AI service đã chạy trước khi test
- Kiểm tra AI service: `curl http://localhost:8000/health` (phải trả về `{"status":"ok"}`)
- Kiểm tra port 8000 có đang được sử dụng: `netstat -an | findstr 8000` (Windows) hoặc `lsof -i :8000` (Linux/Mac)
- Kiểm tra firewall/network settings
- Nếu chạy trong Docker, kiểm tra container: `docker ps | grep ai-service`

### Timeout
- Tăng timeout trong script nếu câu hỏi phức tạp
- Kiểm tra performance của database và API

### Rate limiting
- Google Gemini API có giới hạn requests/phút
- Giảm số lượng concurrent requests nếu gặp lỗi quota


