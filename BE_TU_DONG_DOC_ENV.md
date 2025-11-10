# ✅ BE Tự Động Đọc File .env

## 🎯 Vấn Đề

Bạn lo lắng: **"Để key vào .env thì BE có lấy được key đâu?"**

## ✅ Giải Pháp Đã Áp Dụng

**Tất cả services đã được cấu hình để TỰ ĐỘNG đọc file `.env`!**

### Cách Hoạt Động

1. **Thư viện dotenv-java**: Đã thêm vào tất cả services
2. **Tự động load**: Mỗi service sẽ tự động đọc file `.env` khi khởi động
3. **Set environment variables**: Các keys trong `.env` sẽ được set thành environment variables
4. **Spring Boot đọc được**: Spring Boot sẽ đọc từ environment variables như bình thường

---

## 📋 Danh Sách Services Đã Được Cấu Hình

| Service | Đã Thêm dotenv-java | Đã Sửa Application.java |
|---------|---------------------|--------------------------|
| ✅ service-product | ✅ | ✅ |
| ✅ order-service | ✅ | ✅ |
| ✅ user-service | ✅ | ✅ |
| ✅ service-auth | ✅ | ✅ |
| ✅ service-customer | ✅ | ✅ |
| ✅ inventory-service | ✅ | ✅ |
| ✅ promotion-service | ✅ | ✅ |
| ✅ payment-service | ✅ | ✅ |
| ✅ api-gateway | ✅ | ✅ |
| ✅ discovery-server | ✅ | ✅ |

---

## 🔧 Cách Sử Dụng

### Bước 1: Tạo File `.env` Ở Root Project

Tạo file `.env` ở thư mục gốc của project (cùng cấp với `pom.xml`):

```bash
# Database
DB_USERNAME=root
DB_PASSWORD=your_password

# AWS S3
AWS_ACCESS_KEY_ID=your_aws_key
AWS_SECRET_ACCESS_KEY=your_aws_secret
AWS_REGION=ap-southeast-2
AWS_S3_BUCKET=your_bucket_name
AWS_S3_FOLDER=product-images

# JWT
JWT_SECRET=your_jwt_secret_key_here

# Email
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_email_password
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_FROM=your_email@gmail.com

# SePay
SEPAY_API_KEY=your_sepay_key
SEPAY_SECRET=your_sepay_secret
SEPAY_ACCOUNT_NUMBER=your_account_number
SEPAY_ACCOUNT_NAME=your_account_name
SEPAY_BANK_CODE=your_bank_code

# OpenAI (cho chatbox-service nếu có)
OPENAI_API_KEY=sk-xxxxxxxx

# Google AI (cho ai-service)
GOOGLE_API_KEY=your_google_api_key
```

### Bước 2: Chạy Services

**Cách 1: Chạy JAR Files**

```bash
# Build services
mvn clean package -DskipTests

# Chạy service (tự động đọc .env)
java -jar service-product/target/service-product-*.jar
```

**Cách 2: Chạy Với Maven**

```bash
# Chạy service (tự động đọc .env)
cd service-product
mvn spring-boot:run
```

**Cách 3: Docker Compose**

Docker Compose cũng tự động đọc `.env`:

```yaml
services:
  service-product:
    env_file:
      - .env  # Docker tự động đọc
```

---

## 🔍 Cách Kiểm Tra

### Test 1: Kiểm Tra Service Có Đọc Được .env

**Thêm log vào Application.java để test:**

```java
public static void main(String[] args) {
    Dotenv dotenv = Dotenv.configure()
        .directory("./")
        .ignoreIfMissing()
        .load();

    // Test: In ra một key để kiểm tra
    System.out.println("DB_PASSWORD loaded: " +
        (System.getProperty("DB_PASSWORD") != null ? "✅ YES" : "❌ NO"));

    dotenv.entries().forEach(entry ->
        System.setProperty(entry.getKey(), entry.getValue())
    );

    SpringApplication.run(ServiceProductApplication.class, args);
}
```

### Test 2: Kiểm Tra Database Connection

Khi start service, xem logs:

**✅ Nếu đọc được:**
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

**❌ Nếu không đọc được:**
```
Access denied for user 'root'@'localhost' (using password: YES)
```

---

## 📝 Lưu Ý Quan Trọng

### 1. Vị Trí File `.env`

File `.env` phải ở **root directory** của project (cùng cấp với `pom.xml`):

```
smart-retail-backend/
├── .env                    ← File .env ở đây
├── pom.xml
├── service-product/
├── order-service/
└── ...
```

### 2. Format File `.env`

```properties
# Đúng ✅
DB_PASSWORD=my_password
AWS_ACCESS_KEY_ID=AKIA...

# Sai ❌ (không có dấu cách)
DB_PASSWORD = my_password

# Sai ❌ (không có quotes)
DB_PASSWORD="my_password"
```

### 3. Nếu Không Có File `.env`

- Service vẫn chạy được (không bị lỗi)
- Sẽ dùng **default values** từ `application.properties`
- Ví dụ: `${DB_PASSWORD:sapassword}` → dùng `sapassword` nếu không có `.env`

### 4. Ưu Tiên Đọc

Spring Boot đọc theo thứ tự ưu tiên:

1. **System Environment Variables** (cao nhất)
2. **File `.env`** (qua dotenv-java)
3. **Default values** trong `application.properties` (thấp nhất)

---

## 🎯 Tóm Tắt

### ✅ Đã Làm

1. ✅ Thêm `dotenv-java` vào tất cả services
2. ✅ Sửa `Application.java` để tự động load `.env`
3. ✅ Service tự động đọc file `.env` khi khởi động

### 📝 Cách Sử Dụng

1. Tạo file `.env` ở root project
2. Điền các keys vào file `.env`
3. Chạy services → Tự động đọc keys từ `.env`

### 🔒 Bảo Mật

- File `.env` đã được thêm vào `.gitignore`
- Không commit file `.env` lên GitHub
- Chỉ commit `env.example` (template)

---

## ✅ Kết Luận

**Bây giờ BE đã tự động đọc được keys từ file `.env`!**

Bạn chỉ cần:
1. Tạo file `.env` ở root project
2. Điền keys vào
3. Chạy services → Tự động đọc được! ✅

