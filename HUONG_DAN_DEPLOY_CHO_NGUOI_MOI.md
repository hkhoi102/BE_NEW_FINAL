# 🚀 Hướng Dẫn Deploy Backend Cho Người Mới Bắt Đầu - Từng Bước Chi Tiết

## 📖 Giới Thiệu

Hướng dẫn này dành cho những người **chưa từng deploy** lần nào. Mình sẽ giải thích từng bước một cách đơn giản, dễ hiểu nhất.

**Thời gian ước tính**: 2-3 giờ (lần đầu tiên)

---

## 🎯 Mục Tiêu

Sau khi làm theo hướng dẫn này, bạn sẽ:
- ✅ Hiểu cách deploy backend lên VPS
- ✅ Biết cách cấu hình database
- ✅ Biết cách setup environment variables
- ✅ Biết cách chạy tất cả services
- ✅ Biết cách kiểm tra hệ thống đã chạy đúng chưa

---

## 📋 Bước 0: Hiểu Về Hệ Thống

### Hệ thống của bạn có gì?

Bạn có **11 services** (ứng dụng nhỏ) cần chạy:

1. **Discovery Server** - Quản lý các services khác
2. **API Gateway** - Cửa vào chính của hệ thống
3. **Auth Service** - Xử lý đăng nhập/đăng ký
4. **User Service** - Quản lý người dùng
5. **Customer Service** - Quản lý khách hàng
6. **Product Service** - Quản lý sản phẩm
7. **Inventory Service** - Quản lý kho
8. **Order Service** - Quản lý đơn hàng
9. **Promotion Service** - Quản lý khuyến mãi
10. **Payment Service** - Xử lý thanh toán
11. **AI Service** - Chatbot

**Tất cả đều cần chạy cùng lúc!**

### Cần gì để deploy?

1. **VPS** (Virtual Private Server) - Máy chủ ảo trên internet
2. **Domain** (tùy chọn) - Tên miền như `yourdomain.com`
3. **Các keys/API keys** - AWS, Email, SePay, Google AI, etc.

---

## 🖥️ Bước 1: Chuẩn Bị VPS

### 1.1. Mua VPS

**Các nhà cung cấp phổ biến:**
- **DigitalOcean** (dễ dùng, $6/tháng)
- **Linode** ($5/tháng)
- **Vultr** ($6/tháng)
- **AWS EC2** (phức tạp hơn)
- **VPS Việt Nam**: VNPT, FPT, Viettel

**Cấu hình tối thiểu:**
- RAM: 4GB (khuyến nghị 8GB)
- CPU: 2 cores (khuyến nghị 4 cores)
- Disk: 50GB SSD
- OS: Ubuntu 20.04 hoặc 22.04 LTS

### 1.2. Kết Nối Vào VPS

Sau khi mua VPS, bạn sẽ nhận được:
- **IP Address**: Ví dụ: `123.456.789.012`
- **Username**: Thường là `root` hoặc `ubuntu`
- **Password** hoặc **SSH Key**

#### Cách kết nối (Windows):

**Cách 1: Dùng PuTTY (dễ nhất)**

1. Download PuTTY: https://www.putty.org/
2. Mở PuTTY
3. Nhập:
   - **Host Name**: IP của VPS (ví dụ: `123.456.789.012`)
   - **Port**: 22
   - **Connection Type**: SSH
4. Click "Open"
5. Nhập username: `root` (hoặc `ubuntu`)
6. Nhập password (khi gõ password sẽ không hiện gì, cứ gõ và Enter)

**Cách 2: Dùng PowerShell (Windows 10/11)**

1. Mở PowerShell
2. Gõ lệnh:
```powershell
ssh root@123.456.789.012
# Thay 123.456.789.012 bằng IP của bạn
```
3. Nhập password khi được hỏi

**Cách 3: Dùng CMD**

1. Mở Command Prompt
2. Gõ lệnh tương tự như PowerShell

#### Kiểm tra đã kết nối thành công:

Khi thấy dòng như này là OK:
```
root@your-server:~#
```

### 1.3. Cập Nhật Hệ Thống

Sau khi kết nối vào VPS, chạy các lệnh sau:

