### User Service – User Management APIs

Base URL qua Gateway: `http://localhost:8085`
Base URL trực tiếp service (dev): `http://localhost:8082`

- Tất cả API (trừ đăng ký/kích hoạt/đăng nhập/OTP) yêu cầu JWT ở header:
  - `Authorization: Bearer <accessToken>`
- Quyền hạn (role) lấy từ JWT claim `role` (USER, ADMIN, MANAGER)

### 0) Các API công khai (không cần JWT)
- POST `/api/users/register` – đăng ký (đã dùng qua Auth Service)
- POST `/api/users/activate` – kích hoạt bằng OTP
- POST `/api/users/resend-otp` – gửi lại OTP
- POST `/api/users/forgot-password` – gửi email OTP reset
- POST `/api/users/reset-password` – đặt lại mật khẩu bằng OTP

Ví dụ body:
```json
{ "email": "a@example.com" }
```
```json
{ "email": "a@example.com", "otp": "123456", "newPassword": "newpass123" }
```

---

### 1) Thông tin tài khoản cá nhân (Profile)
Yêu cầu: JWT hợp lệ (USER/ADMIN/MANAGER đều được).

- GET `/api/users/me` – Lấy thông tin user hiện tại
  - Headers: Authorization: Bearer `<accessToken>`
  - Response 200:
    ```json
    { "id": 1, "fullName": "Nguyen Van A", "email": "a@example.com", "role": "USER", "phoneNumber": "0912345678", "active": true }
    ```

- PUT `/api/users/me` – Cập nhật hồ sơ
  - Body:
    ```json
    { "fullName": "Nguyen Van B", "phoneNumber": "0912345679" }
    ```
  - Response 200: trả về thông tin user đã cập nhật

- PUT `/api/users/me/change-password` – Đổi mật khẩu
  - Body:
    ```json
    { "currentPassword": "123456", "newPassword": "newpass123" }
    ```
  - Response 200: OK

---

### 2) Quản lý user (dành cho Admin/Manager)
- Tất cả endpoint dưới đây yêu cầu:
  - ADMIN hoặc MANAGER: list, xem, tạo, cập nhật, cập nhật trạng thái
  - ADMIN: xóa (vô hiệu hóa) và đổi quyền

- GET `/api/users` – Danh sách người dùng (phân trang, tìm kiếm)
  - Query: `q` (từ khóa theo email/fullName), `page`, `size`, `sort`
  - Ví dụ: `/api/users?q=anh&page=0&size=10&sort=id,desc`
  - Response 200 (Page):
    ```json
    {
      "content": [ {"id":1, "fullName":"Admin", "email":"admin@example.com", "role":"ADMIN", "phoneNumber":"...", "active":true} ],
      "totalElements": 1, "totalPages": 1, "number": 0, "size": 10
    }
    ```
- GET '/api/users/role/MANAGER' – Lấy danh sách manager
- GET '/api/users/role/USER' – Lấy danh sách USER
- GET `/api/users/{id}` – Lấy chi tiết 1 user
- GET '/api/users/manager/{id}' – Lấy chi tiết 1 manager
- POST `/api/users` – Tạo user mới (dùng cho admin/manager)
  - Body:
    ```json
    {
      "fullName": "User B",
      "email": "userb@example.com",
      "password": "123456",
      "phoneNumber": "0911111111",
      "role": "USER"
    }
    ```

- PUT `/api/users/{id}` – Cập nhật thông tin user
  - Body:
    ```json
    {
      "fullName": "User B Updated",
      "email": "userb@example.com",
      "phoneNumber": "0911111111",
      "role": "MANAGER"
    }
    ```

- DELETE `/api/users/{id}` – Vô hiệu hóa user (ADMIN)
  - Response 200: OK

- PATCH `/api/users/{id}/status` – Cập nhật trạng thái (ADMIN/MANAGER)
  - Body:
    ```json
    { "active": true }
    ```

- PATCH `/api/users/{id}/role` – Cập nhật quyền (ADMIN)
  - Body:
    ```json
    { "role": "ADMIN" }
    ```

---

### 3) Khôi phục tài khoản
- POST `/api/users/forgot-password`
  - Body: `{ "email": "a@example.com" }`
  - Kết quả: Gửi OTP tới email.

- POST `/api/users/reset-password`
  - Body: `{ "email": "a@example.com", "otp": "123456", "newPassword": "newpass123" }`

---

### Quyền & kiểm thử nhanh
- Token USER gọi `GET /api/users` → 403 Forbidden
- Token ADMIN gọi `GET /api/users` → 200 OK

### Lưu ý
- Giá trị role hợp lệ: `USER`, `ADMIN`, `MANAGER`
- Số điện thoại: 8–15 chữ số (`^[0-9]{8,15}$`)
- Một số lỗi thường gặp: `Email already exists`, `Phone already exists`, `Account not activated`, `Invalid OTP`, `OTP expired`
