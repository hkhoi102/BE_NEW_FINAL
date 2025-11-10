# ✅ Kiểm Tra Services Có Đọc Được Keys Chưa

## 📋 Tình Trạng Hiện Tại

### ✅ Các Services Đã Được Cấu Hình Đúng

Tất cả services đã dùng format `${ENV_VAR:default}` để đọc environment variables:

| Service | Keys Đã Cấu Hình | Format |
|---------|------------------|--------|
| **service-product** | AWS keys, DB credentials | `${AWS_ACCESS_KEY_ID:}`, `${DB_PASSWORD:sapassword}` |
| **order-service** | Email, JWT, DB | `${MAIL_PASSWORD:}`, `${JWT_SECRET:...}`, `${DB_PASSWORD:sapassword}` |
| **user-service** | Email, JWT, DB | `${MAIL_PASSWORD:}`, `${JWT_SECRET:...}`, `${DB_PASSWORD:sapassword}` |
| **payment-service** | SePay keys | `${SEPAY_API_KEY:}`, `${SEPAY_SECRET:}` |
| **Tất cả services** | DB credentials | `${DB_USERNAME:root}`, `${DB_PASSWORD:sapassword}` |

## ⚠️ QUAN TRỌNG: Spring Boot Không Tự Đọc File .env

**Spring Boot KHÔNG tự động đọc file `.env`!**

Spring Boot chỉ đọc từ:
1. **System Environment Variables** (biến môi trường hệ thống)
2. **Command line arguments** (`-Dproperty=value`)
3. **application.properties/yml** (với `${ENV_VAR:default}`)

### Cách Services Đọc Keys:

```properties
# Format trong application.properties
spring.datasource.password=${DB_PASSWORD:sapassword}
```

**Cách hoạt động:**
- Đọc từ **System Environment Variable** `DB_PASSWORD`
- Nếu không có → dùng default value `sapassword`

---

## 🔍 Cách Kiểm Tra Services Có Đọc Được Keys

### Cách 1: Kiểm Tra Logs Khi Start Service

Khi start service, xem logs:

**Nếu đọc được keys:**
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
# Không có lỗi connection
```

**Nếu KHÔNG đọc được keys:**
```
Access denied for user 'root'@'localhost' (using password: YES)
# Hoặc
Communications link failure
```

### Cách 2: Test Với Environment Variables

**Windows PowerShell:**
```powershell
# Set environment variables
$env:DB_PASSWORD="your_password"
$env:AWS_ACCESS_KEY_ID="your_key"

# Start service
cd service-product
java -jar target/service-product-*.jar
```

**Windows CMD:**
```cmd
set DB_PASSWORD=your_password
set AWS_ACCESS_KEY_ID=your_key
cd service-product
java -jar target\service-product-*.jar
```

**Linux/Mac:**
```bash
export DB_PASSWORD=your_password
export AWS_ACCESS_KEY_ID=your_key
cd service-product
java -jar target/service-product-*.jar
```

### Cách 3: Kiểm Tra Trong Docker

```bash
# Kiểm tra environment variables trong container
docker exec <container-name> env | grep DB_PASSWORD
docker exec <container-name> env | grep AWS_ACCESS_KEY_ID

# Xem logs
docker logs <container-name> | grep -i "password\|key\|connection"
```

### Cách 4: Tạo Test Endpoint (Tạm thời, chỉ dùng dev)

Thêm vào một service để test (ví dụ `service-product`):

```java
@RestController
@RequestMapping("/test")
public class ConfigTestController {

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${aws.s3.access-key:NOT_SET}")
    private String awsAccessKey;

    @GetMapping("/config")
    public Map<String, String> checkConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("db_username", dbUsername);
        config.put("db_password_set", dbPassword != null && !dbPassword.isEmpty() ? "YES" : "NO");
        config.put("aws_access_key_set",
            awsAccessKey.equals("NOT_SET") || awsAccessKey.isEmpty() ? "NO" : "YES");
        config.put("env_db_password", System.getenv("DB_PASSWORD") != null ? "SET" : "NOT_SET");
        config.put("env_aws_key", System.getenv("AWS_ACCESS_KEY_ID") != null ? "SET" : "NOT_SET");
        return config;
    }
}
```

Sau đó test:
```bash
curl http://localhost:8084/test/config
```

---

## 🐳 Với Docker Compose - Tự Động Đọc .env

**Docker Compose TỰ ĐỘNG đọc file `.env`!**

Khi dùng `docker-compose.yml` với `env_file: - .env`, Docker sẽ tự động:
1. Đọc file `.env`
2. Set các biến thành environment variables trong container
3. Spring Boot sẽ đọc được từ environment variables

**Ví dụ:**
```yaml
services:
  service-product:
    env_file:
      - .env  # Docker tự động đọc và set env vars