```bash
# Cập nhật danh sách phần mềm
sudo apt update

# Cập nhật các phần mềm đã cài
sudo apt upgrade -y

# Cài các công cụ cơ bản
sudo apt install -y curl wget git vim htop
```

**Giải thích:**
- `sudo` = chạy với quyền admin
- `apt` = công cụ cài phần mềm trên Ubuntu
- `update` = cập nhật danh sách
- `upgrade` = nâng cấp phần mềm
- `-y` = tự động trả lời "yes"

**Chờ 5-10 phút** để hoàn thành.

---

## ☕ Bước 2: Cài Đặt Java

### 2.1. Cài Java 17

```bash
# Cài Java 17
sudo apt install -y openjdk-17-jdk
```

**Chờ 2-3 phút** để cài xong.

### 2.2. Kiểm Tra Java Đã Cài Chưa

```bash
java -version
```

**Kết quả mong đợi:**
```
openjdk version "17.0.x"
OpenJDK Runtime Environment (build 17.0.x)
OpenJDK 64-Bit Server VM (build 17.0.x, mixed mode, sharing)
```

Nếu thấy kết quả này = ✅ Java đã cài thành công!

### 2.3. Cài Maven

```bash
# Cài Maven
sudo apt install -y maven
```

### 2.4. Kiểm Tra Maven

```bash
mvn -version
```

**Kết quả mong đợi:**
```
Apache Maven 3.6.x
Maven home: /usr/share/maven
Java version: 17.0.x
```

Nếu thấy kết quả này = ✅ Maven đã cài thành công!

---

## 🗄️ Bước 3: Cài Đặt MySQL

### 3.1. Cài MySQL

```bash
# Cài MySQL Server
sudo apt install -y mysql-server
```

**Chờ 3-5 phút** để cài xong.

### 3.2. Khởi Động MySQL

```bash
# Khởi động MySQL
sudo systemctl start mysql

# Tự động khởi động khi VPS reboot
sudo systemctl enable mysql

# Kiểm tra MySQL đang chạy chưa
sudo systemctl status mysql
```

**Kết quả mong đợi:** Thấy dòng `Active: active (running)` = ✅ MySQL đang chạy!

### 3.3. Bảo Mật MySQL

```bash
# Chạy script bảo mật
sudo mysql_secure_installation
```

**Khi được hỏi, trả lời như sau:**

1. **"Would you like to setup VALIDATE PASSWORD plugin?"**
   - Nhập: `N` (No) - Để đơn giản, không cần validate password phức tạp

2. **"Enter password for root user:"**
   - Nhập password mạnh (ví dụ: `MySecurePass123!`)
   - **Ghi nhớ password này!** Bạn sẽ cần dùng sau

3. **"Remove anonymous users?"**
   - Nhập: `Y` (Yes)

4. **"Disallow root login remotely?"**
   - Nhập: `Y` (Yes)

5. **"Remove test database?"**
   - Nhập: `Y` (Yes)

6. **"Reload privilege tables now?"**
   - Nhập: `Y` (Yes)

### 3.4. Đăng Nhập MySQL

```bash
# Đăng nhập MySQL
sudo mysql -u root -p
```

**Nhập password** bạn vừa tạo ở bước 3.3.

**Khi thấy dòng này là OK:**
```
mysql>
```

### 3.5. Tạo Databases

Trong MySQL (sau khi đăng nhập), copy và paste từng dòng:

