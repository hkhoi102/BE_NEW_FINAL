#!/bin/bash
# Script để set environment variables cho Linux/Mac
# Usage: source set-env.sh
# Sau đó chạy: java -jar service-name/target/*.jar

# Database
export DB_USERNAME=root
export DB_PASSWORD=sapassword

# JWT
export JWT_SECRET=smartRetailJwtSecretKey_ChangeMe_ToA32BytesMin_StrongKey_2025

# AWS S3 (cho service-product)
export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=
export AWS_REGION=ap-southeast-2
export AWS_S3_BUCKET=
export AWS_S3_FOLDER=product-images

# Email (cho order-service, user-service)
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=
export MAIL_PASSWORD=
export MAIL_FROM=

# SePay (cho payment-service)
export SEPAY_API_KEY=
export SEPAY_SECRET=
export SEPAY_ACCOUNT_NUMBER=
export SEPAY_ACCOUNT_NAME=
export SEPAY_BANK_CODE=

echo "Environment variables đã được set!"
echo ""
echo "Lưu ý: Sửa file này và điền các keys thật của bạn"
echo "Sau đó chạy: java -jar service-name/target/*.jar"

