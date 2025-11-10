@echo off
REM Script để build tất cả services trên Windows
REM Usage: build-all.bat

echo 🚀 Building Smart Retail Backend Services...
echo.

REM Danh sách services
set services=discovery-server api-gateway service-auth user-service service-customer service-product inventory-service order-service promotion-service payment-service

REM Build từng service
for %%s in (%services%) do (
    echo 📦 Building %%s...
    cd %%s
    if exist "..\mvnw.cmd" (
        call ..\mvnw.cmd clean package -DskipTests
    ) else (
        call mvn clean package -DskipTests
    )

    if errorlevel 1 (
        echo ❌ Failed to build %%s
        exit /b 1
    )

    echo ✅ %%s built successfully
    echo.
    cd ..
)

echo 🎉 All services built successfully!
echo.
echo JAR files are in each service's target/ directory
pause