```sql
-- Tạo database cho Product Service
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho Order Service
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho Inventory Service
CREATE DATABASE IF NOT EXISTS inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho Customer Service
CREATE DATABASE IF NOT EXISTS customer_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho User Service
CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho Auth Service
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho Promotion Service
CREATE DATABASE IF NOT EXISTS promotion_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho Analytics (nếu cần)
CREATE DATABASE IF NOT EXISTS analytics_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho Chatbox (nếu cần)
CREATE DATABASE IF NOT EXISTS chatbox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Kiểm tra databases đã tạo:**

```sql
SHOW DATABASES;
```

**Bạn sẽ thấy danh sách:**
```
+--------------------+
| Database           |
+--------------------+
| auth_db            |
| chatbox_db         |
| customer_db        |
| information_schema |
| inventory_db       |
| order_db           |
| product_db         |
| promotion_db       |
| user_db            |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
```

### 3.6. Tạo User Cho Ứng Dụng

Vẫn trong MySQL, chạy lệnh sau (THAY `your_strong_password` bằng password mạnh):

```sql
-- Tạo user cho ứng dụng
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
```

**Lưu ý:**
- `your_strong_password` = password cho app_user (ví dụ: `AppPass123!`)
- `reader_strong_password` = password cho reader (ví dụ: `ReaderPass123!`)
- **Ghi nhớ 2 passwords này!**

### 3.7. Thoát MySQL

```sql
EXIT;
```

### 3.8. Kiểm Tra Kết Nối

```bash
# Test kết nối với user mới tạo
mysql -u app_user -p -h localhost product_db
```

**Nhập password** của `app_user`.

**Nếu kết nối được và thấy `mysql>` = ✅ OK!**

Gõ `EXIT;` để thoát.

---

## 🐳 Bước 4: Cài Đặt Docker

### 4.1. Cài Docker

```bash
# Download script cài Docker
curl -fsSL https://get.docker.com -o get-docker.sh

# Chạy script
sudo sh get-docker.sh
```

**Chờ 2-3 phút** để cài xong.

### 4.2. Thêm User Vào Docker Group

```bash
# Thêm user hiện tại vào docker group
sudo usermod -aG docker $USER

# Logout và login lại để áp dụng
# Hoặc chạy lệnh này:
newgrp docker
```

### 4.3. Kiểm Tra Docker

```bash
docker --version
```

**Kết quả mong đợi:**
```
Docker version 24.x.x
```

### 4.4. Cài Docker Compose

```bash
# Download Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# Cấp quyền thực thi
sudo chmod +x /usr/local/bin/docker-compose

# Kiểm tra
docker-compose --version
```

**Kết quả mong đợi:**
```
Docker Compose version v2.x.x
```

---

## 📦 Bước 5: Upload Code Lên VPS

### 5.1. Tạo Thư Mục

```bash
# Tạo thư mục cho project
mkdir -p /opt/smart-retail
cd /opt/smart-retail
```

### 5.2. Upload Code (Chọn 1 trong 3 cách)

#### Cách 1: Dùng Git (Nếu code đã có trên GitHub/GitLab)

```bash
# Clone project
git clone https://github.com/your-username/smart-retail-backend.git
cd smart-retail-backend
```

#### Cách 2: Dùng SCP (Từ máy Windows)

**Mở PowerShell hoặc CMD trên máy Windows:**

```powershell
# Tạo file zip của project trước
# Sau đó upload
scp -r D:\DATN\code\smart-retail-backend root@123.456.789.012:/opt/smart-retail/
# Thay IP và đường dẫn cho đúng
```

#### Cách 3: Dùng WinSCP (Dễ nhất cho người mới) ⭐

**Bước 1: Tạo thư mục trên VPS trước**

Trên VPS (qua SSH hoặc WinSCP Terminal), chạy lệnh:
```bash
mkdir -p /opt/smart-retail
```

**Bước 2: Mở WinSCP và kết nối**

1. **Download WinSCP** (nếu chưa có): https://winscp.net/
2. **Mở WinSCP**
3. **Tạo kết nối mới:**
   - Click "New Site" hoặc "New Session"
   - Điền thông tin:
     - **File protocol**: SFTP
     - **Host name**: IP của VPS (ví dụ: `103.229.52.246`)
     - **Port number**: 22
     - **User name**: `root`
     - **Password**: password của VPS
   - Click "Save" để lưu (tùy chọn)
   - Click "Login" để kết nối

**Bước 3: Upload code lên VPS**

Sau khi kết nối thành công, bạn sẽ thấy 2 cửa sổ:
- **Bên trái**: Máy tính của bạn (Local)
- **Bên phải**: VPS (Remote)

**Cách upload:**

1. **Bên trái (Local)**: Điều hướng đến thư mục chứa code
   - Ví dụ: `D:\DATN\code\`
   - Tìm thư mục `smart-retail-backend`

2. **Bên phải (Remote)**: Điều hướng đến `/opt/smart-retail/`
   - Click vào thanh địa chỉ bên phải
   - Gõ: `/opt/smart-retail`
   - Nhấn Enter

3. **Upload thư mục:**
   - **Cách 1 (Kéo thả)**: Kéo thư mục `smart-retail-backend` từ bên trái sang bên phải
   - **Cách 2 (Right-click)**:
     - Right-click vào thư mục `smart-retail-backend` bên trái
     - Chọn "Upload"
     - Chọn thư mục đích: `/opt/smart-retail/`
     - Click "OK"

4. **Chờ upload hoàn tất** (có thể mất 5-10 phút tùy kích thước code)

**Lưu ý quan trọng:**
- ✅ Đảm bảo upload **toàn bộ thư mục** `smart-retail-backend`, không chỉ một phần
- ✅ Sau khi upload, thư mục trên VPS sẽ là: `/opt/smart-retail/smart-retail-backend/`
- ✅ Nếu có file lớn, WinSCP sẽ hiện progress bar

**Bước 4: Kiểm tra quyền truy cập**

Sau khi upload xong, trên VPS (SSH hoặc WinSCP Terminal), chạy:
```bash
# Kiểm tra thư mục đã có chưa
ls -la /opt/smart-retail/

