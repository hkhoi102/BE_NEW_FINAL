# 🚀 Hướng Dẫn Deploy Backend Smart Retail - Chi Tiết Từng Bước

## 📋 Mục Lục

1. [Yêu Cầu Hệ Thống](#yêu-cầu-hệ-thống)
2. [Chuẩn Bị Môi Trường](#chuẩn-bị-môi-trường)
3. [Setup Database](#setup-database)
4. [Cấu Hình Environment Variables](#cấu-hình-environment-variables)
5. [Build Ứng Dụng](#build-ứng-dụng)
6. [Deploy với Docker (Khuyến nghị)](#deploy-với-docker-khuyến-nghị)
7. [Deploy Truyền Thống (JAR Files)](#deploy-truyền-thống-jar-files)
8. [Kiểm Tra và Troubleshooting](#kiểm-tra-và-troubleshooting)

---

## 1. Yêu Cầu Hệ Thống

### Phần Mềm Cần Thiết

- **Java 17** (JDK 17) - Tối thiểu Java 17, hỗ trợ Java 21
- **Maven 3.6+** (hoặc dùng `mvnw` có sẵn trong project)
- **MySQL 8.0+**
- **Docker & Docker Compose** (nếu deploy bằng Docker)
- **Python 3.11+** (cho AI Service)

### Tài Nguyên Server

- **RAM**: Tối thiểu 4GB (khuyến nghị 8GB+)
- **CPU**: 2 cores trở lên
- **Disk**: 20GB+ trống
- **Network**: Các ports cần mở (xem bên dưới)

### Ports Cần Mở

| Service | Port | Mô Tả |
|---------|------|-------|
| Discovery Server | 8761 | Eureka Server |
| API Gateway | 8085 | Entry point cho tất cả requests |
| Auth Service | 8081 | Authentication |
| User Service | 8082 | User management |
| Customer Service | 8083 | Customer management |
| Product Service | 8084 | Product management |
| Order Service | 8088 | Order management |
| Inventory Service | 8086 | Inventory management |
| Promotion Service | 8087 | Promotion management |
| Payment Service | 8090 | Payment processing |
| AI Service | 8000 | AI Chat service |
| MySQL | 3306 | Database |

---

## 2. Chuẩn Bị Môi Trường

### 2.1. Kiểm Tra Java

```bash
java -version
# Phải hiển thị: openjdk version "17" hoặc "21" (tối thiểu Java 17)
```

Nếu chưa có Java 17 hoặc 21:
- **Windows**: Download từ [Adoptium](https://adoptium.net/) - chọn JDK 17 hoặc 21
- **Linux**:
  ```bash
  sudo apt update
  sudo apt install openjdk-17-jdk
  # hoặc
  sudo apt install openjdk-21-jdk
  ```
- **Mac**:
  ```bash
  brew install openjdk@17
  # hoặc
  brew install openjdk@21
  ```

### 2.2. Kiểm Tra Maven

```bash
mvn -version
# Hoặc dùng mvnw có sẵn:
./mvnw -version  # Linux/Mac
.\mvnw.cmd -version  # Windows
```

### 2.3. Kiểm Tra MySQL

```bash
mysql --version
```

Cài đặt MySQL nếu chưa có:
- **Windows**: Download từ [MySQL Official](https://dev.mysql.com/downloads/)
- **Linux**:
  ```bash
  sudo apt install mysql-server
  sudo systemctl start mysql
  sudo systemctl enable mysql
  ```
- **Mac**:
  ```bash
  brew install mysql
  brew services start mysql
  ```

### 2.4. Cài Docker (Nếu dùng Docker)

```bash
docker --version
docker-compose --version
```

Cài đặt:
- **Windows/Mac**: Download [Docker Desktop](https://www.docker.com/products/docker-desktop)
- **Linux**:
  ```bash
  curl -fsSL https://get.docker.com -o get-docker.sh
  sudo sh get-docker.sh
  sudo usermod -aG docker $USER
  ```

---

## 3. Setup Database

### 3.1. Tạo Databases

Đăng nhập MySQL:
```bash
mysql -u root -p
```

Chạy các lệnh sau:
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

-- Tạo user cho ứng dụng (thay 'your_password' bằng password thật)
CREATE USER IF NOT EXISTS 'app_user'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON product_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON customer_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON user_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON auth_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON promotion_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON analytics_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON chatbox_db.* TO 'app_user'@'%';
FLUSH PRIVILEGES;

-- Tạo user read-only cho AI Service (nếu cần)
CREATE USER IF NOT EXISTS 'reader'@'%' IDENTIFIED BY 'reader_password';
GRANT SELECT ON product_db.* TO 'reader'@'%';
GRANT SELECT ON order_db.* TO 'reader'@'%';
GRANT SELECT ON inventory_db.* TO 'reader'@'%';
FLUSH PRIVILEGES;

EXIT;
```

### 3.2. Kiểm Tra Kết Nối

```bash
mysql -u app_user -p -h localhost product_db
# Nhập password, nếu kết nối được là OK
```

---

## 4. Cấu Hình Environment Variables

### 4.1. Tạo File .env

Copy file template:
```bash
# Windows
copy env.example .env

# Linux/Mac
cp env.example .env
```

### 4.2. Điền Thông Tin Vào .env

Mở file `.env` và điền các giá trị thật:

```bash
# Database
DB_USERNAME=app_user
DB_PASSWORD=your_database_password

# JWT Secret (phải có ít nhất 32 ký tự)
JWT_SECRET=your-very-strong-jwt-secret-key-at-least-32-characters-long

# AWS S3 (cho service-product)
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
AWS_REGION=ap-southeast-2
AWS_S3_BUCKET=your-bucket-name
AWS_S3_FOLDER=product-images

# Email (cho order-service, user-service)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password
MAIL_FROM=your-email@gmail.com

# SePay (cho payment-service)
SEPAY_API_KEY=your-sepay-api-key
SEPAY_SECRET=your-sepay-secret
SEPAY_ACCOUNT_NUMBER=your-account-number
SEPAY_ACCOUNT_NAME=your-account-name
SEPAY_BANK_CODE=your-bank-code

# AI Service
GOOGLE_API_KEY=your-google-api-key
MODEL_NAME=gemini-2.5-flash
MYSQL_URL=mysql+pymysql://reader:reader_password@localhost:3306/product_db
```

**Lưu ý**:
- File `.env` đã được `.gitignore`, sẽ không bị commit lên git
- Giữ file này an toàn, không chia sẻ công khai

---

## 5. Build Ứng Dụng

### 5.1. Build Tất Cả Services

Từ thư mục root của project:

```bash
# Windows
.\mvnw.cmd clean package -DskipTests

# Linux/Mac
./mvnw clean package -DskipTests
```

Hoặc build từng service:
```bash
cd discovery-server
../mvnw.cmd clean package -DskipTests
cd ../api-gateway
../mvnw.cmd clean package -DskipTests
# ... tiếp tục với các service khác
```

### 5.2. Kiểm Tra JAR Files

Sau khi build, các file JAR sẽ nằm trong `target/` của mỗi service:
```
discovery-server/target/discovery-server-*.jar
api-gateway/target/api-gateway-*.jar
service-auth/target/service-auth-*.jar
...
```

---

## 6. Deploy với Docker (Khuyến nghị)

### 6.1. Tạo Dockerfile cho Mỗi Service

Tạo file `Dockerfile` trong mỗi service (ví dụ `service-product/Dockerfile`):

```dockerfile
FROM openjdk:17-jdk-slim
# Hoặc dùng Java 21: FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy JAR file
COPY target/*.jar app.jar

# Expose port
EXPOSE 8084

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Lưu ý**: Tạo Dockerfile tương tự cho tất cả services, chỉ thay port.

### 6.2. Build Docker Images

```bash
# Build từng service
cd service-product
docker build -t service-product:latest .
cd ../order-service
docker build -t order-service:latest .
# ... tiếp tục với các service khác
```

Hoặc dùng script tự động (tạo file `build-docker.sh`):

```bash
#!/bin/bash
services=("discovery-server" "api-gateway" "service-auth" "user-service"
          "service-customer" "service-product" "inventory-service"
          "order-service" "promotion-service" "payment-service")

for service in "${services[@]}"; do
  echo "Building $service..."
  cd $service
  docker build -t ${service}:latest .
  cd ..
done
```

### 6.3. Sử Dụng Docker Compose

Copy file mẫu:
```bash
copy docker-compose.example.yml docker-compose.yml
```

Sửa file `docker-compose.yml` nếu cần, sau đó chạy:

```bash
docker-compose up -d
```

Kiểm tra logs:
```bash
docker-compose logs -f
```

Kiểm tra services đang chạy:
```bash
docker-compose ps
```

### 6.4. Thứ Tự Khởi Động Services

**Quan trọng**: Phải khởi động theo thứ tự:

1. **MySQL** (nếu dùng Docker)
2. **Discovery Server** (Eureka) - Port 8761
3. **Các Microservices** (auth, user, product, etc.)
4. **API Gateway** - Port 8085
5. **AI Service** (nếu cần)

Docker Compose sẽ tự động xử lý dependencies nếu bạn cấu hình đúng.

---

## 7. Deploy Truyền Thống (JAR Files)

### 7.1. Copy JAR Files Lên Server

```bash
# Tạo thư mục trên server
mkdir -p /opt/smart-retail/services

# Copy JAR files
scp discovery-server/target/*.jar user@server:/opt/smart-retail/services/
scp api-gateway/target/*.jar user@server:/opt/smart-retail/services/
# ... tiếp tục
```

### 7.2. Tạo Systemd Services

Tạo file `/etc/systemd/system/discovery-server.service`:

```ini
[Unit]
Description=Discovery Server (Eureka)
After=network.target mysql.service

[Service]
Type=simple
User=your-user
WorkingDirectory=/opt/smart-retail/services
Environment="SPRING_PROFILES_ACTIVE=prod"
EnvironmentFile=/opt/smart-retail/.env
ExecStart=/usr/bin/java -jar /opt/smart-retail/services/discovery-server-*.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Tạo tương tự cho các services khác, chỉ thay:
- `Description`
- `ExecStart` (đường dẫn JAR file)

### 7.3. Enable và Start Services

```bash
# Enable services
sudo systemctl enable discovery-server
sudo systemctl enable api-gateway
sudo systemctl enable service-auth
# ... tiếp tục

# Start services (theo thứ tự)
sudo systemctl start discovery-server
sleep 30  # Đợi Eureka khởi động
sudo systemctl start service-auth
sudo systemctl start user-service
# ... tiếp tục với các service khác
sudo systemctl start api-gateway  # Start cuối cùng
```

### 7.4. Kiểm Tra Status

```bash
# Xem status
sudo systemctl status discovery-server
sudo systemctl status api-gateway

# Xem logs
sudo journalctl -u discovery-server -f
sudo journalctl -u api-gateway -f
```

---

## 8. Kiểm Tra và Troubleshooting

### 8.1. Kiểm Tra Services Đang Chạy

#### Với Docker:
```bash
docker-compose ps
docker-compose logs discovery-server
docker-compose logs api-gateway
```

#### Với Systemd:
```bash
sudo systemctl status discovery-server
sudo systemctl status api-gateway
```

### 8.2. Kiểm Tra Eureka Dashboard

Mở browser: `http://your-server-ip:8761`

Bạn sẽ thấy danh sách các services đã đăng ký. Nếu không thấy service nào, kiểm tra:
- Service có kết nối được đến Eureka không?
- Port 8761 có bị chặn không?
- Logs của service có lỗi gì không?

### 8.3. Kiểm Tra API Gateway

```bash
# Health check
curl http://localhost:8085/actuator/health

# Test endpoint
curl http://localhost:8085/api/products
```

### 8.4. Kiểm Tra Database Connection

```bash
# Test kết nối từ service
docker exec -it <container-name> mysql -u app_user -p -h mysql-host product_db
```

### 8.5. Common Issues và Giải Pháp

#### Issue 1: Service không đăng ký được với Eureka

**Nguyên nhân**:
- Eureka chưa khởi động xong
- Network không kết nối được

**Giải pháp**:
```bash
# Kiểm tra Eureka đã chạy chưa
curl http://localhost:8761

# Kiểm tra logs
docker-compose logs discovery-server
```

#### Issue 2: Database connection failed

**Nguyên nhân**:
- Database chưa được tạo
- Username/password sai
- Database server không accessible

**Giải pháp**:
```bash
# Kiểm tra database
mysql -u app_user -p -h localhost product_db

# Kiểm tra environment variables
docker exec <container> env | grep DB_
```

#### Issue 3: Port already in use

**Nguyên nhân**: Port đã được sử dụng bởi process khác

**Giải pháp**:
```bash
# Tìm process đang dùng port
# Linux/Mac
lsof -i :8084
# Windows
netstat -ano | findstr :8084

# Kill process hoặc đổi port trong application.properties
```

#### Issue 4: Out of memory

**Nguyên nhân**: JVM không đủ memory

**Giải pháp**:
Thêm vào Dockerfile hoặc systemd service:
```bash
JAVA_OPTS="-Xms512m -Xmx1024m"
```

#### Issue 5: Environment variables không được đọc

**Nguyên nhân**:
- File .env không được load
- Tên biến sai

**Giải pháp**:
```bash
# Kiểm tra environment variables trong container
docker exec <container> env

# Kiểm tra file .env có đúng format không
cat .env
```

### 8.6. Monitoring và Logs

#### Xem logs real-time:
```bash
# Docker
docker-compose logs -f

# Systemd
sudo journalctl -u service-name -f
```

#### Health Checks:
```bash
# Eureka
curl http://localhost:8761/actuator/health

# API Gateway
curl http://localhost:8085/actuator/health

# Các services
curl http://localhost:8084/actuator/health  # Product Service
curl http://localhost:8088/actuator/health  # Order Service
```

---

## 9. Deploy AI Service (Python)

### 9.1. Setup Python Environment

```bash
cd ai-service
python -m venv venv

# Windows
venv\Scripts\activate

# Linux/Mac
source venv/bin/activate
```

### 9.2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 9.3. Setup Environment Variables

Tạo file `ai-service/.env`:
```bash
GOOGLE_API_KEY=your-google-api-key
MODEL_NAME=gemini-2.5-flash
USE_GEMINI=true
MYSQL_URL=mysql+pymysql://reader:reader_password@localhost:3306/product_db
```

### 9.4. Run AI Service

```bash
# Development
uvicorn app.main:app --reload --port 8000

# Production (với Docker)
cd ai-service
docker build -t ai-service:latest .
docker run -d -p 8000:8000 --env-file .env ai-service:latest
```

---

## 10. Checklist Trước Khi Deploy Production

- [ ] Đã setup MySQL và tạo tất cả databases
- [ ] Đã tạo file `.env` với tất cả keys thật
- [ ] Đã build tất cả JAR files thành công
- [ ] Đã test kết nối database từ server
- [ ] Đã mở tất cả ports cần thiết trên firewall
- [ ] Đã cấu hình reverse proxy (nginx) nếu cần
- [ ] Đã setup SSL/TLS certificates nếu cần HTTPS
- [ ] Đã cấu hình backup database
- [ ] Đã setup monitoring và logging
- [ ] Đã test tất cả endpoints

---

## 11. Tài Liệu Tham Khảo

- `DEPLOYMENT_GUIDE.md` - Hướng dẫn quản lý keys và secrets
- `HUONG_DAN_DEPLOY.md` - Hướng dẫn environment variables
- `env.example` - Template cho file .env
- `docker-compose.example.yml` - Template Docker Compose

---

**Chúc bạn deploy thành công!** 🎉

Nếu gặp vấn đề, kiểm tra logs và xem phần Troubleshooting ở trên.

