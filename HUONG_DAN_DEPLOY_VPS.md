# 🚀 Hướng Dẫn Deploy Backend Smart Retail Lên VPS

## 📋 Mục Lục

1. [Tổng Quan Hệ Thống](#tổng-quan-hệ-thống)
2. [Yêu Cầu VPS](#yêu-cầu-vps)
3. [Chuẩn Bị VPS](#chuẩn-bị-vps)
4. [Setup Database](#setup-database)
5. [Cấu Hình Environment Variables](#cấu-hình-environment-variables)
6. [Build và Deploy Services](#build-và-deploy-services)
7. [Cấu Hình Nginx Reverse Proxy](#cấu-hình-nginx-reverse-proxy)
8. [Cấu Hình Firewall](#cấu-hình-firewall)
9. [Kiểm Tra và Monitoring](#kiểm-tra-và-monitoring)
10. [Troubleshooting](#troubleshooting)

---

## 📦 Tổng Quan Hệ Thống

### Danh Sách Tất Cả Services

Hệ thống Smart Retail Backend bao gồm **11 services**:

| # | Service | Port | Mô Tả | Technology |
|---|---------|------|-------|------------|
| 1 | **Discovery Server** | 8761 | Eureka Service Discovery | Java Spring Boot |
| 2 | **API Gateway** | 8085 | Entry point cho tất cả API requests | Java Spring Cloud Gateway |
| 3 | **Auth Service** | 8081 | Xác thực và phân quyền | Java Spring Boot |
| 4 | **User Service** | 8082 | Quản lý người dùng | Java Spring Boot |
| 5 | **Customer Service** | 8083 | Quản lý khách hàng | Java Spring Boot |
| 6 | **Product Service** | 8084 | Quản lý sản phẩm, upload ảnh lên S3 | Java Spring Boot |
| 7 | **Inventory Service** | 8086 | Quản lý kho hàng | Java Spring Boot |
| 8 | **Order Service** | 8088 | Quản lý đơn hàng, gửi email | Java Spring Boot |
| 9 | **Promotion Service** | 8087 | Quản lý khuyến mãi | Java Spring Boot |
| 10 | **Payment Service** | 8090 | Xử lý thanh toán (SePay) | Java Spring Boot |
| 11 | **AI Service** | 8000 | Chatbot AI (Gemini/OpenAI) | Python FastAPI |

### Kiến Trúc

```
Internet
   ↓
Nginx (Reverse Proxy) :80, :443
   ↓
API Gateway :8085
   ↓
Eureka Discovery :8761
   ↓
┌─────────────────────────────────────┐
│  Microservices (8081-8090)          │
│  - Auth, User, Customer, Product   │
│  - Inventory, Order, Promotion      │
│  - Payment                          │
└─────────────────────────────────────┘
   ↓
MySQL Database :3306
```

---

## 💻 Yêu Cầu VPS

### Tài Nguyên Tối Thiểu

- **RAM**: 4GB (khuyến nghị 8GB+)
- **CPU**: 2 cores (khuyến nghị 4 cores+)
- **Disk**: 50GB SSD (khuyến nghị 100GB+)
- **Bandwidth**: 100Mbps
- **OS**: Ubuntu 20.04 LTS hoặc 22.04 LTS (khuyến nghị)

### Phần Mềm Cần Cài

- Java 17 hoặc 21 (JDK)
- Maven 3.6+
- MySQL 8.0+
- Docker & Docker Compose (khuyến nghị)
- Nginx (reverse proxy)
- Git

### Ports Cần Mở

| Port | Service | Mô Tả |
|------|---------|-------|
| 80 | HTTP | Nginx |
| 443 | HTTPS | Nginx SSL |
| 8761 | Eureka | Discovery Server (có thể chỉ mở nội bộ) |
| 8085 | API Gateway | Entry point (có thể chỉ mở nội bộ) |
| 3306 | MySQL | Database (chỉ mở nội bộ) |
| 8000 | AI Service | AI Chat (có thể chỉ mở nội bộ) |

**Lưu ý**: Các ports 8081-8090 chỉ cần mở nội bộ, không cần expose ra ngoài vì đã có API Gateway.

---

## 🛠️ Chuẩn Bị VPS

### Bước 1: Kết Nối VPS

```bash
# SSH vào VPS
ssh root@your-vps-ip
# hoặc
ssh username@your-vps-ip
```

### Bước 2: Cập Nhật Hệ Thống

```bash
# Cập nhật package list
sudo apt update
sudo apt upgrade -y

# Cài đặt các công cụ cơ bản
sudo apt install -y curl wget git vim htop net-tools
```

### Bước 3: Cài Đặt Java 17

```bash
# Cài OpenJDK 17
sudo apt install -y openjdk-17-jdk

# Kiểm tra version
java -version
# Kết quả: openjdk version "17.0.x"

# Set JAVA_HOME (thêm vào ~/.bashrc)
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### Bước 4: Cài Đặt Maven

```bash
# Cài Maven
sudo apt install -y maven

# Kiểm tra
mvn -version
```

### Bước 5: Cài Đặt MySQL 8.0

```bash
# Cài MySQL
sudo apt install -y mysql-server

# Bảo mật MySQL
sudo mysql_secure_installation

# Khởi động và enable MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# Kiểm tra
sudo systemctl status mysql
```

### Bước 6: Cài Đặt Docker & Docker Compose

```bash
# Cài Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Thêm user vào docker group
sudo usermod -aG docker $USER

# Cài Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Kiểm tra
docker --version
docker-compose --version

# Logout và login lại để áp dụng docker group
```

### Bước 7: Cài Đặt Nginx

```bash
# Cài Nginx
sudo apt install -y nginx

# Khởi động Nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# Kiểm tra
sudo systemctl status nginx
```

### Bước 8: Cài Đặt Python 3.11 (cho AI Service)

```bash
# Cài Python 3.11
sudo apt install -y python3.11 python3.11-venv python3-pip

# Kiểm tra
python3.11 --version
```

---

## 🗄️ Setup Database

### Bước 1: Đăng Nhập MySQL

```bash
sudo mysql -u root -p
```

### Bước 2: Tạo Databases

```sql
-- Tạo tất cả databases
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS customer_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS promotion_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS analytics_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS chatbox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Kiểm tra
SHOW DATABASES;
```

### Bước 3: Tạo User và Cấp Quyền

```sql
-- Tạo user cho ứng dụng (THAY 'your_strong_password' bằng password mạnh)
CREATE USER IF NOT EXISTS 'app_user'@'%' IDENTIFIED BY 'your_strong_password';

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
CREATE USER IF NOT EXISTS 'reader'@'%' IDENTIFIED BY 'reader_strong_password';
GRANT SELECT ON product_db.* TO 'reader'@'%';
GRANT SELECT ON order_db.* TO 'reader'@'%';
GRANT SELECT ON inventory_db.* TO 'reader'@'%';

-- Áp dụng thay đổi
FLUSH PRIVILEGES;

-- Thoát
EXIT;
```

### Bước 4: Cấu Hình MySQL Cho Remote Access

```bash
# Sửa file cấu hình MySQL
sudo nano /etc/mysql/mysql.conf.d/mysqld.cnf

# Tìm dòng: bind-address = 127.0.0.1
# Đổi thành: bind-address = 0.0.0.0
# (Hoặc comment: #bind-address = 127.0.0.1)

# Restart MySQL
sudo systemctl restart mysql
```

### Bước 5: Kiểm Tra Kết Nối

```bash
# Test kết nối
mysql -u app_user -p -h localhost product_db
# Nhập password, nếu kết nối được là OK
```

---

## 🔐 Cấu Hình Environment Variables

### Bước 1: Clone Project Lên VPS

```bash
# Tạo thư mục cho project
mkdir -p /opt/smart-retail
cd /opt/smart-retail

# Clone project (thay URL bằng repo của bạn)
git clone https://github.com/your-username/smart-retail-backend.git
cd smart-retail-backend

# Hoặc upload code lên VPS bằng SCP/SFTP
```

### Bước 2: Tạo File .env

```bash
# Copy template
cp env.example .env

# Sửa file .env
nano .env
```

### Bước 3: Điền Thông Tin Vào .env

```bash
# ============================================
# Database Configuration
# ============================================
DB_USERNAME=app_user
DB_PASSWORD=your_strong_password_here
DB_HOST=localhost
DB_PORT=3306

# ============================================
# JWT Secret (PHẢI có ít nhất 32 ký tự)
# ============================================
JWT_SECRET=your-very-strong-jwt-secret-key-at-least-32-characters-long-change-this-in-production

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
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password-16-chars
MAIL_FROM=your-email@gmail.com

# ============================================
# SePay Configuration (cho payment-service)
# ============================================
SEPAY_API_URL=https://api.sepay.vn
SEPAY_API_KEY=your-sepay-api-key
SEPAY_SECRET=your-sepay-secret
SEPAY_ACCOUNT_NUMBER=your-account-number
SEPAY_ACCOUNT_NAME=your-account-name
SEPAY_BANK_CODE=your-bank-code
SEPAY_WEBHOOK_VERIFY=false

# ============================================
# AI Service Configuration
# ============================================
GOOGLE_API_KEY=your-google-api-key
MODEL_NAME=gemini-2.5-flash
USE_GEMINI=true
MYSQL_URL=mysql+pymysql://reader:reader_strong_password@localhost:3306/product_db

# ============================================
# Eureka Discovery Server
# ============================================
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka/

# ============================================
# Spring Profiles
# ============================================
SPRING_PROFILES_ACTIVE=prod
```

**Lưu ý quan trọng:**
- Thay TẤT CẢ các giá trị `your_*` bằng giá trị thật
- File `.env` đã được `.gitignore`, sẽ không bị commit
- Giữ file này an toàn, không chia sẻ công khai

### Bước 4: Tạo File .env Cho AI Service

```bash
# Tạo .env cho AI Service
cd ai-service
cp env.sample .env
nano .env
```

Điền thông tin:
```bash
GOOGLE_API_KEY=your-google-api-key
MODEL_NAME=gemini-2.5-flash
USE_GEMINI=true
MYSQL_URL=mysql+pymysql://reader:reader_strong_password@localhost:3306/product_db
```

---

## 🏗️ Build và Deploy Services

### Phương Pháp 1: Deploy Với Docker Compose (Khuyến nghị)

#### Bước 1: Tạo Dockerfiles Cho Tất Cả Services

```bash
# Về thư mục root
cd /opt/smart-retail/smart-retail-backend

# Chạy script tạo Dockerfiles (nếu có)
chmod +x create-dockerfiles.sh
./create-dockerfiles.sh

# Hoặc tạo thủ công từng Dockerfile
```

#### Bước 2: Build Tất Cả JAR Files

```bash
# Build tất cả services
./mvnw clean package -DskipTests

# Hoặc build từng service
cd discovery-server && ../mvnw.cmd clean package -DskipTests && cd ..
cd api-gateway && ../mvnw.cmd clean package -DskipTests && cd ..
# ... tiếp tục với các service khác
```

#### Bước 3: Build Docker Images

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
cd ../ai-service
docker build -t ai-service:latest .
cd ..
```

#### Bước 4: Tạo Docker Compose File

```bash
# Copy template
cp docker-compose.example.yml docker-compose.yml

# Sửa file nếu cần (thường không cần sửa)
nano docker-compose.yml
```

#### Bước 5: Chạy Docker Compose

```bash
# Start tất cả services
docker-compose up -d

# Xem logs
docker-compose logs -f

# Kiểm tra services đang chạy
docker-compose ps
```

### Phương Pháp 2: Deploy Với Systemd (JAR Files)

#### Bước 1: Build JAR Files

```bash
cd /opt/smart-retail/smart-retail-backend
./mvnw clean package -DskipTests
```

#### Bước 2: Tạo Thư Mục Cho Services

```bash
sudo mkdir -p /opt/smart-retail/services
sudo mkdir -p /opt/smart-retail/logs
```

#### Bước 3: Copy JAR Files

```bash
# Copy tất cả JAR files
sudo cp discovery-server/target/*.jar /opt/smart-retail/services/discovery-server.jar
sudo cp api-gateway/target/*.jar /opt/smart-retail/services/api-gateway.jar
sudo cp service-auth/target/*.jar /opt/smart-retail/services/service-auth.jar
sudo cp user-service/target/*.jar /opt/smart-retail/services/user-service.jar
sudo cp service-customer/target/*.jar /opt/smart-retail/services/service-customer.jar
sudo cp service-product/target/*.jar /opt/smart-retail/services/service-product.jar
sudo cp inventory-service/target/*.jar /opt/smart-retail/services/inventory-service.jar
sudo cp order-service/target/*.jar /opt/smart-retail/services/order-service.jar
sudo cp promotion-service/target/*.jar /opt/smart-retail/services/promotion-service.jar
sudo cp payment-service/target/*.jar /opt/smart-retail/services/payment-service.jar
```

#### Bước 4: Tạo Systemd Service Files

**Discovery Server:**

```bash
sudo nano /etc/systemd/system/discovery-server.service
```

```ini
[Unit]
Description=Discovery Server (Eureka)
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/smart-retail/services
EnvironmentFile=/opt/smart-retail/smart-retail-backend/.env
ExecStart=/usr/bin/java -jar /opt/smart-retail/services/discovery-server.jar
Restart=always
RestartSec=10
StandardOutput=append:/opt/smart-retail/logs/discovery-server.log
StandardError=append:/opt/smart-retail/logs/discovery-server-error.log

[Install]
WantedBy=multi-user.target
```

**API Gateway:**

```bash
sudo nano /etc/systemd/system/api-gateway.service
```

```ini
[Unit]
Description=API Gateway
After=network.target discovery-server.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/smart-retail/services
EnvironmentFile=/opt/smart-retail/smart-retail-backend/.env
ExecStart=/usr/bin/java -jar /opt/smart-retail/services/api-gateway.jar
Restart=always
RestartSec=10
StandardOutput=append:/opt/smart-retail/logs/api-gateway.log
StandardError=append:/opt/smart-retail/logs/api-gateway-error.log

[Install]
WantedBy=multi-user.target
```

**Tạo tương tự cho các services khác**, chỉ thay:
- `Description`
- `ExecStart` (đường dẫn JAR file)
- `StandardOutput` và `StandardError` (log files)

#### Bước 5: Enable và Start Services

```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable services (tự động start khi boot)
sudo systemctl enable discovery-server
sudo systemctl enable service-auth
sudo systemctl enable user-service
sudo systemctl enable service-customer
sudo systemctl enable service-product
sudo systemctl enable inventory-service
sudo systemctl enable order-service
sudo systemctl enable promotion-service
sudo systemctl enable payment-service
sudo systemctl enable api-gateway

# Start services theo thứ tự
sudo systemctl start discovery-server
sleep 30  # Đợi Eureka khởi động

sudo systemctl start service-auth
sudo systemctl start user-service
sudo systemctl start service-customer
sudo systemctl start service-product
sudo systemctl start inventory-service
sudo systemctl start order-service
sudo systemctl start promotion-service
sudo systemctl start payment-service

sleep 20  # Đợi các services đăng ký với Eureka

sudo systemctl start api-gateway  # Start cuối cùng
```

#### Bước 6: Kiểm Tra Status

```bash
# Xem status tất cả services
sudo systemctl status discovery-server
sudo systemctl status api-gateway
sudo systemctl status service-product

# Xem logs
sudo journalctl -u discovery-server -f
sudo journalctl -u api-gateway -f
```

---

## 🌐 Cấu Hình Nginx Reverse Proxy

### Bước 1: Tạo Nginx Config

```bash
sudo nano /etc/nginx/sites-available/smart-retail
```

### Bước 2: Cấu Hình Nginx

```nginx
# HTTP Server - Redirect to HTTPS
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;

    # Redirect all HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

# HTTPS Server
server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;

    # SSL Certificates (sử dụng Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Logging
    access_log /var/log/nginx/smart-retail-access.log;
    error_log /var/log/nginx/smart-retail-error.log;

    # Client body size (cho upload ảnh)
    client_max_body_size 10M;

    # API Gateway - Main Entry Point
    location /api/ {
        proxy_pass http://localhost:8085;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        proxy_read_timeout 300s;
        proxy_connect_timeout 75s;
    }

    # Eureka Dashboard (chỉ cho admin, có thể bảo vệ bằng basic auth)
    location /eureka/ {
        proxy_pass http://localhost:8761/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Basic Auth (tùy chọn)
        # auth_basic "Eureka Dashboard";
        # auth_basic_user_file /etc/nginx/.htpasswd;
    }

    # AI Service (nếu cần expose)
    location /ai/ {
        proxy_pass http://localhost:8000/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Health Check Endpoint
    location /health {
        proxy_pass http://localhost:8085/actuator/health;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }
}
```

### Bước 3: Enable Site

```bash
# Tạo symbolic link
sudo ln -s /etc/nginx/sites-available/smart-retail /etc/nginx/sites-enabled/

# Test cấu hình
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

### Bước 4: Cài Đặt SSL Certificate (Let's Encrypt)

```bash
# Cài Certbot
sudo apt install -y certbot python3-certbot-nginx

# Lấy certificate (thay your-domain.com bằng domain của bạn)
sudo certbot --nginx -d your-domain.com -d www.your-domain.com

# Certbot sẽ tự động cấu hình SSL và renew
```

---

## 🔥 Cấu Hình Firewall

### Bước 1: Cấu Hình UFW (Ubuntu Firewall)

```bash
# Cho phép SSH
sudo ufw allow 22/tcp

# Cho phép HTTP và HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Cho phép MySQL (chỉ từ localhost, không cần mở ra ngoài)
# sudo ufw allow from 127.0.0.1 to any port 3306

# Enable firewall
sudo ufw enable

# Kiểm tra status
sudo ufw status
```

### Bước 2: Cấu Hình Fail2Ban (Bảo Vệ SSH)

```bash
# Cài Fail2Ban
sudo apt install -y fail2ban

# Khởi động
sudo systemctl start fail2ban
sudo systemctl enable fail2ban

# Kiểm tra
sudo systemctl status fail2ban
```

---

## ✅ Kiểm Tra và Monitoring

### Bước 1: Kiểm Tra Eureka Dashboard

```bash
# Mở browser: http://your-vps-ip:8761
# Hoặc qua domain: https://your-domain.com/eureka/
```

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

### Bước 2: Kiểm Tra Health Endpoints

```bash
# API Gateway
curl http://localhost:8085/actuator/health

# Product Service
curl http://localhost:8084/actuator/health

# Order Service
curl http://localhost:8088/actuator/health

# Qua domain
curl https://your-domain.com/health
```

### Bước 3: Test API Gateway

```bash
# Test endpoint qua API Gateway
curl https://your-domain.com/api/products

# Hoặc với authentication
curl -H "Authorization: Bearer YOUR_TOKEN" https://your-domain.com/api/products
```

### Bước 4: Monitoring Script

Tạo script để monitor services:

```bash
sudo nano /opt/smart-retail/monitor.sh
```

```bash
#!/bin/bash

echo "=== Smart Retail Services Status ==="
echo ""

# Check Discovery Server
if curl -s http://localhost:8761 > /dev/null; then
    echo "✅ Discovery Server: UP"
else
    echo "❌ Discovery Server: DOWN"
fi

# Check API Gateway
if curl -s http://localhost:8085/actuator/health | grep -q "UP"; then
    echo "✅ API Gateway: UP"
else
    echo "❌ API Gateway: DOWN"
fi

# Check Services
services=("8081:Auth" "8082:User" "8083:Customer" "8084:Product" "8086:Inventory" "8087:Promotion" "8088:Order" "8090:Payment")

for service in "${services[@]}"; do
    port=$(echo $service | cut -d: -f1)
    name=$(echo $service | cut -d: -f2)
    if curl -s http://localhost:$port/actuator/health | grep -q "UP"; then
        echo "✅ $name Service: UP"
    else
        echo "❌ $name Service: DOWN"
    fi
done

# Check AI Service
if curl -s http://localhost:8000/health > /dev/null; then
    echo "✅ AI Service: UP"
else
    echo "❌ AI Service: DOWN"
fi

echo ""
echo "=== Docker Containers ==="
docker-compose ps
```

```bash
# Make executable
chmod +x /opt/smart-retail/monitor.sh

# Chạy
/opt/smart-retail/monitor.sh
```

---

## 🔧 Troubleshooting

### Lỗi: Service không đăng ký được với Eureka

**Nguyên nhân:**
- Eureka chưa khởi động xong
- Network không kết nối được
- Port bị chặn

**Giải pháp:**
```bash
# Kiểm tra Eureka đã chạy chưa
curl http://localhost:8761

# Kiểm tra logs
docker-compose logs discovery-server
# hoặc
sudo journalctl -u discovery-server -f

# Kiểm tra network
docker network ls
docker network inspect smart-retail-network
```

### Lỗi: Database connection failed

**Nguyên nhân:**
- Database chưa được tạo
- Username/password sai
- MySQL không accessible

**Giải pháp:**
```bash
# Kiểm tra MySQL đang chạy
sudo systemctl status mysql

# Test kết nối
mysql -u app_user -p -h localhost product_db

# Kiểm tra environment variables
docker exec <container> env | grep DB_
# hoặc
sudo systemctl show service-product | grep Environment
```

### Lỗi: Port already in use

**Giải pháp:**
```bash
# Tìm process đang dùng port
sudo lsof -i :8084
# hoặc
sudo netstat -tulpn | grep :8084

# Kill process
sudo kill -9 <PID>
```

### Lỗi: Out of memory

**Giải pháp:**
Thêm vào Dockerfile hoặc systemd service:
```bash
JAVA_OPTS="-Xms512m -Xmx1024m"
```

Hoặc trong docker-compose.yml:
```yaml
environment:
  - JAVA_OPTS=-Xms512m -Xmx1024m
```

### Lỗi: Environment variables không được đọc

**Giải pháp:**
```bash
# Kiểm tra file .env
cat /opt/smart-retail/smart-retail-backend/.env

# Kiểm tra environment variables trong container
docker exec <container> env

# Kiểm tra systemd service
sudo systemctl show service-product | grep EnvironmentFile
```

### Lỗi: Nginx 502 Bad Gateway

**Nguyên nhân:**
- API Gateway chưa chạy
- Port không đúng

**Giải pháp:**
```bash
# Kiểm tra API Gateway
curl http://localhost:8085/actuator/health

# Kiểm tra Nginx logs
sudo tail -f /var/log/nginx/smart-retail-error.log

# Kiểm tra cấu hình Nginx
sudo nginx -t
```

---

## 📊 Maintenance

### Backup Database

```bash
# Tạo script backup
sudo nano /opt/smart-retail/backup-db.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/opt/smart-retail/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# Backup tất cả databases
mysqldump -u app_user -p'your_password' --all-databases > $BACKUP_DIR/all_databases_$DATE.sql

# Compress
gzip $BACKUP_DIR/all_databases_$DATE.sql

# Xóa backup cũ hơn 7 ngày
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_DIR/all_databases_$DATE.sql.gz"
```

```bash
# Make executable
chmod +x /opt/smart-retail/backup-db.sh

# Thêm vào crontab (backup hàng ngày lúc 2h sáng)
crontab -e
# Thêm dòng:
0 2 * * * /opt/smart-retail/backup-db.sh
```

### Update Services

```bash
# Pull code mới
cd /opt/smart-retail/smart-retail-backend
git pull

# Rebuild và restart
./mvnw clean package -DskipTests
docker-compose build
docker-compose up -d
```

### View Logs

```bash
# Docker logs
docker-compose logs -f service-product
docker-compose logs -f api-gateway

# Systemd logs
sudo journalctl -u service-product -f
sudo journalctl -u api-gateway -f

# Nginx logs
sudo tail -f /var/log/nginx/smart-retail-access.log
sudo tail -f /var/log/nginx/smart-retail-error.log
```

---

## ✅ Checklist Deploy

- [ ] VPS đã được setup với đủ tài nguyên
- [ ] Java 17 đã được cài
- [ ] MySQL đã được cài và chạy
- [ ] Databases đã được tạo
- [ ] User và quyền đã được cấp
- [ ] File `.env` đã được tạo và điền keys
- [ ] Services đã được build thành công
- [ ] Docker images đã được build (nếu dùng Docker)
- [ ] Services đã được start và chạy
- [ ] Eureka Dashboard hiển thị tất cả services
- [ ] API Gateway có thể route requests
- [ ] Nginx đã được cấu hình
- [ ] SSL certificate đã được cài
- [ ] Firewall đã được cấu hình
- [ ] Health endpoints trả về "UP"
- [ ] Backup database đã được setup

---

## 🎉 Hoàn Thành!

Backend của bạn đã được deploy thành công lên VPS!

**Các endpoints chính:**
- API Gateway: `https://your-domain.com/api/`
- Eureka Dashboard: `https://your-domain.com/eureka/`
- Health Check: `https://your-domain.com/health`

**Các API endpoints:**
- Products: `https://your-domain.com/api/products`
- Orders: `https://your-domain.com/api/orders`
- Auth: `https://your-domain.com/api/auth`
- Users: `https://your-domain.com/api/users`

---

## 📚 Tài Liệu Tham Khảo

- `HUONG_DAN_DEPLOY_CHI_TIET.md` - Hướng dẫn chi tiết từng bước
- `THU_TU_DEPLOY.md` - Thứ tự start services
- `DEPLOYMENT_GUIDE.md` - Hướng dẫn quản lý keys và secrets
- `env.example` - Template cho file .env
- `docker-compose.example.yml` - Template Docker Compose

---

**Chúc bạn deploy thành công!** 🚀

Nếu gặp vấn đề, kiểm tra logs và xem phần Troubleshooting ở trên.

