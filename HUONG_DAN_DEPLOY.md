# 🚀 Hướng Dẫn Deploy - Làm Sao Ứng Dụng Biết Các Keys?

## ❓ Câu Hỏi

Khi deploy, làm sao ứng dụng biết được các keys (AWS, database password, JWT secret, etc.)?

## ✅ Câu Trả Lời Ngắn Gọn

**Ứng dụng đọc keys từ Environment Variables** của hệ điều hành/server. Bạn cần **set các environment variables** trước khi chạy ứng dụng.

## 📋 Các Cách Set Environment Variables Khi Deploy

### 1. 🐳 Docker (Phổ Biến Nhất)

#### Cách 1: Dùng file `.env`

**Bước 1**: Tạo file `.env` trên server (KHÔNG commit vào git):
```bash
# .env
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
DB_PASSWORD=your-db-password
JWT_SECRET=your-jwt-secret
```

**Bước 2**: Chạy Docker:
```bash
docker run -d -p 8084:8084 --env-file .env service-product:latest
```

#### Cách 2: Dùng `-e` flag
```bash
docker run -d -p 8084:8084 \
  -e AWS_ACCESS_KEY_ID="your-access-key" \
  -e AWS_SECRET_ACCESS_KEY="your-secret-key" \
  -e DB_PASSWORD="your-db-password" \
  service-product:latest
```

#### Cách 3: Docker Compose (Khuyến nghị)

Tạo `docker-compose.yml`:
```yaml
version: '3.8'
services:
  service-product:
    image: service-product:latest
    ports:
      - "8084:8084"
    env_file:
      - .env  # Đọc từ file .env
    restart: unless-stopped
```

Chạy:
```bash
docker-compose up -d
```

### 2. 🖥️ Server Thông Thường (Linux/Windows)

#### Linux (systemd)

Tạo file `/etc/systemd/system/service-product.service`:
```ini
[Unit]
Description=Service Product
After=network.target

[Service]
Type=simple
User=your-user
Environment="AWS_ACCESS_KEY_ID=your-access-key"
Environment="AWS_SECRET_ACCESS_KEY=your-secret-key"
Environment="DB_PASSWORD=your-db-password"
Environment="JWT_SECRET=your-jwt-secret"
ExecStart=/usr/bin/java -jar /path/to/service-product.jar
Restart=always

[Install]
WantedBy=multi-user.target
```

Enable và start:
```bash
sudo systemctl enable service-product
sudo systemctl start service-product
```

#### Windows (Service hoặc Task Scheduler)

Tạo file `.bat` hoặc `.ps1`:
```batch
@echo off
set AWS_ACCESS_KEY_ID=your-access-key
set AWS_SECRET_ACCESS_KEY=your-secret-key
set DB_PASSWORD=your-db-password
set JWT_SECRET=your-jwt-secret
java -jar service-product.jar
```

### 3. ☁️ Cloud Platforms

#### AWS EC2 / Elastic Beanstalk

**EC2**: Set trong systemd service (xem trên)

**Elastic Beanstalk**:
```bash
eb setenv AWS_ACCESS_KEY_ID=your-key AWS_SECRET_ACCESS_KEY=your-secret
```

Hoặc dùng **AWS Secrets Manager** (Khuyến nghị):
1. Lưu secrets vào Secrets Manager
2. EC2 instance có IAM role để đọc secrets
3. Ứng dụng tự động đọc từ Secrets Manager

#### Azure App Service

Vào Portal → App Service → Configuration → Application settings:
- Thêm: `AWS_ACCESS_KEY_ID` = `your-key`
- Thêm: `AWS_SECRET_ACCESS_KEY` = `your-secret`

Hoặc dùng **Azure Key Vault** (Khuyến nghị)

#### Google Cloud (Cloud Run / App Engine)

```bash
gcloud run deploy service-product \
  --set-env-vars="AWS_ACCESS_KEY_ID=your-key,AWS_SECRET_ACCESS_KEY=your-secret"
```

Hoặc dùng **Secret Manager** (Khuyến nghị)

