# 📋 Thứ Tự Deploy - Từng Bước

## ⚠️ QUAN TRỌNG: Thứ Tự Các Bước

**KHÔNG phải bước 1 là start services!** Bạn cần chuẩn bị trước.

## 🔄 Thứ Tự Đầy Đủ

### Bước 1: Chuẩn Bị Môi Trường ✅
- [ ] Cài Java 17
- [ ] Cài Maven (hoặc dùng mvnw)
- [ ] Cài MySQL 8.0+
- [ ] Cài Docker (nếu dùng Docker)

### Bước 2: Setup Database ✅
- [ ] Khởi động MySQL
- [ ] Tạo các databases (product_db, order_db, inventory_db, etc.)
- [ ] Tạo user và cấp quyền

### Bước 3: Cấu Hình Environment Variables ✅
- [ ] Copy `env.example` thành `.env`
- [ ] Điền các keys thật vào `.env`

### Bước 4: Build Ứng Dụng ✅
- [ ] Build tất cả services: `build-all.bat` hoặc `./build-all.sh`
- [ ] Kiểm tra JAR files đã được tạo trong `target/`

### Bước 5: Deploy/Start Services ✅
**Đây mới là bước start services!**

---

## 🚀 Thứ Tự Start Services (Bước 5)

### ⚠️ QUAN TRỌNG: Phải start theo thứ tự!

### 1️⃣ **MySQL** (Nếu chưa chạy)
```bash
# Kiểm tra MySQL đã chạy chưa
mysql -u root -p

# Nếu chưa chạy, start MySQL:
# Windows: Services → MySQL → Start
# Linux: sudo systemctl start mysql
# Mac: brew services start mysql
```

### 2️⃣ **Discovery Server (Eureka)** - Port 8761
```bash
# Phải start ĐẦU TIÊN!
# Vì tất cả services khác cần đăng ký với Eureka

# Với Docker:
docker run -d -p 8761:8761 discovery-server:latest

# Với JAR:
java -jar discovery-server/target/discovery-server-*.jar

# Đợi 30 giây để Eureka khởi động xong
```

### 3️⃣ **Các Microservices** (Có thể start song song)
Sau khi Eureka đã chạy, bạn có thể start các services này:

```bash
# Auth Service - Port 8081
java -jar service-auth/target/service-auth-*.jar

# User Service - Port 8082
java -jar user-service/target/user-service-*.jar

# Customer Service - Port 8083
java -jar service-customer/target/service-customer-*.jar

# Product Service - Port 8084
java -jar service-product/target/service-product-*.jar

# Inventory Service - Port 8086
java -jar inventory-service/target/inventory-service-*.jar

# Promotion Service - Port 8087
java -jar promotion-service/target/promotion-service-*.jar

# Order Service - Port 8088
java -jar order-service/target/order-service-*.jar

# Payment Service - Port 8090
java -jar payment-service/target/payment-service-*.jar
```

**Lưu ý**: Có thể start song song, nhưng nên đợi mỗi service đăng ký xong với Eureka (khoảng 10-20 giây mỗi service)

### 4️⃣ **API Gateway** - Port 8085
```bash
# Phải start CUỐI CÙNG!
# Vì API Gateway cần biết các services khác đã đăng ký với Eureka

java -jar api-gateway/target/api-gateway-*.jar
```

### 5️⃣ **AI Service** (Tùy chọn) - Port 8000
```bash
# Có thể start bất cứ lúc nào
cd ai-service
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

---

## 🐳 Với Docker Compose (Dễ Hơn)

Nếu dùng Docker Compose, thứ tự được tự động xử lý:

```bash
# 1. Build images (nếu chưa build)
docker-compose build

# 2. Start tất cả (tự động xử lý dependencies)
docker-compose up -d

# Docker Compose sẽ tự động:
# - Start discovery-server trước
# - Đợi discovery-server sẵn sàng
# - Start các services khác
# - Start api-gateway cuối cùng
```

---

## ✅ Checklist Start Services

- [ ] MySQL đã chạy
- [ ] Databases đã được tạo
- [ ] File `.env` đã được cấu hình
- [ ] JAR files đã được build
- [ ] Discovery Server đã start và chạy (kiểm tra http://localhost:8761)
- [ ] Các microservices đã start
- [ ] API Gateway đã start (cuối cùng)

---

## 🔍 Kiểm Tra Sau Khi Start

### 1. Kiểm Tra Eureka Dashboard
Mở browser: `http://localhost:8761`

Bạn sẽ thấy danh sách các services đã đăng ký:
- ✅ service-auth
- ✅ user-service
- ✅ service-customer
- ✅ service-product
- ✅ inventory-service
- ✅ order-service
- ✅ promotion-service
- ✅ payment-service
- ✅ api-gateway

### 2. Kiểm Tra Health Endpoints
```bash
# API Gateway
curl http://localhost:8085/actuator/health

# Product Service
curl http://localhost:8084/actuator/health

# Order Service
curl http://localhost:8088/actuator/health
```

### 3. Test API Gateway
```bash
# Test endpoint qua API Gateway
curl http://localhost:8085/api/products
```

---

## ⚠️ Lưu Ý Quan Trọng

1. **Discovery Server phải start TRƯỚC** - Tất cả services khác cần nó
2. **API Gateway phải start CUỐI** - Cần biết các services đã đăng ký
3. **Đợi mỗi service đăng ký xong** - Khoảng 10-30 giây mỗi service
4. **Kiểm tra logs** nếu service không start được

---

## 🎯 Tóm Tắt

**Thứ tự start:**
1. MySQL ✅
2. Discovery Server (Eureka) ✅
3. Các Microservices ✅
4. API Gateway ✅ (cuối cùng)
5. AI Service (tùy chọn) ✅

**Với Docker Compose:** Chỉ cần `docker-compose up -d` - tự động xử lý thứ tự!

---

Xem `HUONG_DAN_DEPLOY_CHI_TIET.md` để biết chi tiết từng bước.

