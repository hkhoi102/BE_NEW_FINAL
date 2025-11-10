# 📋 Danh Sách Tất Cả Services - Smart Retail Backend

## 🎯 Tổng Quan

Hệ thống Smart Retail Backend bao gồm **11 services** được xây dựng theo kiến trúc microservices.

---

## 📦 Chi Tiết Từng Service

### 1. Discovery Server (Eureka)
- **Port**: 8761
- **Technology**: Java Spring Boot + Netflix Eureka
- **Mô tả**: Service Discovery Server, quản lý đăng ký và tìm kiếm các microservices
- **Dependencies**: Không có (chạy đầu tiên)
- **Environment Variables**: Không cần đặc biệt
- **URL**: `http://localhost:8761` (Eureka Dashboard)

### 2. API Gateway
- **Port**: 8085
- **Technology**: Java Spring Cloud Gateway
- **Mô tả**: Entry point cho tất cả API requests, routing đến các microservices
- **Dependencies**: Discovery Server
- **Environment Variables**:
  - `JWT_SECRET` (để validate tokens)
  - `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`
- **URL**: `http://localhost:8085`
- **Routes**:
  - `/api/auth/**` → Auth Service
  - `/api/users/**` → User Service
  - `/api/customers/**` → Customer Service
  - `/api/products/**` → Product Service
  - `/api/categories/**` → Product Service
  - `/api/orders/**` → Order Service
  - `/api/inventory/**` → Inventory Service
  - `/api/promotions/**` → Promotion Service
  - `/api/payments/**` → Payment Service

### 3. Auth Service
- **Port**: 8081
- **Technology**: Java Spring Boot
- **Mô tả**: Xác thực người dùng, tạo và validate JWT tokens
- **Dependencies**: Discovery Server, MySQL (auth_db)
- **Environment Variables**:
  - `JWT_SECRET` (bắt buộc, tối thiểu 32 ký tự)
  - `DB_USERNAME`
  - `DB_PASSWORD`
- **Database**: `auth_db`
- **URL**: `http://localhost:8081`

### 4. User Service
- **Port**: 8082
- **Technology**: Java Spring Boot
- **Mô tả**: Quản lý thông tin người dùng, gửi email
- **Dependencies**: Discovery Server, MySQL (user_db)
- **Environment Variables**:
  - `JWT_SECRET`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `MAIL_HOST` (smtp.gmail.com)
  - `MAIL_PORT` (587)
  - `MAIL_USERNAME` (email@gmail.com)
  - `MAIL_PASSWORD` (App Password, không phải password thường)
  - `MAIL_FROM` (email@gmail.com)
- **Database**: `user_db`
- **URL**: `http://localhost:8082`

### 5. Customer Service
- **Port**: 8083
- **Technology**: Java Spring Boot
- **Mô tả**: Quản lý thông tin khách hàng
- **Dependencies**: Discovery Server, MySQL (customer_db)
- **Environment Variables**:
  - `JWT_SECRET`
  - `DB_USERNAME`
  - `DB_PASSWORD`
- **Database**: `customer_db`
- **URL**: `http://localhost:8083`

### 6. Product Service
- **Port**: 8084
- **Technology**: Java Spring Boot
- **Mô tả**: Quản lý sản phẩm, categories, upload ảnh lên AWS S3
- **Dependencies**: Discovery Server, MySQL (product_db), AWS S3
- **Environment Variables**:
  - `JWT_SECRET`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `AWS_ACCESS_KEY_ID` (bắt buộc)
  - `AWS_SECRET_ACCESS_KEY` (bắt buộc)
  - `AWS_REGION` (ví dụ: ap-southeast-2)
  - `AWS_S3_BUCKET` (tên bucket)
  - `AWS_S3_FOLDER` (thư mục lưu ảnh, ví dụ: product-images)
- **Database**: `product_db`
- **URL**: `http://localhost:8084`
- **Features**: Upload/download ảnh sản phẩm từ S3

### 7. Inventory Service
- **Port**: 8086
- **Technology**: Java Spring Boot
- **Mô tả**: Quản lý kho hàng, số lượng tồn kho
- **Dependencies**: Discovery Server, MySQL (inventory_db)
- **Environment Variables**:
  - `JWT_SECRET`
  - `DB_USERNAME`
  - `DB_PASSWORD`
- **Database**: `inventory_db`
- **URL**: `http://localhost:8086`

