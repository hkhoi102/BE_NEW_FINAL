# 🚀 Hướng Dẫn Deploy Nhanh - Smart Retail Backend

## 📚 Tài Liệu

Chọn hướng dẫn phù hợp với bạn:

### 1. **HUONG_DAN_DEPLOY_CHO_NGUOI_MOI.md** ⭐ (Khuyến nghị cho người mới)
- Hướng dẫn từng bước chi tiết nhất
- Giải thích mọi thứ một cách đơn giản
- Dành cho người chưa từng deploy lần nào
- **Bắt đầu từ đây nếu bạn là người mới!**

### 2. **HUONG_DAN_DEPLOY_VPS.md**
- Hướng dẫn deploy lên VPS chi tiết
- Bao gồm cấu hình Nginx, SSL, Firewall
- Dành cho người đã có kinh nghiệm

### 3. **DANH_SACH_SERVICES.md**
- Danh sách tất cả 11 services
- Thông tin chi tiết từng service
- Environment variables cần thiết

### 4. **THU_TU_DEPLOY.md**
- Thứ tự khởi động services
- Checklist deploy

---

## 🎯 Quick Start (Tóm Tắt)

### Yêu Cầu:
- VPS với Ubuntu 20.04/22.04
- RAM: 4GB+ (khuyến nghị 8GB)
- CPU: 2 cores+
- Disk: 50GB+

### Các Bước Chính:

1. **Kết nối VPS**
   ```bash
   ssh root@your-vps-ip
   ```

2. **Chạy script tự động** (Dễ nhất)
   ```bash
   # Upload file deploy-vps-auto.sh lên VPS
   chmod +x deploy-vps-auto.sh
   sudo ./deploy-vps-auto.sh
   ```

3. **Hoặc làm thủ công:**
   - Đọc file: `HUONG_DAN_DEPLOY_CHO_NGUOI_MOI.md`
   - Làm theo từng bước

---

## 📋 Checklist Trước Khi Deploy

- [ ] Đã có VPS
- [ ] Đã có các API keys:
  - [ ] AWS S3 (cho Product Service)
  - [ ] Gmail App Password (cho Email)
  - [ ] SePay API Key (cho Payment)
  - [ ] Google Gemini API Key (cho AI Service)
- [ ] Đã upload code lên VPS
- [ ] Đã tạo file `.env` và điền thông tin

---

## 🔧 Các Lệnh Thường Dùng

### Xem logs:
```bash
docker-compose logs -f
docker-compose logs -f service-product
```

### Kiểm tra status:
```bash
docker-compose ps
```

### Restart services:
```bash
docker-compose restart
docker-compose restart service-product
```

### Stop tất cả:
```bash
docker-compose down
```

### Start lại:
```bash
docker-compose up -d
```

---

## 🌐 URLs Sau Khi Deploy

- **Eureka Dashboard**: `http://your-vps-ip:8761`
- **API Gateway**: `http://your-vps-ip:8085`
- **Health Check**: `http://your-vps-ip:8085/actuator/health`
- **API Endpoint**: `http://your-vps-ip:8085/api/products`

---

## ❓ Cần Giúp Đỡ?

1. Đọc file `HUONG_DAN_DEPLOY_CHO_NGUOI_MOI.md` - Hướng dẫn chi tiết nhất
2. Xem phần Troubleshooting trong các file hướng dẫn
3. Kiểm tra logs: `docker-compose logs -f`

---

## 📝 Ghi Chú

- **Lần đầu deploy**: Mất khoảng 2-3 giờ
- **Các lần sau**: Chỉ cần 10-15 phút
- **Nếu gặp lỗi**: Xem logs và phần Troubleshooting

---

**Chúc bạn deploy thành công!** 🎉

