# 🚀 Hướng Dẫn Deploy Smart Retail Backend

## 📚 Tài Liệu Deploy

Dự án này có **3 tài liệu hướng dẫn deploy** tùy theo nhu cầu của bạn:

### 1. ⚡ **Quick Start** - Deploy Nhanh (30 phút)
📄 File: `deploy-quick-start.md`

**Dành cho**: Người muốn deploy nhanh, đã quen với Docker

**Nội dung**:
- Các bước deploy nhanh nhất
- Sử dụng Docker Compose
- Checklist ngắn gọn

👉 **[Bắt đầu với Quick Start →](deploy-quick-start.md)**

---

### 2. 📖 **Hướng Dẫn Chi Tiết** - Từng Bước (Đầy đủ)
📄 File: `HUONG_DAN_DEPLOY_CHI_TIET.md`

**Dành cho**: Người mới, muốn hiểu rõ từng bước

**Nội dung**:
- Yêu cầu hệ thống
- Setup database chi tiết
- Build ứng dụng
- Deploy với Docker
- Deploy truyền thống (JAR files)
- Troubleshooting đầy đủ

👉 **[Xem Hướng Dẫn Chi Tiết →](HUONG_DAN_DEPLOY_CHI_TIET.md)**

---

### 3. 🔐 **Quản Lý Keys & Secrets** - Bảo Mật
📄 File: `DEPLOYMENT_GUIDE.md` và `HUONG_DAN_DEPLOY.md`

**Dành cho**: Người cần hiểu cách quản lý keys khi deploy

**Nội dung**:
- Cách set environment variables
- Quản lý secrets trên các platform
- Best practices bảo mật

👉 **[Xem Hướng Dẫn Keys →](HUONG_DAN_DEPLOY.md)**

---

## 🎯 Bạn Nên Bắt Đầu Từ Đâu?

### Nếu bạn là người mới:
1. Đọc `HUONG_DAN_DEPLOY_CHI_TIET.md` - Hướng dẫn đầy đủ
2. Làm theo từng bước
3. Tham khảo `deploy-quick-start.md` nếu cần checklist nhanh

### Nếu bạn đã quen với Docker:
1. Đọc `deploy-quick-start.md` - Quick start
2. Tham khảo `HUONG_DAN_DEPLOY_CHI_TIET.md` nếu gặp vấn đề

### Nếu bạn cần deploy lên Cloud:
1. Đọc `DEPLOYMENT_GUIDE.md` - Hướng dẫn cho AWS, Azure, GCP
2. Tham khảo `HUONG_DAN_DEPLOY.md` - Environment variables

---

## 📋 Checklist Nhanh

Trước khi deploy, đảm bảo bạn có:

- [ ] Java 21 đã cài
- [ ] Maven hoặc mvnw
- [ ] MySQL 8.0+ đã setup
- [ ] Docker & Docker Compose (nếu dùng Docker)
- [ ] File `.env` đã được tạo và điền keys
- [ ] Databases đã được tạo
- [ ] Ports đã được mở trên firewall

---

## 🛠️ Scripts Hỗ Trợ

Dự án có sẵn các scripts để giúp bạn deploy dễ dàng hơn:

### Build Tất Cả Services
```bash
# Windows
build-all.bat

# Linux/Mac
chmod +x build-all.sh
./build-all.sh
```

### Tạo Dockerfiles Tự Động
```bash
# Linux/Mac
chmod +x create-dockerfiles.sh
./create-dockerfiles.sh
```

---

## 🐳 Docker Compose

File mẫu: `docker-compose.example.yml`

**Cách dùng**:
```bash
# 1. Copy file mẫu
copy docker-compose.example.yml docker-compose.yml

# 2. Sửa nếu cần (thường không cần)

# 3. Chạy
docker-compose up -d

# 4. Xem logs
docker-compose logs -f
```

---

## 📞 Cần Hỗ Trợ?

1. **Kiểm tra logs**: `docker-compose logs -f` hoặc `journalctl -u service-name`
2. **Xem Troubleshooting**: Trong `HUONG_DAN_DEPLOY_CHI_TIET.md`
3. **Kiểm tra Eureka**: Mở `http://localhost:8761` để xem services đã đăng ký chưa

---

## 🎉 Chúc Bạn Deploy Thành Công!

**Bắt đầu ngay**: [Quick Start Guide](deploy-quick-start.md)

