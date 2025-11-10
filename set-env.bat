@echo off
REM Script để set environment variables cho Windows
REM Usage: set-env.bat
REM Sau đó chạy: java -jar service-name/target/*.jar

REM Database
set DB_USERNAME=root
set DB_PASSWORD=sapassword

REM JWT
set JWT_SECRET=smartRetailJwtSecretKey_ChangeMe_ToA32BytesMin_StrongKey_2025

REM AWS S3 (cho service-product)
set AWS_ACCESS_KEY_ID=
set AWS_SECRET_ACCESS_KEY=
set AWS_REGION=ap-southeast-2
set AWS_S3_BUCKET=
set AWS_S3_FOLDER=product-images

REM Email (cho order-service, user-service)
set MAIL_HOST=smtp.gmail.com
set MAIL_PORT=587
set MAIL_USERNAME=
set MAIL_PASSWORD=
set MAIL_FROM=

REM SePay (cho payment-service)
set SEPAY_API_KEY=
set SEPAY_SECRET=
set SEPAY_ACCOUNT_NUMBER=
set SEPAY_ACCOUNT_NAME=
set SEPAY_BANK_CODE=

echo Environment variables đã được set!
echo.
echo Lưu ý: Sửa file này và điền các keys thật của bạn
echo Sau đó chạy: java -jar service-name/target/*.jar