### 8. Order Service
- **Port**: 8088
- **Technology**: Java Spring Boot
- **Mô tả**: Quản lý đơn hàng, gửi email xác nhận đơn hàng
- **Dependencies**: Discovery Server, MySQL (order_db)
- **Environment Variables**:
  - `JWT_SECRET`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `MAIL_HOST`
  - `MAIL_PORT`
  - `MAIL_USERNAME`
  - `MAIL_PASSWORD`
  - `MAIL_FROM`
- **Database**: `order_db`
- **URL**: `http://localhost:8088`
- **Features**: Gửi email xác nhận đơn hàng

### 9. Promotion Service
- **Port**: 8087
- **Technology**: Java Spring Boot
- **Mô tả**: Quản lý khuyến mãi, giảm giá
- **Dependencies**: Discovery Server, MySQL (promotion_db)
- **Environment Variables**:
  - `JWT_SECRET`
  - `DB_USERNAME`
  - `DB_PASSWORD`
- **Database**: `promotion_db`
- **URL**: `http://localhost:8087`

### 10. Payment Service
- **Port**: 8090
- **Technology**: Java Spring Boot
- **Mô tả**: Xử lý thanh toán qua SePay API
- **Dependencies**: Discovery Server, MySQL (nếu có)
- **Environment Variables**:
  - `JWT_SECRET`
  - `SEPAY_API_URL` (https://api.sepay.vn)
  - `SEPAY_API_KEY` (bắt buộc)
  - `SEPAY_SECRET` (bắt buộc)
  - `SEPAY_ACCOUNT_NUMBER`
  - `SEPAY_ACCOUNT_NAME`
  - `SEPAY_BANK_CODE`
  - `SEPAY_WEBHOOK_VERIFY` (true/false)
- **URL**: `http://localhost:8090`
- **Features**: Tích hợp SePay payment gateway

### 11. AI Service
- **Port**: 8000
- **Technology**: Python FastAPI
- **Mô tả**: Chatbot AI sử dụng Google Gemini hoặc OpenAI
- **Dependencies**: MySQL (read-only access)
- **Environment Variables**:
  - `GOOGLE_API_KEY` (nếu dùng Gemini)
  - `OPENAI_API_KEY` (nếu dùng OpenAI)
  - `MODEL_NAME` (ví dụ: gemini-2.5-flash)
  - `USE_GEMINI` (true/false)
  - `MYSQL_URL` (connection string, ví dụ: mysql+pymysql://reader:password@localhost:3306/product_db)
- **Database**: Read-only access đến `product_db`, `order_db`, `inventory_db`
- **URL**: `http://localhost:8000`
- **Features**:
  - Chatbot trả lời câu hỏi về sản phẩm
  - Đọc thông tin từ database để trả lời

---

## 🔄 Thứ Tự Khởi Động

### Bắt Buộc Phải Theo Thứ Tự:

1. **MySQL** - Database server
2. **Discovery Server (Eureka)** - Port 8761 (phải start đầu tiên)
3. **Các Microservices** (có thể start song song):
   - Auth Service (8081)
   - User Service (8082)
   - Customer Service (8083)
   - Product Service (8084)
   - Inventory Service (8086)
   - Promotion Service (8087)
   - Order Service (8088)
   - Payment Service (8090)
4. **API Gateway** - Port 8085 (phải start cuối cùng)
5. **AI Service** - Port 8000 (có thể start bất cứ lúc nào)

### Lý Do Thứ Tự:

- **Discovery Server** phải start đầu tiên vì tất cả services khác cần đăng ký với nó
- **API Gateway** phải start cuối cùng vì cần biết các services khác đã đăng ký với Eureka
- **AI Service** độc lập, có thể start bất cứ lúc nào

---

## 🗄️ Databases

| Database | Services Sử Dụng | Mô Tả |
|----------|------------------|-------|
| `auth_db` | Auth Service | Lưu thông tin authentication |
| `user_db` | User Service | Lưu thông tin người dùng |
| `customer_db` | Customer Service | Lưu thông tin khách hàng |
| `product_db` | Product Service, AI Service (read-only) | Lưu thông tin sản phẩm, categories |
| `inventory_db` | Inventory Service, AI Service (read-only) | Lưu thông tin kho hàng |
| `order_db` | Order Service, AI Service (read-only) | Lưu thông tin đơn hàng |
| `promotion_db` | Promotion Service | Lưu thông tin khuyến mãi |
| `analytics_db` | Analytics Service (nếu có) | Lưu dữ liệu phân tích |
| `chatbox_db` | AI Service (nếu có) | Lưu lịch sử chat |

---

## 🔐 Environment Variables Tổng Hợp

### Tất Cả Services Cần:

```bash
# Database
DB_USERNAME=app_user
DB_PASSWORD=your_strong_password

# JWT (tất cả services dùng chung)
JWT_SECRET=your-very-strong-jwt-secret-key-at-least-32-characters-long

# Eureka
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka/
```

### Service-Specific:

**Product Service:**
```bash
AWS_ACCESS_KEY_ID=your-aws-key
AWS_SECRET_ACCESS_KEY=your-aws-secret
AWS_REGION=ap-southeast-2
AWS_S3_BUCKET=your-bucket-name
AWS_S3_FOLDER=product-images
```

**Order Service & User Service:**
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-email@gmail.com
```

**Payment Service:**
```bash
SEPAY_API_URL=https://api.sepay.vn
SEPAY_API_KEY=your-sepay-key
SEPAY_SECRET=your-sepay-secret
SEPAY_ACCOUNT_NUMBER=your-account
SEPAY_ACCOUNT_NAME=your-name
SEPAY_BANK_CODE=your-bank-code
```

**AI Service:**
```bash
GOOGLE_API_KEY=your-google-key
MODEL_NAME=gemini-2.5-flash
USE_GEMINI=true
MYSQL_URL=mysql+pymysql://reader:password@localhost:3306/product_db
```

---

## 🌐 API Endpoints (Qua API Gateway)

Tất cả API requests đều đi qua API Gateway tại port 8085:

- **Auth**: `http://localhost:8085/api/auth/**`
- **Users**: `http://localhost:8085/api/users/**`
- **Customers**: `http://localhost:8085/api/customers/**`
- **Products**: `http://localhost:8085/api/products/**`
- **Categories**: `http://localhost:8085/api/categories/**`
- **Orders**: `http://localhost:8085/api/orders/**`
- **Inventory**: `http://localhost:8085/api/inventory/**`
- **Promotions**: `http://localhost:8085/api/promotions/**`
- **Payments**: `http://localhost:8085/api/payments/**`

**Lưu ý**: Không nên gọi trực tiếp các services, luôn đi qua API Gateway.

---

## 📊 Health Check Endpoints

Mỗi service có health check endpoint:

- Discovery Server: `http://localhost:8761/actuator/health`
- API Gateway: `http://localhost:8085/actuator/health`
- Auth Service: `http://localhost:8081/actuator/health`
- User Service: `http://localhost:8082/actuator/health`
- Customer Service: `http://localhost:8083/actuator/health`
- Product Service: `http://localhost:8084/actuator/health`
- Inventory Service: `http://localhost:8086/actuator/health`
- Promotion Service: `http://localhost:8087/actuator/health`
- Order Service: `http://localhost:8088/actuator/health`
- Payment Service: `http://localhost:8090/actuator/health`
- AI Service: `http://localhost:8000/health`

---

## 🔍 Monitoring

### Eureka Dashboard

Truy cập: `http://localhost:8761`

Hiển thị:
- Danh sách tất cả services đã đăng ký
- Status của từng service (UP/DOWN)
- Metadata của services

### Actuator Endpoints

Mỗi Spring Boot service có Actuator endpoints:
- `/actuator/health` - Health check
- `/actuator/info` - Service information
- `/actuator/metrics` - Metrics

---

## 🚀 Quick Start Commands

### Với Docker Compose:

```bash
# Start tất cả
docker-compose up -d

# Xem logs
docker-compose logs -f

# Stop tất cả
docker-compose down

# Restart một service
docker-compose restart service-product
```

### Với Systemd:

```bash
# Start tất cả
sudo systemctl start discovery-server
sleep 30
sudo systemctl start service-auth user-service service-customer service-product inventory-service order-service promotion-service payment-service
sleep 20
sudo systemctl start api-gateway

# Stop tất cả
sudo systemctl stop api-gateway
sudo systemctl stop service-auth user-service service-customer service-product inventory-service order-service promotion-service payment-service
sudo systemctl stop discovery-server

# Status
sudo systemctl status discovery-server
sudo systemctl status api-gateway
```

---

## 📚 Tài Liệu Tham Khảo

- `HUONG_DAN_DEPLOY_VPS.md` - Hướng dẫn deploy lên VPS
- `HUONG_DAN_DEPLOY_CHI_TIET.md` - Hướng dẫn deploy chi tiết
- `THU_TU_DEPLOY.md` - Thứ tự deploy services
- `DEPLOYMENT_GUIDE.md` - Hướng dẫn quản lý keys và secrets

---

**Tổng kết**: Hệ thống có 11 services, 9 databases, sử dụng Eureka cho service discovery và API Gateway làm entry point.

