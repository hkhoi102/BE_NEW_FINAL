# 🚀 Hướng Dẫn Deploy Backend - Từng Bước Chi Tiết

## 📋 Mục Lục

1. [Chuẩn Bị](#1-chuẩn-bị)
2. [Setup Database](#2-setup-database)
3. [Cấu Hình Environment Variables](#3-cấu-hình-environment-variables)
4. [Build Ứng Dụng](#4-build-ứng-dụng)
5. [Deploy Services](#5-deploy-services)
6. [Kiểm Tra](#6-kiểm-tra)

---

## 1. Chuẩn Bị

### 1.1. Kiểm Tra Java

Mở Command Prompt hoặc PowerShell và chạy:

```bash
java -version
```

**Kết quả mong đợi:**
```
openjdk version "17.0.x" hoặc "21.0.x"
```

**Nếu chưa có Java:**
- Download JDK 17 từ: https://adoptium.net/temurin/releases/?version=17
- Cài đặt và thêm vào PATH
- Khởi động lại terminal và kiểm tra lại

### 1.2. Kiểm Tra Maven

```bash
mvn -version
```

**Hoặc dùng Maven Wrapper có sẵn:**
```bash
# Windows
.\mvnw.cmd -version

# Linux/Mac
./mvnw -version
```

### 1.3. Kiểm Tra MySQL

```bash
mysql --version
```

**Nếu chưa có MySQL:**
- Download từ: https://dev.mysql.com/downloads/mysql/
- Cài đặt và ghi nhớ password root

### 1.4. Kiểm Tra Docker (Nếu dùng Docker)

```bash
docker --version
docker-compose --version
```

---

## 2. Setup Database

### 2.1. Khởi Động MySQL

**Windows:**
- Mở Services (Win + R → `services.msc`)
- Tìm "MySQL" → Right click → Start

**Linux:**
```bash
sudo systemctl start mysql
sudo systemctl enable mysql
```

**Mac:**
```bash
brew services start mysql
```

### 2.2. Đăng Nhập MySQL

```bash
mysql -u root -p
# Nhập password root của bạn
```

### 2.3. Tạo Databases

Copy và paste từng dòng dưới đây vào MySQL:

```sql
-- Tạo các databases
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS customer_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS promotion_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS analytics_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS chatbox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2.4. Tạo User và Cấp Quyền

**Thay `your_password` bằng password thật của bạn:**

```sql
-- Tạo user cho ứng dụng
CREATE USER IF NOT EXISTS 'app_user'@'%' IDENTIFIED BY 'your_password';

-- Cấp quyền cho tất cả databases
GRANT ALL PRIVILEGES ON product_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON customer_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON user_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON auth_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON promotion_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON analytics_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON chatbox_db.* TO 'app_user'@'%';

-- Tạo user read-only cho AI Service
CREATE USER IF NOT EXISTS 'reader'@'%' IDENTIFIED BY 'reader_password';
GRANT SELECT ON product_db.* TO 'reader'@'%';
GRANT SELECT ON order_db.* TO 'reader'@'%';
GRANT SELECT ON inventory_db.* TO 'reader'@'%';

-- Áp dụng thay đổi
FLUSH PRIVILEGES;

-- Kiểm tra
SHOW DATABASES;

-- Thoát
EXIT;
```

### 2.5. Kiểm Tra Kết Nối

```bash
mysql -u app_user -p -h localhost product_db
# Nhập password, nếu kết nối được là OK
# Gõ EXIT để thoát
```

---

## 3. Cấu Hình Environment Variables

### 3.1. Tạo File .env

**Windows:**
```bash
copy env.example .env
```

**Linux/Mac:**
```bash
cp env.example .env
```

### 3.2. Mở File .env và Điền Thông Tin

Mở file `.env` bằng Notepad hoặc text editor và điền các giá trị thật:

```bash
# ============================================
# Database Configuration
# ============================================
DB_USERNAME=app_user
DB_PASSWORD=your_database_password  # Password bạn đã tạo ở bước 2.4

# ============================================
# JWT Secret (phải có ít nhất 32 ký tự)
# ============================================
JWT_SECRET=your-very-strong-jwt-secret-key-at-least-32-characters-long-change-this

# ============================================
# AWS S3 Configuration (cho service-product)
# ============================================
AWS_ACCESS_KEY_ID=your-aws-access-key-id
AWS_SECRET_ACCESS_KEY=your-aws-secret-access-key
AWS_REGION=ap-southeast-2
AWS_S3_BUCKET=your-bucket-name
AWS_S3_FOLDER=product-images

# ============================================
# Email Configuration (cho order-service, user-service)
# ============================================
# Gmail: Cần dùng App Password (không phải password thường)
# Bật 2-Step Verification → Tạo App Password
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password-16-chars
MAIL_FROM=your-email@gmail.com

# ============================================
# SePay Configuration (cho payment-service)
# ============================================
SEPAY_API_KEY=your-sepay-api-key
SEPAY_SECRET=your-sepay-secret
SEPAY_ACCOUNT_NUMBER=your-account-number
SEPAY_ACCOUNT_NAME=your-account-name
SEPAY_BANK_CODE=your-bank-code

# ============================================
# AI Service Configuration
# ============================================
GOOGLE_API_KEY=your-google-api-key
MODEL_NAME=gemini-2.5-flash
USE_GEMINI=true
MYSQL_URL=mysql+pymysql://reader:reader_password@localhost:3306/product_db
```

**Lưu ý quan trọng:**
- Thay TẤT CẢ các giá trị `your_*` bằng giá trị thật
- File `.env` đã được `.gitignore`, sẽ không bị commit lên git
- Giữ file này an toàn, không chia sẻ công khai

---

## 4. Build Ứng Dụng

### 4.1. Mở Terminal/Command Prompt

Điều hướng đến thư mục root của project:

```bash
cd D:\DATN\code\smart-retail-backend
```

### 4.2. Build Tất Cả Services

**Windows:**
```bash
build-all.bat
```

**Linux/Mac:**
```bash
chmod +x build-all.sh
./build-all.sh
```

**Hoặc build thủ công:**
```bash
# Windows
.\mvnw.cmd clean package -DskipTests

# Linux/Mac
./mvnw clean package -DskipTests
```

### 4.3. Kiểm Tra JAR Files Đã Được Tạo

Sau khi build xong, kiểm tra các file JAR:

```bash
# Windows
dir discovery-server\target\*.jar
dir api-gateway\target\*.jar
dir service-auth\target\*.jar

# Linux/Mac
ls -lh discovery-server/target/*.jar
ls -lh api-gateway/target/*.jar
ls -lh service-auth/target/*.jar
```

**Bạn sẽ thấy các file như:**
- `discovery-server-0.0.1-SNAPSHOT.jar`
- `api-gateway-0.0.1-SNAPSHOT.jar`
- `service-auth-0.0.1-SNAPSHOT.jar`
- etc.

---

## 5. Deploy Services

### Phương Pháp A: Deploy Với Docker Compose (Khuyến nghị)

#### 5.1. Tạo Dockerfiles

**Tạo Dockerfile cho mỗi service:**

Ví dụ: Tạo file `discovery-server/Dockerfile`:

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Lặp lại cho các service khác, chỉ thay port:**
- `api-gateway/Dockerfile` → Port 8085
- `service-auth/Dockerfile` → Port 8081
- `user-service/Dockerfile` → Port 8082
- etc.

**Hoặc dùng script tự động (Linux/Mac):**
```bash
chmod +x create-dockerfiles.sh
./create-dockerfiles.sh
```

#### 5.2. Build Docker Images

```bash
# Build từng service
cd discovery-server
docker build -t discovery-server:latest .
cd ../api-gateway
docker build -t api-gateway:latest .
cd ../service-auth
docker build -t service-auth:latest .
cd ../user-service
docker build -t user-service:latest .
cd ../service-customer
docker build -t service-customer:latest .
cd ../service-product
docker build -t service-product:latest .
cd ../inventory-service
docker build -t inventory-service:latest .
cd ../order-service
docker build -t order-service:latest .
cd ../promotion-service
docker build -t promotion-service:latest .
cd ../payment-service
docker build -t payment-service:latest .
cd ..
```

#### 5.3. Copy Docker Compose File

```bash
# Windows
copy docker-compose.example.yml docker-compose.yml

# Linux/Mac
cp docker-compose.example.yml docker-compose.yml
```

#### 5.4. Chạy Docker Compose

```bash
docker-compose up -d
```

**Xem logs:**
```bash
docker-compose logs -f
```

**Kiểm tra services đang chạy:**
```bash
docker-compose ps
```

---

### Phương Pháp B: Deploy Với JAR Files (Truyền Thống)

#### 5.1. Start Discovery Server (Eureka) - ĐẦU TIÊN

Mở terminal mới và chạy:

```bash
cd discovery-server
java -jar target/discovery-server-*.jar
```

**Đợi 30 giây** để Eureka khởi động xong. Bạn sẽ thấy:
```
Started DiscoveryServerApplication in X.XXX seconds
```

**Kiểm tra:** Mở browser: `http://localhost:8761`

#### 5.2. Start Các Microservices

Mở các terminal mới cho mỗi service:

**Terminal 2 - Auth Service:**
```bash
cd service-auth
java -jar target/service-auth-*.jar
```

**Terminal 3 - User Service:**
```bash
cd user-service
java -jar target/user-service-*.jar
```

**Terminal 4 - Customer Service:**
```bash
cd service-customer
java -jar target/service-customer-*.jar
```

**Terminal 5 - Product Service:**
```bash
cd service-product
java -jar target/service-product-*.jar
```

**Terminal 6 - Inventory Service:**
```bash
cd inventory-service
java -jar target/inventory-service-*.jar
```

**Terminal 7 - Promotion Service:**
```bash
cd promotion-service
java -jar target/promotion-service-*.jar
```

**Terminal 8 - Order Service:**
```bash
cd order-service
java -jar target/order-service-*.jar
```

**Terminal 9 - Payment Service:**
```bash
cd payment-service
java -jar target/payment-service-*.jar
```

#### 5.3. Start API Gateway - CUỐI CÙNG

**Terminal 10 - API Gateway:**
```bash
cd api-gateway
java -jar target/api-gateway-*.jar
```

**Lưu ý:** API Gateway phải start CUỐI CÙNG vì cần biết các services đã đăng ký với Eureka.

---

## 6. Kiểm Tra

### 6.1. Kiểm Tra Eureka Dashboard

Mở browser: `http://localhost:8761`

**Bạn sẽ thấy danh sách các services đã đăng ký:**
- ✅ service-auth
- ✅ user-service
- ✅ service-customer
- ✅ service-product
- ✅ inventory-service
- ✅ order-service
- ✅ promotion-service
- ✅ payment-service
- ✅ api-gateway

### 6.2. Kiểm Tra Health Endpoints

Mở terminal mới và chạy:

```bash
# API Gateway
curl http://localhost:8085/actuator/health

# Product Service
curl http://localhost:8084/actuator/health

# Order Service
curl http://localhost:8088/actuator/health

# Auth Service
curl http://localhost:8081/actuator/health
```

**Kết quả mong đợi:**
```json
{"status":"UP"}
```

### 6.3. Test API Gateway

```bash
# Test endpoint qua API Gateway
curl http://localhost:8085/api/products

# Hoặc mở browser:
# http://localhost:8085/api/products
```

### 6.4. Kiểm Tra Logs

**Với Docker:**
```bash
docker-compose logs -f discovery-server
docker-compose logs -f api-gateway
```

**Với JAR files:**
- Xem logs trong các terminal đã mở
- Hoặc logs sẽ hiển thị trực tiếp trong terminal

---

## 🔧 Troubleshooting

### Lỗi: Port đã được sử dụng

**Giải pháp:**
```bash
# Windows - Tìm process đang dùng port
netstat -ano | findstr :8084

# Kill process (thay PID bằng số thật)
taskkill /PID <PID> /F

# Hoặc đổi port trong application.properties
```

### Lỗi: Database connection failed

**Kiểm tra:**
1. MySQL đã chạy chưa?
2. Database đã được tạo chưa?
3. Username/password trong `.env` đúng chưa?
4. User đã có quyền truy cập database chưa?

**Test kết nối:**
```bash
mysql -u app_user -p -h localhost product_db
```

### Lỗi: Service không đăng ký với Eureka

**Nguyên nhân:**
- Eureka chưa khởi động xong
- Network không kết nối được

**Giải pháp:**
1. Đợi Eureka khởi động xong (30 giây)
2. Kiểm tra Eureka đang chạy: `http://localhost:8761`
3. Kiểm tra logs của service để xem lỗi cụ thể

### Lỗi: Environment variables không được đọc

**Kiểm tra:**
1. File `.env` có đúng format không?
2. File `.env` có trong cùng thư mục với docker-compose.yml không?
3. Với JAR files, cần set environment variables thủ công:

```bash
# Windows PowerShell
$env:DB_PASSWORD="your-password"
$env:JWT_SECRET="your-secret"
java -jar service-product/target/*.jar

# Windows CMD
set DB_PASSWORD=your-password
set JWT_SECRET=your-secret
java -jar service-product/target/*.jar

# Linux/Mac
export DB_PASSWORD=your-password
export JWT_SECRET=your-secret
java -jar service-product/target/*.jar
```

---

## ✅ Checklist Hoàn Thành

- [ ] Java 17 đã được cài
- [ ] MySQL đã được cài và chạy
- [ ] Databases đã được tạo
- [ ] User và quyền đã được cấp
- [ ] File `.env` đã được tạo và điền keys
- [ ] Services đã được build thành công
- [ ] JAR files đã được tạo
- [ ] Discovery Server đã start và chạy
- [ ] Các microservices đã start
- [ ] API Gateway đã start
- [ ] Eureka Dashboard hiển thị tất cả services
- [ ] Health endpoints trả về "UP"
- [ ] API Gateway có thể route requests

---

## 🎉 Hoàn Thành!

Backend của bạn đã được deploy thành công!

**Các endpoints chính:**
- Eureka Dashboard: `http://localhost:8761`
- API Gateway: `http://localhost:8085`
- Product API: `http://localhost:8085/api/products`
- Order API: `http://localhost:8085/api/orders`
- Auth API: `http://localhost:8085/api/auth`

---

## 📚 Tài Liệu Tham Khảo

- `HUONG_DAN_DEPLOY_CHI_TIET.md` - Hướng dẫn chi tiết hơn
- `THU_TU_DEPLOY.md` - Thứ tự start services
- `DEPLOY_KHONG_CAN_GITHUB.md` - Deploy không cần GitHub

---

**Chúc bạn deploy thành công!** 🚀

