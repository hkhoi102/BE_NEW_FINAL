#!/bin/bash

# ============================================
# Script Tự Động Deploy Smart Retail Backend
# ============================================
# Sử dụng: ./deploy-vps-auto.sh
# ============================================

set -e  # Dừng nếu có lỗi

# Màu sắc cho output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Hàm in thông báo
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Kiểm tra quyền root
if [ "$EUID" -ne 0 ]; then
    print_error "Vui lòng chạy script với quyền root (sudo ./deploy-vps-auto.sh)"
    exit 1
fi

print_info "Bắt đầu deploy Smart Retail Backend..."

# ============================================
# Bước 1: Cập nhật hệ thống
# ============================================
print_info "Bước 1: Cập nhật hệ thống..."
apt update -y
apt upgrade -y
apt install -y curl wget git vim htop net-tools

# ============================================
# Bước 2: Cài Java 17
# ============================================
print_info "Bước 2: Cài đặt Java 17..."
if ! command -v java &> /dev/null; then
    apt install -y openjdk-17-jdk
    print_info "Java 17 đã được cài đặt"
else
    print_warning "Java đã được cài đặt: $(java -version 2>&1 | head -n 1)"
fi

# ============================================
# Bước 3: Cài Maven
# ============================================
print_info "Bước 3: Cài đặt Maven..."
if ! command -v mvn &> /dev/null; then
    apt install -y maven
    print_info "Maven đã được cài đặt"
else
    print_warning "Maven đã được cài đặt: $(mvn -version | head -n 1)"
fi

# ============================================
# Bước 4: Cài MySQL
# ============================================
print_info "Bước 4: Cài đặt MySQL..."
if ! command -v mysql &> /dev/null; then
    apt install -y mysql-server
    systemctl start mysql
    systemctl enable mysql
    print_info "MySQL đã được cài đặt và khởi động"
else
    print_warning "MySQL đã được cài đặt"
fi

# ============================================
# Bước 5: Cài Docker
# ============================================
print_info "Bước 5: Cài đặt Docker..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    rm get-docker.sh
    print_info "Docker đã được cài đặt"
else
    print_warning "Docker đã được cài đặt: $(docker --version)"
fi

# Thêm user vào docker group
if ! groups $SUDO_USER | grep -q docker; then
    usermod -aG docker $SUDO_USER
    print_info "Đã thêm user vào docker group"
fi

# ============================================
# Bước 6: Cài Docker Compose
# ============================================
print_info "Bước 6: Cài đặt Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    print_info "Docker Compose đã được cài đặt"
else
    print_warning "Docker Compose đã được cài đặt: $(docker-compose --version)"
fi

# ============================================
# Bước 7: Cài Python 3.11 (cho AI Service)
# ============================================
print_info "Bước 7: Cài đặt Python 3.11..."
apt install -y python3.11 python3.11-venv python3-pip

# ============================================
# Bước 8: Tạo Databases
# ============================================
print_info "Bước 8: Tạo databases..."
print_warning "Vui lòng nhập password MySQL root:"
read -s MYSQL_ROOT_PASSWORD

# Tạo databases
mysql -u root -p"$MYSQL_ROOT_PASSWORD" <<EOF
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS customer_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS promotion_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS analytics_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS chatbox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EOF

print_info "Databases đã được tạo"

# ============================================
# Bước 9: Tạo User MySQL
# ============================================
print_info "Bước 9: Tạo MySQL user cho ứng dụng..."
print_warning "Nhập password cho app_user:"
read -s APP_USER_PASSWORD

print_warning "Nhập password cho reader user:"
read -s READER_PASSWORD

mysql -u root -p"$MYSQL_ROOT_PASSWORD" <<EOF
CREATE USER IF NOT EXISTS 'app_user'@'%' IDENTIFIED BY '$APP_USER_PASSWORD';
GRANT ALL PRIVILEGES ON product_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON customer_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON user_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON auth_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON promotion_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON analytics_db.* TO 'app_user'@'%';
GRANT ALL PRIVILEGES ON chatbox_db.* TO 'app_user'@'%';

CREATE USER IF NOT EXISTS 'reader'@'%' IDENTIFIED BY '$READER_PASSWORD';
GRANT SELECT ON product_db.* TO 'reader'@'%';
GRANT SELECT ON order_db.* TO 'reader'@'%';
GRANT SELECT ON inventory_db.* TO 'reader'@'%';

FLUSH PRIVILEGES;
EOF

print_info "MySQL users đã được tạo"

# ============================================
# Bước 10: Kiểm tra code đã có chưa
# ============================================
print_info "Bước 10: Kiểm tra code..."
if [ ! -d "/opt/smart-retail/smart-retail-backend" ]; then
    print_warning "Thư mục project chưa tồn tại!"
    print_info "Tạo thư mục..."
    mkdir -p /opt/smart-retail
    print_warning "Vui lòng upload code vào /opt/smart-retail/smart-retail-backend"
    print_warning "Sau đó chạy lại script này"
    exit 1
fi

cd /opt/smart-retail/smart-retail-backend