# Vào thư mục
cd /opt/smart-retail/smart-retail-backend

# Xem danh sách các thư mục con
ls -la
```

**Bạn sẽ thấy các thư mục:**
- `discovery-server/`
- `api-gateway/`
- `service-auth/`
- `user-service/`
- `service-product/`
- `inventory-service/`
- `order-service/`
- `promotion-service/`
- `payment-service/`
- `ai-service/`
- Và các file khác...

**Nếu thấy đầy đủ = ✅ Upload thành công!**

**Troubleshooting WinSCP:**

- **Lỗi "Permission denied"**:
  - Chạy lệnh trên VPS: `chmod 755 /opt/smart-retail`

- **Upload bị gián đoạn**:
  - Thử lại, WinSCP sẽ tiếp tục từ chỗ dừng

- **Không thấy thư mục trên VPS**:
  - Refresh bên phải (F5)
  - Kiểm tra lại đường dẫn `/opt/smart-retail`

### 5.3. Kiểm Tra Code Đã Upload

```bash
# Vào thư mục project
cd /opt/smart-retail/smart-retail-backend

# Xem danh sách thư mục
ls -la
```

**Bạn sẽ thấy các thư mục:**
- `discovery-server/`
- `api-gateway/`
- `service-auth/`
- `user-service/`
- `service-product/`
- `inventory-service/`
- `order-service/`
- `promotion-service/`
- `payment-service/`
- `ai-service/`

---

## 🔐 Bước 6: Cấu Hình Environment Variables

### 6.1. Tạo File .env

```bash
# Vào thư mục project
cd /opt/smart-retail/smart-retail-backend

# Copy file template
cp env.example .env

# Mở file để sửa
nano .env
```

### 6.2. Sửa File .env

**Trong file .env, bạn cần điền các thông tin sau:**

#### 6.2.1. Database Configuration

```bash
DB_USERNAME=app_user
DB_PASSWORD=your_strong_password
# Thay your_strong_password bằng password bạn đã tạo ở bước 3.6
```

#### 6.2.2. JWT Secret

```bash
JWT_SECRET=your-very-strong-jwt-secret-key-at-least-32-characters-long-change-this
# Tạo một chuỗi ngẫu nhiên dài ít nhất 32 ký tự
# Ví dụ: MySuperSecretJWTKeyForSmartRetail2024!@#
```

**Cách tạo JWT Secret ngẫu nhiên:**
```bash
# Trên VPS, chạy lệnh này để tạo chuỗi ngẫu nhiên
openssl rand -base64 32
# Copy kết quả và dán vào JWT_SECRET
```

#### 6.2.3. AWS S3 (Cho Product Service)

**Nếu bạn chưa có AWS account:**
- Đăng ký tại: https://aws.amazon.com/
- Tạo S3 bucket
- Tạo IAM user và lấy Access Key

```bash
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_REGION=ap-southeast-2
AWS_S3_BUCKET=your-bucket-name
AWS_S3_FOLDER=product-images
```

**Lưu ý:** Thay bằng thông tin thật của bạn!

#### 6.2.4. Email Configuration (Cho Order Service & User Service)

**Nếu dùng Gmail:**

1. Bật 2-Step Verification: https://myaccount.google.com/security
2. Tạo App Password:
   - Vào: https://myaccount.google.com/apppasswords
   - Chọn "Mail" và "Other"
   - Nhập tên: "Smart Retail"
   - Copy password 16 ký tự

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-16-char-app-password
MAIL_FROM=your-email@gmail.com
```

