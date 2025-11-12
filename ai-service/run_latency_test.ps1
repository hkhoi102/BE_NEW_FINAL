# Script PowerShell để chạy test latency trên Windows
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Test Latency AI Chatbot" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra Python
try {
    $pythonVersion = python --version 2>&1
    Write-Host "[INFO] $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Python chưa được cài đặt hoặc không có trong PATH" -ForegroundColor Red
    exit 1
}

# Kiểm tra aiohttp
try {
    python -c "import aiohttp" 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "aiohttp not found"
    }
} catch {
    Write-Host "[INFO] Đang cài đặt aiohttp..." -ForegroundColor Yellow
    pip install aiohttp
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Không thể cài đặt aiohttp" -ForegroundColor Red
        exit 1
    }
}

# Chạy test
Write-Host "[INFO] Đang chạy test latency..." -ForegroundColor Green
Write-Host ""
python test_latency.py $args