# ============================================
# Bước 11: Tạo file .env nếu chưa có
# ============================================
print_info "Bước 11: Kiểm tra file .env..."
if [ ! -f ".env" ]; then
    print_warning "File .env chưa tồn tại!"
    if [ -f "env.example" ]; then
        cp env.example .env
        print_info "Đã tạo file .env từ template"
        print_warning "VUI LÒNG SỬA FILE .env VÀ ĐIỀN CÁC THÔNG TIN THẬT!"
        print_warning "Chạy: nano /opt/smart-retail/smart-retail-backend/.env"
        print_warning "Sau đó chạy lại script này"
        exit 1
    else
        print_error "Không tìm thấy env.example!"
        exit 1
    fi
fi

# ============================================
# Bước 12: Build ứng dụng
# ============================================
print_info "Bước 12: Build ứng dụng (có thể mất 10-15 phút)..."
if [ -f "mvnw" ]; then
    chmod +x mvnw
    ./mvnw clean package -DskipTests
else
    mvn clean package -DskipTests
fi

print_info "Build hoàn tất!"

# ============================================
# Bước 13: Tạo Dockerfiles
# ============================================
print_info "Bước 13: Tạo Dockerfiles..."
if [ -f "create-dockerfiles.sh" ]; then
    chmod +x create-dockerfiles.sh
    ./create-dockerfiles.sh
else
    print_warning "Không tìm thấy create-dockerfiles.sh, tạo thủ công..."

    # Tạo Dockerfile cho từng service
    services=("discovery-server:8761" "api-gateway:8085" "service-auth:8081"
              "user-service:8082" "service-customer:8083" "service-product:8084"
              "inventory-service:8086" "promotion-service:8087" "order-service:8088"
              "payment-service:8090")

    for service_info in "${services[@]}"; do
        service=$(echo $service_info | cut -d: -f1)
        port=$(echo $service_info | cut -d: -f2)

        if [ -d "$service" ] && [ ! -f "$service/Dockerfile" ]; then
            cat > "$service/Dockerfile" <<EOF
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE $port

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
            print_info "Đã tạo Dockerfile cho $service"
        fi
    done
fi

# ============================================
# Bước 14: Build Docker images
# ============================================
print_info "Bước 14: Build Docker images (có thể mất 10-15 phút)..."
services=("discovery-server" "api-gateway" "service-auth" "user-service"
          "service-customer" "service-product" "inventory-service"
          "order-service" "promotion-service" "payment-service")

for service in "${services[@]}"; do
    if [ -d "$service" ]; then
        print_info "Building $service..."
        cd $service
        docker build -t ${service}:latest . || print_error "Lỗi khi build $service"
        cd ..
    fi
done

# Build AI Service
if [ -d "ai-service" ]; then
    print_info "Building ai-service..."
    cd ai-service
    docker build -t ai-service:latest . || print_error "Lỗi khi build ai-service"
    cd ..
fi

print_info "Docker images đã được build!"

# ============================================
# Bước 15: Chạy Docker Compose
# ============================================
print_info "Bước 15: Chạy Docker Compose..."
if [ ! -f "docker-compose.yml" ]; then
    if [ -f "docker-compose.example.yml" ]; then
        cp docker-compose.example.yml docker-compose.yml
        print_info "Đã tạo docker-compose.yml từ template"
    else
        print_error "Không tìm thấy docker-compose.yml hoặc docker-compose.example.yml!"
        exit 1
    fi
fi

# Stop containers cũ nếu có
docker-compose down 2>/dev/null || true

# Start containers
docker-compose up -d

print_info "Đợi services khởi động (30 giây)..."
sleep 30

# ============================================
# Bước 16: Kiểm tra
# ============================================
print_info "Bước 16: Kiểm tra services..."

# Kiểm tra containers
print_info "Danh sách containers:"
docker-compose ps

# Kiểm tra Eureka
print_info "Kiểm tra Eureka..."
if curl -s http://localhost:8761 > /dev/null; then
    print_info "✅ Eureka đang chạy tại http://localhost:8761"
else
    print_error "❌ Eureka không chạy được"
fi

# Kiểm tra API Gateway
print_info "Kiểm tra API Gateway..."
if curl -s http://localhost:8085/actuator/health | grep -q "UP"; then
    print_info "✅ API Gateway đang chạy tại http://localhost:8085"
else
    print_warning "⚠️  API Gateway có thể chưa sẵn sàng, đợi thêm..."
fi

# ============================================
# Hoàn thành
# ============================================
echo ""
print_info "============================================"
print_info "🎉 DEPLOY HOÀN TẤT!"
print_info "============================================"
echo ""
print_info "Các URL quan trọng:"
print_info "  - Eureka Dashboard: http://$(hostname -I | awk '{print $1}'):8761"
print_info "  - API Gateway: http://$(hostname -I | awk '{print $1}'):8085"
print_info "  - Health Check: http://$(hostname -I | awk '{print $1}'):8085/actuator/health"
echo ""
print_info "Các lệnh hữu ích:"
print_info "  - Xem logs: docker-compose logs -f"
print_info "  - Xem status: docker-compose ps"
print_info "  - Restart: docker-compose restart"
print_info "  - Stop: docker-compose down"
echo ""
print_warning "Lưu ý: Nếu services chưa sẵn sàng, đợi thêm 1-2 phút và kiểm tra lại"
echo ""

