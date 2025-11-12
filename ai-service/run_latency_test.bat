@echo off
REM Script để chạy test latency trên Windows
echo ============================================================
echo Test Latency AI Chatbot
echo ============================================================
echo.

REM Kiểm tra Python
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python chua duoc cai dat hoac khong co trong PATH
    pause
    exit /b 1
)

REM Kiểm tra aiohttp
python -c "import aiohttp" >nul 2>&1
if errorlevel 1 (
    echo [INFO] Dang cai dat aiohttp...
    pip install aiohttp
    if errorlevel 1 (
        echo [ERROR] Khong the cai dat aiohttp
        pause
        exit /b 1
    )
)

REM Chạy test
echo [INFO] Dang chay test latency...
echo.
python test_latency.py %*

pause


