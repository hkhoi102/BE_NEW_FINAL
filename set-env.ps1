# Script để set environment variables cho Windows PowerShell
# Usage: .\set-env.ps1
# Sau đó chạy: java -jar service-name/target/*.jar

# Database
$env:DB_USERNAME="root"
$env:DB_PASSWORD="sapassword"

# JWT
$env:JWT_SECRET="smartRetailJwtSecretKey_ChangeMe_ToA32BytesMin_StrongKey_2025"

# AWS S3 (cho service-product)
$env:AWS_ACCESS_KEY_ID=""
$env:AWS_SECRET_ACCESS_KEY=""
$env:AWS_REGION="ap-southeast-2"
$env:AWS_S3_BUCKET=""
$env:AWS_S3_FOLDER="product-images"

# Email (cho order-service, user-service)
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME=""
$env:MAIL_PASSWORD=""
$env:MAIL_FROM=""

# SePay (cho payment-service)
$env:SEPAY_API_KEY=""
$env:SEPAY_SECRET=""
$env:SEPAY_ACCOUNT_NUMBER=""
$env:SEPAY_ACCOUNT_NAME=""
$env:SEPAY_BANK_CODE=""

Write-Host "Environment variables đã được set!" -ForegroundColor Green
Write-Host ""
Write-Host "Lưu ý: Sửa file này và điền các keys thật của bạn" -ForegroundColor Yellow
Write-Host "Sau đó chạy: java -jar service-name/target/*.jar" -ForegroundColor Yellow