#### 6.2.5. SePay (Cho Payment Service)

**Nếu bạn chưa có SePay account:**
- Đăng ký tại: https://sepay.vn/
- Lấy API Key và Secret

```bash
SEPAY_API_URL=https://api.sepay.vn
SEPAY_API_KEY=your-sepay-api-key
SEPAY_SECRET=your-sepay-secret
SEPAY_ACCOUNT_NUMBER=your-account-number
SEPAY_ACCOUNT_NAME=your-account-name
SEPAY_BANK_CODE=your-bank-code
SEPAY_WEBHOOK_VERIFY=false
```

**Lưu ý:** Nếu chưa có, có thể để tạm giá trị giả, sau này cập nhật.

#### 6.2.6. AI Service

**Nếu dùng Google Gemini (Free):**
- Đăng ký tại: https://makersuite.google.com/app/apikey
- Tạo API Key

```bash
GOOGLE_API_KEY=your-google-api-key
MODEL_NAME=gemini-2.5-flash
USE_GEMINI=true
MYSQL_URL=mysql+pymysql://reader:reader_strong_password@localhost:3306/product_db
# Thay reader_strong_password bằng password bạn đã tạo ở bước 3.6
```

### 6.3. Lưu File .env

**Trong nano:**
1. Nhấn `Ctrl + O` để lưu
2. Nhấn `Enter` để xác nhận
3. Nhấn `Ctrl + X` để thoát

### 6.4. Tạo File .env Cho AI Service

```bash
# Vào thư mục ai-service
cd /opt/smart-retail/smart-retail-backend/ai-service

# Copy template (nếu có)
cp env.sample .env

# Hoặc tạo mới
nano .env
```

**Điền nội dung:**
```bash
GOOGLE_API_KEY=your-google-api-key
MODEL_NAME=gemini-2.5-flash
USE_GEMINI=true
MYSQL_URL=mysql+pymysql://reader:reader_strong_password@localhost:3306/product_db
```

**Lưu và thoát:** `Ctrl + O`, `Enter`, `Ctrl + X`

---

## 🏗️ Bước 7: Build Ứng Dụng

### 7.1. Vào Thư Mục Project

```bash
cd /opt/smart-retail/smart-retail-backend
```

### 7.2. Build Tất Cả Services

```bash
# Build tất cả (có thể mất 10-15 phút)
./mvnw clean package -DskipTests
```

**Nếu không có file mvnw, cài Maven wrapper:**
```bash
mvn wrapper:wrapper
```

**Hoặc dùng Maven trực tiếp:**
```bash
mvn clean package -DskipTests
```

**Chờ build xong!** Bạn sẽ thấy:
```
[INFO] BUILD SUCCESS
```

### 7.3. Kiểm Tra JAR Files Đã Tạo

```bash
# Kiểm tra từng service
ls -lh discovery-server/target/*.jar
ls -lh api-gateway/target/*.jar
ls -lh service-auth/target/*.jar
ls -lh user-service/target/*.jar
ls -lh service-product/target/*.jar
ls -lh inventory-service/target/*.jar
ls -lh order-service/target/*.jar
ls -lh promotion-service/target/*.jar
ls -lh payment-service/target/*.jar
```

**Nếu thấy file `.jar` = ✅ Build thành công!**

---

## 🐳 Bước 8: Tạo Dockerfiles

### 8.1. Tạo Dockerfile Cho Từng Service

**Discovery Server:**

```bash
cd /opt/smart-retail/smart-retail-backend/discovery-server
nano Dockerfile
```

**Copy nội dung:**
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Lưu:** `Ctrl + O`, `Enter`, `Ctrl + X`

**Lặp lại cho các services khác**, chỉ thay port:

- `api-gateway/Dockerfile` → Port 8085
- `service-auth/Dockerfile` → Port 8081
- `user-service/Dockerfile` → Port 8082
- `service-customer/Dockerfile` → Port 8083
- `service-product/Dockerfile` → Port 8084
- `inventory-service/Dockerfile` → Port 8086
- `promotion-service/Dockerfile` → Port 8087
- `order-service/Dockerfile` → Port 8088
- `payment-service/Dockerfile` → Port 8090

