# 🚀 Hướng Dẫn Deploy và Quản Lý Keys

## 📋 Tổng Quan

Khi deploy ứng dụng, bạn cần set các environment variables để ứng dụng có thể đọc được các keys. Dưới đây là các cách phổ biến:

## 1. 🐳 Docker Deployment

### Cách 1: Sử dụng `-e` flag (cho test/development)

```bash
docker run -d \
  -p 8084:8084 \
  -e AWS_ACCESS_KEY_ID="your-access-key" \
  -e AWS_SECRET_ACCESS_KEY="your-secret-key" \
  -e AWS_REGION="ap-southeast-2" \
  -e AWS_S3_BUCKET="your-bucket" \
  -e DB_PASSWORD="your-db-password" \
  -e JWT_SECRET="your-jwt-secret" \
  service-product:latest
```

### Cách 2: Sử dụng file `.env` (Khuyến nghị)

**Bước 1**: Tạo file `.env` trên server (KHÔNG commit vào git):

```bash
# .env (trên server)
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=ap-southeast-2
AWS_S3_BUCKET=your-bucket
DB_PASSWORD=your-db-password
JWT_SECRET=your-jwt-secret
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
SEPAY_API_KEY=your-sepay-key
```

**Bước 2**: Chạy Docker với `--env-file`:

```bash
docker run -d \
  -p 8084:8084 \
  --env-file .env \
  service-product:latest
```

### Cách 3: Docker Compose (Khuyến nghị cho production)

Tạo file `docker-compose.yml`:

```yaml
version: '3.8'

services:
  service-product:
    image: service-product:latest
    ports:
      - "8084:8084"
    env_file:
      - .env
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    restart: unless-stopped

  order-service:
    image: order-service:latest
    ports:
      - "8088:8088"
    env_file:
      - .env
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    restart: unless-stopped

  # ... các services khác
```

Chạy:
```bash
docker-compose up -d
```

## 2. ☁️ Cloud Platform Deployment

### AWS (EC2, ECS, Elastic Beanstalk)

#### EC2 / Traditional Server

**Cách 1: System Environment Variables**

Tạo file `/etc/environment` (Linux):
```bash
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
DB_PASSWORD=your-db-password
JWT_SECRET=your-jwt-secret
```

Hoặc tạo file `/etc/systemd/system/your-service.service`:
```ini
[Unit]
Description=Smart Retail Service Product
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

**Cách 2: AWS Systems Manager Parameter Store (Khuyến nghị)**

1. Lưu secrets vào Parameter Store:
```bash
aws ssm put-parameter \
  --name "/smart-retail/aws-access-key" \
  --value "your-access-key" \
  --type "SecureString"

aws ssm put-parameter \
  --name "/smart-retail/aws-secret-key" \
  --value "your-secret-key" \
  --type "SecureString"
```

2. EC2 instance cần có IAM role với quyền đọc Parameter Store

3. Ứng dụng đọc từ Parameter Store (cần thêm dependency):
```xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-ssm</artifactId>
</dependency>
```

**Cách 3: AWS Secrets Manager (Best Practice cho Production)**

1. Lưu secrets:
```bash
aws secretsmanager create-secret \
  --name smart-retail/secrets \
  --secret-string '{
    "AWS_ACCESS_KEY_ID": "your-access-key",
    "AWS_SECRET_ACCESS_KEY": "your-secret-key",
    "DB_PASSWORD": "your-db-password",
    "JWT_SECRET": "your-jwt-secret"
  }'
