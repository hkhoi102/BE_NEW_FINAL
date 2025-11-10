# 📤 Hướng Dẫn Upload Code Lên VPS Bằng WinSCP - Chi Tiết Từng Bước

## 🎯 Mục Đích

Hướng dẫn này giúp bạn upload thư mục `smart-retail-backend` từ máy Windows lên VPS bằng WinSCP.

---

## 📋 Chuẩn Bị

### Cần Có:
- ✅ WinSCP đã cài đặt (download: https://winscp.net/)
- ✅ Thông tin VPS:
  - IP Address (ví dụ: `103.229.52.246`)
  - Username: `root`
  - Password: password của VPS
- ✅ Code đã có sẵn trên máy Windows (ví dụ: `D:\DATN\code\smart-retail-backend`)

---

## 🚀 Các Bước Thực Hiện

### Bước 1: Tạo Thư Mục Trên VPS

**Trước khi upload, cần tạo thư mục trên VPS:**

**Cách 1: Dùng WinSCP Terminal**

1. Mở WinSCP và kết nối vào VPS (xem Bước 2)
2. Click vào menu **"Commands"** → **"Open Terminal"**
3. Gõ lệnh:
   ```bash
   mkdir -p /opt/smart-retail
   ```
4. Nhấn Enter

**Cách 2: Dùng SSH (PuTTY hoặc PowerShell)**

1. Kết nối SSH vào VPS
2. Chạy lệnh:
   ```bash
   mkdir -p /opt/smart-retail
   ```

---

### Bước 2: Kết Nối WinSCP Vào VPS

1. **Mở WinSCP**

2. **Tạo kết nối mới:**
   - Click **"New Site"** hoặc **"New Session"** (hoặc nhấn `Ctrl+N`)

3. **Điền thông tin kết nối:**

   | Trường | Giá Trị | Ví Dụ |
   |--------|---------|-------|
   | **File protocol** | SFTP | SFTP |
   | **Host name** | IP của VPS | `103.229.52.246` |
   | **Port number** | 22 | `22` |
   | **User name** | root | `root` |
   | **Password** | Password của VPS | `your-password` |

   **Giao diện sẽ trông như thế này:**
   ```
   File protocol: [SFTP ▼]
   Host name:     [103.229.52.246        ]
   Port number:   [22                    ]
   User name:     [root                  ]
   Password:      [••••••••              ]
   ```

4. **Lưu kết nối (tùy chọn):**
   - Click **"Save"** để lưu lại, đặt tên (ví dụ: "My VPS")
   - Lần sau chỉ cần double-click để kết nối

5. **Kết nối:**
   - Click **"Login"** hoặc nhấn Enter
   - Lần đầu có thể hỏi xác nhận, click **"Yes"** hoặc **"Add"**

6. **Kiểm tra kết nối thành công:**
   - Bạn sẽ thấy 2 cửa sổ:
     - **Bên trái**: Máy tính của bạn (Local - Windows)
     - **Bên phải**: VPS (Remote - Linux)

---

### Bước 3: Điều Hướng Đến Thư Mục Đúng

#### 3.1. Bên Trái (Local - Máy Windows):

1. Click vào thanh địa chỉ bên trái (hoặc dùng dropdown)
2. Điều hướng đến thư mục chứa code:
   - Ví dụ: `D:\DATN\code\`
3. Tìm thư mục `smart-retail-backend`

**Hoặc:**
- Click vào **"C:"** trong danh sách bên trái
- Điều hướng: `C:` → `Users` → `YourName` → ... → `smart-retail-backend`
- Hoặc gõ đường dẫn trực tiếp vào thanh địa chỉ: `D:\DATN\code\`

#### 3.2. Bên Phải (Remote - VPS):

1. Click vào thanh địa chỉ bên phải
2. Gõ đường dẫn: `/opt/smart-retail`
3. Nhấn Enter

**Hoặc:**
- Click vào **"/"** (root) trong danh sách
- Điều hướng: `/` → `opt` → `smart-retail`
- Nếu chưa có thư mục `smart-retail`, tạo bằng cách:
  - Right-click vào `/opt/` → **"New"** → **"Directory"**
  - Đặt tên: `smart-retail`
  - Click **"OK"**

---

### Bước 4: Upload Thư Mục

**Có 3 cách upload:**

#### Cách 1: Kéo Thả (Drag & Drop) - Dễ Nhất ⭐

1. **Bên trái**: Tìm và chọn thư mục `smart-retail-backend`
2. **Kéo** thư mục đó từ bên trái
3. **Thả** vào cửa sổ bên phải (vào thư mục `/opt/smart-retail/`)
4. WinSCP sẽ hỏi xác nhận, click **"Copy"** hoặc **"OK"**

**Lưu ý:**
- Đảm bảo thả vào **bên trong** thư mục `/opt/smart-retail/`
- Không thả vào `/root/` hoặc thư mục khác

#### Cách 2: Right-Click → Upload

1. **Bên trái**: Right-click vào thư mục `smart-retail-backend`
2. Chọn **"Upload"** hoặc **"Copy"**
3. WinSCP sẽ hỏi thư mục đích
4. Chọn `/opt/smart-retail/` (hoặc gõ trực tiếp)
5. Click **"OK"**

#### Cách 3: Dùng Nút Upload

1. **Bên trái**: Chọn thư mục `smart-retail-backend`
2. Click nút **"Upload"** trên thanh công cụ (mũi tên lên ↑)
3. Chọn thư mục đích: `/opt/smart-retail/`
4. Click **"OK"**

---

### Bước 5: Chờ Upload Hoàn Tất

1. **WinSCP sẽ hiện cửa sổ progress:**
   - Hiển thị tiến trình upload
   - Tốc độ upload
   - Số file đã upload / tổng số file
   - Thời gian còn lại

2. **Thời gian upload:**
   - Tùy kích thước code (thường 5-15 phút)
   - Tùy tốc độ internet
   - Tùy số lượng file

3. **Khi upload xong:**
   - Cửa sổ progress sẽ tự đóng
   - Hoặc hiện thông báo "Transfer completed"

**Lưu ý:**
- ⚠️ **KHÔNG đóng WinSCP** trong khi đang upload
- ⚠️ **KHÔNG tắt máy** trong khi đang upload
- ✅ Nếu upload bị gián đoạn, có thể thử lại (WinSCP sẽ hỏi có tiếp tục không)

---

### Bước 6: Kiểm Tra Upload Thành Công

#### 6.1. Kiểm Tra Trên WinSCP:

1. **Bên phải (Remote)**: Refresh (nhấn `F5`)
2. Điều hướng đến `/opt/smart-retail/`
3. Bạn sẽ thấy thư mục `smart-retail-backend`

#### 6.2. Kiểm Tra Bằng Terminal:

**Cách 1: Dùng WinSCP Terminal**

1. Trong WinSCP, click **"Commands"** → **"Open Terminal"**
2. Chạy lệnh:
   ```bash
   ls -la /opt/smart-retail/
   ```
3. Bạn sẽ thấy:
   ```
   drwxr-xr-x  root root  smart-retail-backend
   ```

**Cách 2: Dùng SSH**

1. Kết nối SSH vào VPS
2. Chạy các lệnh:
   ```bash
   # Kiểm tra thư mục đã có chưa
   ls -la /opt/smart-retail/

   # Vào thư mục
   cd /opt/smart-retail/smart-retail-backend

   # Xem danh sách các thư mục con
   ls -la
   ```

**Bạn sẽ thấy các thư mục:**
- ✅ `discovery-server/`
- ✅ `api-gateway/`
- ✅ `service-auth/`
- ✅ `user-service/`
- ✅ `service-customer/`
- ✅ `service-product/`
- ✅ `inventory-service/`
- ✅ `order-service/`
- ✅ `promotion-service/`
- ✅ `payment-service/`
- ✅ `ai-service/`
- ✅ Và các file khác (pom.xml, README.md, etc.)

**Nếu thấy đầy đủ = ✅ Upload thành công!**

---

## 🔧 Troubleshooting

### Lỗi 1: "Permission denied"

**Nguyên nhân:** Không có quyền ghi vào thư mục

**Giải pháp:**
```bash
# Trên VPS (SSH hoặc WinSCP Terminal)
chmod 755 /opt/smart-retail
chown -R root:root /opt/smart-retail
```

### Lỗi 2: "Connection timeout" hoặc "Connection refused"

**Nguyên nhân:**
- VPS chưa mở port 22
- Firewall chặn
- IP hoặc password sai

**Giải pháp:**
- Kiểm tra lại IP và password
- Kiểm tra VPS có đang chạy không
- Kiểm tra firewall trên VPS

### Lỗi 3: Upload bị gián đoạn

**Nguyên nhân:**
- Mất kết nối internet
- VPS restart

**Giải pháp:**
- Thử upload lại
- WinSCP sẽ hỏi có tiếp tục từ chỗ dừng không → Chọn "Yes"

### Lỗi 4: Không thấy thư mục sau khi upload

**Nguyên nhân:**
- Upload vào thư mục sai
- Chưa refresh

**Giải pháp:**
1. Refresh bên phải (nhấn `F5`)
2. Kiểm tra lại đường dẫn: `/opt/smart-retail/`
3. Tìm kiếm: Click **"Find Files"** (Ctrl+F), tìm `smart-retail-backend`

### Lỗi 5: Upload quá chậm

**Nguyên nhân:**
- File quá lớn
- Kết nối internet chậm

**Giải pháp:**
- Đợi upload hoàn tất (có thể mất 15-30 phút)
- Hoặc nén code thành file .zip trước, upload file .zip, rồi giải nén trên VPS:
  ```bash
  # Trên VPS
  cd /opt/smart-retail
  unzip smart-retail-backend.zip
  ```

---

## ✅ Checklist

Sau khi upload xong, kiểm tra:

- [ ] Thư mục `/opt/smart-retail/smart-retail-backend/` đã tồn tại trên VPS
- [ ] Có đầy đủ các thư mục con (discovery-server, api-gateway, etc.)
- [ ] Có file `pom.xml` ở thư mục root
- [ ] Có file `env.example`
- [ ] Có file `docker-compose.example.yml`

---

## 🎉 Hoàn Thành!

Sau khi upload thành công, bạn có thể tiếp tục với các bước tiếp theo:

1. **Bước 6**: Cấu hình Environment Variables
2. **Bước 7**: Build ứng dụng
3. **Bước 8**: Chạy services

Xem file `HUONG_DAN_DEPLOY_CHO_NGUOI_MOI.md` để tiếp tục!

---

## 💡 Mẹo Hữu Ích

### Tăng tốc upload:
- Tắt antivirus tạm thời
- Đóng các ứng dụng khác đang dùng internet
- Upload vào giờ ít người dùng

### Kiểm tra kích thước:
```bash
# Trên Windows (PowerShell)
Get-ChildItem -Path "D:\DATN\code\smart-retail-backend" -Recurse | Measure-Object -Property Length -Sum

# Trên VPS (sau khi upload)
du -sh /opt/smart-retail/smart-retail-backend
```

### Upload lại nếu cần:
- Xóa thư mục cũ trên VPS: `rm -rf /opt/smart-retail/smart-retail-backend`
- Upload lại từ đầu

---

**Chúc bạn upload thành công!** 🚀