**Hoặc dùng script tự động (nếu có):**
```bash
cd /opt/smart-retail/smart-retail-backend
chmod +x create-dockerfiles.sh
./create-dockerfiles.sh
```

### 8.2. Build Docker Images

```bash
# Về thư mục root
cd /opt/smart-retail/smart-retail-backend

# Build từng service
cd discovery-server && docker build -t discovery-server:latest . && cd ..
cd api-gateway && docker build -t api-gateway:latest . && cd ..
cd service-auth && docker build -t service-auth:latest . && cd ..
cd user-service && docker build -t user-service:latest . && cd ..
cd service-customer && docker build -t service-customer:latest . && cd ..
cd service-product && docker build -t service-product:latest . && cd ..
cd inventory-service && docker build -t inventory-service:latest . && cd ..
cd order-service && docker build -t order-service:latest . && cd ..
cd promotion-service && docker build -t promotion-service:latest . && cd ..
cd payment-service && docker build -t payment-service:latest . && cd ..
```

**Chờ 10-15 phút** để build xong tất cả images.

### 8.3. Kiểm Tra Images Đã Build

```bash
docker images
```

**Bạn sẽ thấy danh sách:**
```
REPOSITORY            TAG       IMAGE ID       CREATED         SIZE
discovery-server      latest    abc123...      2 minutes ago   500MB
api-gateway           latest    def456...      2 minutes ago   450MB
service-auth          latest    ghi789...      2 minutes ago   400MB
...
```

---

## 🚀 Bước 9: Chạy Services Với Docker Compose

### 9.1. Tạo Docker Compose File

```bash
cd /opt/smart-retail/smart-retail-backend

# Copy template
cp docker-compose.example.yml docker-compose.yml

# Mở để xem (thường không cần sửa)
nano docker-compose.yml
```

### 9.2. Chạy Tất Cả Services

```bash
# Start tất cả services
docker-compose up -d
```

**`-d`** = chạy ở background (detached mode)

**Chờ 1-2 phút** để tất cả services khởi động.

### 9.3. Kiểm Tra Services Đang Chạy

```bash
# Xem danh sách containers
docker-compose ps
```

**Bạn sẽ thấy:**
```
NAME                    STATUS              PORTS
discovery-server        Up 30 seconds       0.0.0.0:8761->8761/tcp
api-gateway             Up 20 seconds       0.0.0.0:8085->8085/tcp
service-auth            Up 25 seconds       0.0.0.0:8081->8081/tcp
...
```

**Nếu tất cả đều "Up" = ✅ OK!**

### 9.4. Xem Logs

```bash
# Xem logs tất cả services
docker-compose logs -f

# Xem logs một service cụ thể
docker-compose logs -f discovery-server
docker-compose logs -f api-gateway
```

**Nhấn `Ctrl + C` để thoát khỏi logs.**

---

## ✅ Bước 10: Kiểm Tra Hệ Thống

### 10.1. Kiểm Tra Eureka Dashboard

**Mở browser trên máy tính của bạn:**
```
http://your-vps-ip:8761
```

**Ví dụ:** `http://123.456.789.012:8761`

**Bạn sẽ thấy:**
- Trang Eureka Dashboard
- Danh sách các services đã đăng ký

**Nếu thấy các services trong danh sách = ✅ OK!**

### 10.2. Kiểm Tra Health Endpoints

**Trên VPS, chạy lệnh:**

```bash
# API Gateway
curl http://localhost:8085/actuator/health

# Product Service
curl http://localhost:8084/actuator/health

# Order Service
curl http://localhost:8088/actuator/health
```

**Kết quả mong đợi:**
```json
{"status":"UP"}
```

**Nếu thấy `"UP"` = ✅ Service đang chạy tốt!**

### 10.3. Test API Gateway

```bash
# Test endpoint
curl http://localhost:8085/api/products
```

**Nếu thấy kết quả (có thể là lỗi 401 nếu cần auth) = ✅ API Gateway đang hoạt động!**

---

## 🌐 Bước 11: Cấu Hình Nginx (Tùy Chọn - Nếu Có Domain)