```

2. EC2/ECS cần IAM role để đọc secrets

3. Ứng dụng đọc từ Secrets Manager

#### ECS (Elastic Container Service)

**Task Definition với Environment Variables:**

```json
{
  "containerDefinitions": [
    {
      "name": "service-product",
      "image": "your-ecr-repo/service-product:latest",
      "environment": [
        {
          "name": "AWS_REGION",
          "value": "ap-southeast-2"
        }
      ],
      "secrets": [
        {
          "name": "AWS_ACCESS_KEY_ID",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:smart-retail/secrets:AWS_ACCESS_KEY_ID::"
        },
        {
          "name": "AWS_SECRET_ACCESS_KEY",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:smart-retail/secrets:AWS_SECRET_ACCESS_KEY::"
        }
      ]
    }
  ]
}
```

#### Elastic Beanstalk

Tạo file `.ebextensions/environment.config`:
```yaml
option_settings:
  aws:elasticbeanstalk:application:environment:
    AWS_ACCESS_KEY_ID: your-access-key
    AWS_SECRET_ACCESS_KEY: your-secret-key
    DB_PASSWORD: your-db-password
    JWT_SECRET: your-jwt-secret
```

Hoặc set qua EB CLI:
```bash
eb setenv AWS_ACCESS_KEY_ID=your-access-key AWS_SECRET_ACCESS_KEY=your-secret-key
```

### Azure App Service

**Cách 1: Application Settings (Portal)**

1. Vào Azure Portal → App Service → Configuration
2. Thêm các Application Settings:
   - `AWS_ACCESS_KEY_ID` = your-access-key
   - `AWS_SECRET_ACCESS_KEY` = your-secret-key
   - `DB_PASSWORD` = your-db-password

**Cách 2: Azure Key Vault (Khuyến nghị)**

1. Lưu secrets vào Key Vault
2. Link Key Vault với App Service
3. Reference trong Application Settings:
   - `AWS_ACCESS_KEY_ID` = `@Microsoft.KeyVault(SecretUri=https://your-vault.vault.azure.net/secrets/aws-access-key/)`

### Google Cloud Platform (GCP)

**Cloud Run / App Engine:**

```bash
gcloud run deploy service-product \
  --set-env-vars="AWS_ACCESS_KEY_ID=your-access-key,AWS_SECRET_ACCESS_KEY=your-secret-key" \
  --set-secrets="DB_PASSWORD=db-password:latest,JWT_SECRET=jwt-secret:latest"
```

**Hoặc dùng Secret Manager:**

1. Tạo secrets:
```bash
echo -n "your-access-key" | gcloud secrets create aws-access-key --data-file=-
echo -n "your-secret-key" | gcloud secrets create aws-secret-key --data-file=-
```

2. Grant access:
```bash
gcloud secrets add-iam-policy-binding aws-access-key \
  --member="serviceAccount:your-service-account@project.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

3. Mount vào Cloud Run:
```bash
gcloud run deploy service-product \
  --update-secrets="/secrets/aws-access-key=aws-access-key:latest"
```

## 3. ☸️ Kubernetes Deployment

### Cách 1: ConfigMap và Secret

**Tạo Secret:**
```bash
kubectl create secret generic app-secrets \
  --from-literal=aws-access-key-id='your-access-key' \
  --from-literal=aws-secret-access-key='your-secret-key' \
  --from-literal=db-password='your-db-password' \
  --from-literal=jwt-secret='your-jwt-secret'
```

**Deployment YAML:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: service-product
spec:
  replicas: 2
  template:
    spec:
      containers:
      - name: service-product
        image: service-product:latest
        env:
        - name: AWS_ACCESS_KEY_ID
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: aws-access-key-id
        - name: AWS_SECRET_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: aws-secret-access-key
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: db-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: jwt-secret
```

### Cách 2: External Secrets Operator (Khuyến nghị)

Tích hợp với AWS Secrets Manager, Azure Key Vault, hoặc HashiCorp Vault.

## 4. 🔄 CI/CD Pipelines

### GitHub Actions

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Deploy to server
        env:
          AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
          JWT_SECRET: ${{ secrets.JWT_SECRET }}
        run: |
          # Your deployment script
```

**Cách set secrets trong GitHub:**
1. Vào Repository → Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Thêm từng secret:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `DB_PASSWORD`
   - `JWT_SECRET`
   - etc.

### GitLab CI/CD

```yaml
deploy:
  script:
    - docker run -d
      -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
      -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
      -e DB_PASSWORD=$DB_PASSWORD
      service-product:latest
```

**Cách set variables trong GitLab:**
1. Vào Project → Settings → CI/CD → Variables
2. Add variable với "Masked" và "Protected" flags

### Jenkins

**Cách 1: Credentials Plugin**

1. Manage Jenkins → Credentials → Add
2. Chọn loại "Secret text" hoặc "Username with password"
3. Sử dụng trong pipeline:
```groovy
pipeline {
    agent any
    environment {
        AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-key')
    }
    stages {
        stage('Deploy') {
            steps {
                sh 'docker run -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID ...'
            }
        }
    }
}
```

## 5. 📝 Danh Sách Environment Variables Cần Set

### Tất Cả Services

```bash
# Database
DB_USERNAME=root
DB_PASSWORD=your-db-password

# JWT (tất cả services dùng chung)
JWT_SECRET=your-strong-jwt-secret-at-least-32-characters
```

### Service Product

```bash
# AWS S3
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
AWS_REGION=ap-southeast-2
AWS_S3_BUCKET=your-bucket-name
AWS_S3_FOLDER=product-images
```

### Order Service & User Service

```bash
# Email (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-email@gmail.com
```

### Payment Service

```bash
# SePay
SEPAY_API_URL=https://api.sepay.vn
SEPAY_API_KEY=your-sepay-api-key
SEPAY_SECRET=your-sepay-secret
SEPAY_ACCOUNT_NUMBER=your-account-number
SEPAY_ACCOUNT_NAME=your-account-name
SEPAY_BANK_CODE=your-bank-code
SEPAY_WEBHOOK_VERIFY=false
```

### AI Service

```bash
# Google Gemini
GOOGLE_API_KEY=your-google-api-key
MODEL_NAME=gemini-2.5-flash
USE_GEMINI=true

# Hoặc OpenAI (fallback)
OPENAI_API_KEY=your-openai-api-key

# MySQL
MYSQL_URL=mysql+pymysql://user:password@host:3306/database
```

### Analytics Service

```bash
# Admin credentials
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your-admin-password
```

## 6. ✅ Best Practices

### 1. **Không bao giờ hardcode keys trong code**
✅ Đã làm - tất cả keys dùng environment variables

### 2. **Sử dụng Secrets Management cho Production**
- AWS: Secrets Manager hoặc Parameter Store
- Azure: Key Vault
- GCP: Secret Manager
- Kubernetes: External Secrets Operator

### 3. **Rotate keys định kỳ**
- Đổi keys mỗi 90 ngày
- Có quy trình rotate không làm gián đoạn service

### 4. **Phân quyền truy cập**
- Chỉ những người/service cần thiết mới có quyền đọc secrets
- Dùng IAM roles thay vì hardcode credentials

### 5. **Audit và Monitoring**
- Log mọi truy cập vào secrets
- Alert khi có truy cập bất thường

### 6. **Backup secrets an toàn**
- Lưu backup ở nơi an toàn (encrypted)
- Có recovery plan

## 7. 🔍 Kiểm Tra Keys Đã Được Set Chưa

### Trong Code (Spring Boot)

Thêm endpoint để kiểm tra (chỉ dùng cho development):

```java
@RestController
@RequestMapping("/admin")
public class ConfigController {

    @Value("${aws.s3.access-key:NOT_SET}")
    private String awsAccessKey;

    @GetMapping("/config-check")
    public Map<String, String> checkConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("aws-access-key-set",
            awsAccessKey.equals("NOT_SET") ? "NO" : "YES");
        // ... check other configs
        return config;
    }
}
```

### Trong Docker

```bash
docker exec <container-id> env | grep AWS
docker exec <container-id> env | grep DB_PASSWORD
```

### Trong Kubernetes

```bash
kubectl exec <pod-name> -- env | grep AWS
kubectl describe pod <pod-name> | grep -A 10 "Environment:"
```

## 8. 📚 Tài Liệu Tham Khảo

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [Azure Key Vault](https://docs.microsoft.com/azure/key-vault/)
- [GCP Secret Manager](https://cloud.google.com/secret-manager/docs)
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)

---

**Lưu ý**: File này chứa hướng dẫn chung. Điều chỉnh theo môi trường deploy cụ thể của bạn.

