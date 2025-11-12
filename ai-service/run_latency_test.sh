#!/bin/bash
# Script để chạy test latency trên Linux/Mac

echo "============================================================"
echo "Test Latency AI Chatbot"
echo "============================================================"
echo ""

# Kiểm tra Python
if ! command -v python3 &> /dev/null; then
    echo "[ERROR] Python3 chưa được cài đặt"
    exit 1
fi

# Kiểm tra aiohttp
python3 -c "import aiohttp" 2>/dev/null
if [ $? -ne 0 ]; then
    echo "[INFO] Đang cài đặt aiohttp..."
    pip3 install aiohttp
    if [ $? -ne 0 ]; then
        echo "[ERROR] Không thể cài đặt aiohttp"
        exit 1
    fi
fi

# Chạy test
echo "[INFO] Đang chạy test latency..."
echo ""
python3 test_latency.py "$@"


