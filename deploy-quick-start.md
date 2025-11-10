# ⚡ Quick Start - Deploy Nhanh

## Phương Pháp Nhanh Nhất: Docker Compose

### Bước 1: Chuẩn Bị (5 phút)

```bash
# 1. Đảm bảo đã có Docker và Docker Compose
docker --version
docker-compose --version

# 2. Setup MySQL (nếu chưa có)
# Option A: Dùng MySQL có sẵn trên server
# Option B: Dùng MySQL trong Docker (thêm vào docker-compose.yml)

# 3. Tạo file .env
copy env.example .env
# Sửa file .env và điền keys thật
```

### Bước 2: Build Services (10-15 phút)

```bash
# Windows
build-all.bat

# Linux/Mac
chmod +x build-all.sh
./build-all.sh
```

### Bước 3: Tạo Dockerfiles (1 phút)

```bash
# Linux/Mac
chmod +x create-dockerfiles.sh
./create-dockerfiles.sh

# Windows: Tạo thủ công hoặc dùng WSL
```

### Bước 4: Build Docker Images (5-10 phút)

```bash
# Build từng service
cd service-product
docker build -t service-product:latest .
cd ../order-service
docker build -t order-service:latest .
# ... tiếp tục với các service khác

# Hoặc dùng script (nếu có)
```

### Bước 5: Deploy với Docker Compose (1 phút)

```bash
# Copy và sửa docker-compose.yml
copy docker-compose.example.yml docker-compose.yml

# Chạy
docker-compose up -d

# Xem logs
docker-compose logs -f
```

### Bước 6: Kiểm Tra

```bash
# Kiểm tra services đang chạy
docker-compose ps

# Kiểm tra Eureka Dashboard
# Mở browser: http://localhost:8761

# Test API Gateway
curl http://localhost:8085/actuator/health
```

## Tổng Thời Gian: ~30 phút

---

## Nếu Gặp Lỗi

1. **Port đã được sử dụng**: Đổi port trong `application.properties` hoặc dừng service đang dùng port đó
2. **Database connection failed**: Kiểm tra MySQL đã chạy và databases đã được tạo chưa
3. **Service không đăng ký với Eureka**: Đợi Eureka khởi động xong (30 giây) trước khi start các service khác

Xem `HUONG_DAN_DEPLOY_CHI_TIET.md` để biết chi tiết troubleshooting.

