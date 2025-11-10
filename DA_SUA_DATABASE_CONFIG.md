# ✅ Đã Sửa Database Configuration - Username và Password

## 🎯 Vấn Đề

Các services không kết nối được database vì:
- Username không nhất quán (một số hardcode `root`, một số dùng env variable)
- Password có default value nhưng không đúng với database thật
- Format không nhất quán giữa các services

## ✅ Đã Sửa

Tất cả các services đã được sửa để:
- **Username**: `${DB_USERNAME:root}` - Có default là `root`, có thể override bằng env variable
- **Password**: `${DB_PASSWORD:sapassword}` - Có default là `sapassword`, có thể override bằng env variable

## 📝 Danh Sách Services Đã Sửa

| Service | Username | Password | Trạng Thái |
|---------|----------|----------|------------|
| **service-auth** | `${DB_USERNAME:root}` | `${DB_PASSWORD:sapassword}` | ✅ Đã sửa |
| **user-service** | `${DB_USERNAME:root}` | `${DB_PASSWORD:sapassword}` | ✅ Đã sửa |
| **service-customer** | `${DB_USERNAME:root}` | `${DB_PASSWORD:sapassword}` | ✅ Đã sửa |
| **service-product** | `${DB_USERNAME:root}` | `${DB_PASSWORD:sapassword}` | ✅ Đã sửa |
| **inventory-service** | `${DB_USERNAME:root}` | `${DB_PASSWORD:sapassword}` | ✅ Đã sửa |
| **order-service** | `${DB_USERNAME:root}` | `${DB_PASSWORD:sapassword}` | ✅ Đã sửa |
| **promotion-service** | `${DB_USERNAME:root}` | `${DB_PASSWORD:sapassword}` | ✅ Đã sửa |
| **payment-service** | Không dùng DB | Không dùng DB | ✅ OK |
| **api-gateway** | Không dùng DB | Không dùng DB | ✅ OK |
| **discovery-server** | Không dùng DB | Không dùng DB | ✅ OK |

## 🔧 Cách Sử Dụng

### Option 1: Dùng Default Values (Nếu MySQL password là `sapassword`)

Nếu MySQL của bạn có:
- Username: `root`
- Password: `sapassword`

Thì không cần làm gì, services sẽ tự động dùng default values.

### Option 2: Set Environment Variables (Khuyến nghị)

Tạo file `.env` hoặc set environment variables:

**Windows PowerShell:**
```powershell
$env:DB_USERNAME="app_user"
$env:DB_PASSWORD="your_database_password"
```

**Windows CMD:**
```cmd
set DB_USERNAME=app_user
set DB_PASSWORD=your_database_password
```

**Linux/Mac:**
```bash
export DB_USERNAME=app_user
export DB_PASSWORD=your_database_password
```

**Hoặc trong file `.env`:**
```bash
DB_USERNAME=app_user
DB_PASSWORD=your_database_password
```

## 📋 Format Database Connection

Tất cả services giờ đều dùng format nhất quán:

```properties
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:sapassword}
```

**Giải thích:**
- `${DB_USERNAME:root}` = Đọc từ env variable `DB_USERNAME`, nếu không có thì dùng `root`
- `${DB_PASSWORD:sapassword}` = Đọc từ env variable `DB_PASSWORD`, nếu không có thì dùng `sapassword`

## ✅ Kiểm Tra

### 1. Kiểm Tra File .env

Đảm bảo file `.env` có:
```bash
DB_USERNAME=root
# hoặc
DB_USERNAME=app_user

DB_PASSWORD=sapassword
# hoặc
DB_PASSWORD=your_database_password
```

### 2. Test Kết Nối Database

```bash
# Test với username/password từ .env
mysql -u ${DB_USERNAME:-root} -p${DB_PASSWORD:-sapassword} -h localhost product_db

# Hoặc test trực tiếp
mysql -u root -psapassword -h localhost product_db
```

### 3. Kiểm Tra Service Kết Nối

Khi start service, kiểm tra logs:
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

Nếu thấy lỗi connection, kiểm tra:
- MySQL đã chạy chưa?
- Username/password đúng chưa?
- Database đã được tạo chưa?
- User có quyền truy cập database chưa?

## 🔍 Troubleshooting

### Lỗi: Access denied for user 'root'@'localhost'

**Nguyên nhân**: Password không đúng

**Giải pháp**:
1. Kiểm tra password MySQL thật là gì
2. Set environment variable `DB_PASSWORD` với password đúng
3. Hoặc sửa default value trong `application.properties` (không khuyến nghị)

### Lỗi: Unknown database 'product_db'

**Nguyên nhân**: Database chưa được tạo

**Giải pháp**:
```sql
CREATE DATABASE product_db;
CREATE DATABASE order_db;
-- ... tạo tất cả databases
```

### Lỗi: User không có quyền

**Nguyên nhân**: User chưa được cấp quyền

**Giải pháp**:
```sql
GRANT ALL PRIVILEGES ON product_db.* TO 'app_user'@'%';
FLUSH PRIVILEGES;
```

## 📚 Tài Liệu Tham Khảo

- `HUONG_DAN_DEPLOY_STEP_BY_STEP.md` - Hướng dẫn deploy chi tiết
- `env.example` - Template cho file .env

---

**Tất cả services đã được sửa! Bây giờ bạn có thể chạy services và chúng sẽ kết nối được database.** ✅