### 11.1. Cài Nginx

```bash
sudo apt install -y nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 11.2. Tạo Config Nginx

```bash
sudo nano /etc/nginx/sites-available/smart-retail
```

**Copy nội dung:**

```nginx
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;

    # API Gateway
    location /api/ {
        proxy_pass http://localhost:8085;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Eureka Dashboard
    location /eureka/ {
        proxy_pass http://localhost:8761/;
        proxy_set_header Host $host;
    }
}
```

**Lưu:** `Ctrl + O`, `Enter`, `Ctrl + X`

### 11.3. Enable Site

```bash
sudo ln -s /etc/nginx/sites-available/smart-retail /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 11.4. Cài SSL (Let's Encrypt)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com -d www.your-domain.com
```

---

## 🔥 Bước 12: Cấu Hình Firewall

### 12.1. Cấu Hình UFW

```bash
# Cho phép SSH
sudo ufw allow 22/tcp

# Cho phép HTTP và HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Cho phép Eureka (nếu cần truy cập từ ngoài)
sudo ufw allow 8761/tcp

# Enable firewall
sudo ufw enable

# Kiểm tra
sudo ufw status
```

---

## 🎉 Hoàn Thành!

### Checklist Cuối Cùng:

- [ ] VPS đã được setup
- [ ] Java 17 đã cài
- [ ] MySQL đã cài và databases đã tạo
- [ ] File `.env` đã được cấu hình
- [ ] Services đã được build
- [ ] Docker images đã được build
- [ ] Services đã chạy với Docker Compose
- [ ] Eureka Dashboard hiển thị các services
- [ ] Health endpoints trả về "UP"
- [ ] API Gateway có thể truy cập được

### Các URL Quan Trọng:

- **Eureka Dashboard**: `http://your-vps-ip:8761`
- **API Gateway**: `http://your-vps-ip:8085`
- **API Endpoint**: `http://your-vps-ip:8085/api/products`

---

## 🔧 Xử Lý Lỗi Thường Gặp

### Lỗi 1: "Cannot connect to MySQL"

**Nguyên nhân:** MySQL chưa chạy hoặc password sai

**Giải pháp:**
```bash
# Kiểm tra MySQL
sudo systemctl status mysql

# Khởi động MySQL
sudo systemctl start mysql

# Test kết nối
mysql -u app_user -p -h localhost product_db
```

### Lỗi 2: "Port already in use"

**Nguyên nhân:** Port đã được sử dụng

**Giải pháp:**
```bash
# Tìm process đang dùng port
sudo lsof -i :8084

# Kill process
sudo kill -9 <PID>
```

### Lỗi 3: "Service không đăng ký với Eureka"

**Nguyên nhân:** Eureka chưa khởi động xong

**Giải pháp:**
```bash
# Đợi Eureka khởi động (30 giây)
# Kiểm tra Eureka
curl http://localhost:8761

# Xem logs
docker-compose logs discovery-server
```

### Lỗi 4: "Environment variables không được đọc"

**Nguyên nhân:** File .env không đúng format hoặc không được load

**Giải pháp:**
```bash
# Kiểm tra file .env
cat /opt/smart-retail/smart-retail-backend/.env

# Kiểm tra trong container
docker exec <container-name> env | grep DB_PASSWORD
```

### Lỗi 5: "Out of memory"

**Nguyên nhân:** VPS không đủ RAM

**Giải pháp:**
- Nâng cấp VPS lên 8GB RAM
- Hoặc giảm số services chạy cùng lúc

---

## 📞 Cần Giúp Đỡ?

Nếu gặp vấn đề:

1. **Xem logs:**
   ```bash
   docker-compose logs -f
   ```

2. **Kiểm tra từng service:**
   ```bash
   docker-compose ps
   docker-compose logs service-name
   ```

3. **Restart services:**
   ```bash
   docker-compose restart
   ```

4. **Xem tài liệu khác:**
   - `HUONG_DAN_DEPLOY_VPS.md` - Hướng dẫn chi tiết VPS
   - `DANH_SACH_SERVICES.md` - Danh sách services
   - `THU_TU_DEPLOY.md` - Thứ tự deploy

---

**Chúc bạn deploy thành công!** 🚀

Nếu có thắc mắc, cứ hỏi mình nhé!