```

---

## 📝 Cách Đảm Bảo Services Đọc Được Keys

### Phương Pháp A: Docker Compose (Khuyến nghị)

1. Tạo file `.env` ở root project:
```bash
DB_USERNAME=root
DB_PASSWORD=your_password
AWS_ACCESS_KEY_ID=your_key
AWS_SECRET_ACCESS_KEY=your_secret
JWT_SECRET=your_jwt_secret
```

2. Docker Compose tự động đọc:
```yaml
services:
  service-product:
    env_file:
      - .env
```

3. Services sẽ tự động đọc được!

### Phương Pháp B: JAR Files - Set Environment Variables

**Trước khi chạy JAR, set environment variables:**

**Windows PowerShell:**
```powershell
# Tạo file set-env.ps1
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:AWS_ACCESS_KEY_ID="your_key"
$env:AWS_SECRET_ACCESS_KEY="your_secret"
$env:JWT_SECRET="your_jwt_secret"

# Chạy service
java -jar service-product/target/service-product-*.jar
```

**Windows CMD:**
```cmd
REM Tạo file set-env.bat
set DB_USERNAME=root
set DB_PASSWORD=your_password
set AWS_ACCESS_KEY_ID=your_key
set AWS_SECRET_ACCESS_KEY=your_secret
set JWT_SECRET=your_jwt_secret

REM Chạy service
java -jar service-product\target\service-product-*.jar
```

**Linux/Mac:**
```bash
# Tạo file set-env.sh
export DB_USERNAME=root
export DB_PASSWORD=your_password
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret
export JWT_SECRET=your_jwt_secret

# Chạy service
source set-env.sh
java -jar service-product/target/service-product-*.jar
```

### Phương Pháp C: Dùng dotenv-java (Nâng cao)

Nếu muốn Spring Boot tự đọc file `.env`, thêm dependency:

```xml
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>dotenv-java</artifactId>
    <version>3.0.0</version>
</dependency>
```

Và thêm vào `Application.java`:
```java
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class ServiceProductApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();
        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );
        SpringApplication.run(ServiceProductApplication.class, args);
    }
}
```

---

## ✅ Checklist Kiểm Tra

- [ ] File `.env` đã được tạo và điền keys
- [ ] Với Docker: `docker-compose.yml` có `env_file: - .env`
- [ ] Với JAR: Environment variables đã được set trước khi chạy
- [ ] Service start không có lỗi connection
- [ ] Logs không có lỗi "Access denied" hoặc "password"
- [ ] Database connection thành công (thấy "HikariPool-1 - Start completed")

---

## 🔍 Test Nhanh

### Test 1: Kiểm Tra Environment Variables

**Windows PowerShell:**
```powershell
# Kiểm tra env vars có được set chưa
$env:DB_PASSWORD
$env:AWS_ACCESS_KEY_ID
```

**Linux/Mac:**
```bash
echo $DB_PASSWORD
echo $AWS_ACCESS_KEY_ID
```

### Test 2: Test Database Connection

```bash
# Test với password từ env var
mysql -u ${DB_USERNAME:-root} -p${DB_PASSWORD:-sapassword} -h localhost product_db
```

### Test 3: Start Service và Xem Logs

```bash
# Start service
java -jar service-product/target/service-product-*.jar

# Xem logs - tìm dòng:
# "HikariPool-1 - Start completed" = ✅ Kết nối thành công
# "Access denied" = ❌ Password sai
```

---

## 📚 Tóm Tắt

### ✅ Services Đã Được Cấu Hình Đúng

Tất cả services đã dùng format `${ENV_VAR:default}` - **ĐÚNG!**

### ⚠️ Lưu Ý Quan Trọng

1. **Spring Boot KHÔNG tự đọc file `.env`**
2. **Docker Compose TỰ ĐỘNG đọc file `.env`** ✅
3. **Với JAR files, cần set environment variables thủ công** ⚠️

### 🎯 Giải Pháp

- **Docker Compose**: Chỉ cần tạo file `.env` → Tự động đọc ✅
- **JAR Files**: Set environment variables trước khi chạy ⚠️

---

**Services đã sẵn sàng đọc keys! Chỉ cần đảm bảo environment variables được set đúng cách.** ✅