### 4. ☸️ Kubernetes

Tạo Secret:
```bash
kubectl create secret generic app-secrets \
  --from-literal=aws-access-key='your-key' \
  --from-literal=aws-secret-key='your-secret'
```

Trong Deployment YAML:
```yaml
env:
- name: AWS_ACCESS_KEY_ID
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: aws-access-key
```

## 🔍 Làm Sao Kiểm Tra Keys Đã Được Set?

### Kiểm tra trong Docker:
```bash
docker exec <container-id> env | grep AWS
docker exec <container-id> env | grep DB_PASSWORD
```

### Kiểm tra trong Linux:
```bash
systemctl show service-product | grep Environment
```

### Kiểm tra trong ứng dụng (tạm thời, chỉ dùng dev):
Thêm endpoint:
```java
@GetMapping("/check-config")
public Map<String, String> check() {
    Map<String, String> config = new HashMap<>();
    config.put("aws-key-set",
        System.getenv("AWS_ACCESS_KEY_ID") != null ? "YES" : "NO");
    return config;
}
```

## 📝 Danh Sách Environment Variables Cần Set

### Cho Tất Cả Services:
```bash
DB_USERNAME=root
DB_PASSWORD=your-db-password
JWT_SECRET=your-jwt-secret-min-32-chars
```

### Cho Service Product:
```bash
AWS_ACCESS_KEY_ID=your-aws-key
AWS_SECRET_ACCESS_KEY=your-aws-secret
AWS_REGION=ap-southeast-2
AWS_S3_BUCKET=your-bucket
```

### Cho Order Service & User Service:
```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-email@gmail.com
```

### Cho Payment Service:
```bash
SEPAY_API_KEY=your-sepay-key
SEPAY_SECRET=your-sepay-secret
SEPAY_ACCOUNT_NUMBER=your-account
```

### Cho AI Service:
```bash
GOOGLE_API_KEY=your-google-key
# hoặc
OPENAI_API_KEY=your-openai-key
MYSQL_URL=mysql+pymysql://user:pass@host:3306/db
```

## ✅ Checklist Khi Deploy

- [ ] Đã tạo file `.env` trên server (hoặc set environment variables)
- [ ] Đã test ứng dụng có đọc được environment variables chưa
- [ ] Đã đảm bảo file `.env` không bị commit lên git
- [ ] Đã set đúng tất cả keys cần thiết cho từng service
- [ ] Đã test ứng dụng chạy được với environment variables

## 💡 Best Practices

1. **Dùng Secrets Management** cho production (AWS Secrets Manager, Azure Key Vault, etc.)
2. **Không hardcode** keys trong code (✅ đã làm)
3. **Rotate keys** định kỳ
4. **Phân quyền** - chỉ những service/user cần thiết mới có quyền đọc keys

## 🚀 Ví Dụ Nhanh: Docker Compose

Tôi đã tạo sẵn file `docker-compose.example.yml` và `.env.example`:

**Bước 1**: Copy file `env.example` thành `.env`:
```bash
# Windows
copy env.example .env

# Linux/Mac
cp env.example .env
```

**Bước 2**: Sửa file `.env` và điền keys thật của bạn

**Bước 3**: Copy `docker-compose.example.yml` thành `docker-compose.yml`:
```bash
cp docker-compose.example.yml docker-compose.yml
```

**Bước 4**: Chạy:
```bash
docker-compose up -d
```

Tất cả services sẽ tự động đọc keys từ file `.env`! 🎉

## 📚 Xem Thêm

- `DEPLOYMENT_GUIDE.md` - Hướng dẫn chi tiết cho từng platform
- `DA_SUA_XONG.md` - Danh sách đầy đủ environment variables
- `docker-compose.example.yml` - File Docker Compose mẫu
- `env.example` - Template cho file .env

---

**Tóm lại**: Khi deploy, bạn set environment variables trên server/container, và ứng dụng Spring Boot sẽ tự động đọc chúng từ `System.getenv()` hoặc `${ENV_VAR}` trong `application.properties`.

