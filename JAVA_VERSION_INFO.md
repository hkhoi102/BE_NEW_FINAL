# ☕ Thông Tin Về Java Version

## ✅ Bạn Có Thể Dùng JDK 17!

Dự án này **hỗ trợ Java 17 và Java 21**. Tất cả các service con đã được cấu hình để dùng Java 17.

## 📋 Yêu Cầu

- **Tối thiểu**: Java 17 (JDK 17)
- **Khuyến nghị**: Java 17 hoặc Java 21
- **Spring Boot 3.2.3** yêu cầu tối thiểu Java 17

## 🔍 Kiểm Tra Version

```bash
java -version
```

Kết quả phải hiển thị:
```
openjdk version "17.x.x"  # ✅ OK
# hoặc
openjdk version "21.x.x"  # ✅ OK
```

## 📝 Đã Cập Nhật

Tôi đã sửa:
- ✅ Root `pom.xml`: `<java.version>17</java.version>`
- ✅ Hướng dẫn deploy: Cập nhật yêu cầu Java 17
- ✅ Dockerfile mẫu: Dùng `openjdk:17-jdk-slim`

## 🚀 Build Với Java 17

Sau khi cài JDK 17, bạn có thể build bình thường:

```bash
# Windows
.\mvnw.cmd clean package -DskipTests

# Linux/Mac
./mvnw clean package -DskipTests
```

## ⚠️ Lưu Ý

- Nếu bạn đang dùng Java 21, vẫn hoạt động bình thường (backward compatible)
- Nếu bạn đang dùng Java 11 hoặc thấp hơn, **PHẢI nâng cấp lên Java 17+**
- Spring Boot 3.x không hỗ trợ Java 11

## 🔗 Tải Java 17

- **Adoptium (Eclipse Temurin)**: https://adoptium.net/temurin/releases/?version=17
- **Oracle JDK**: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
- **Amazon Corretto**: https://aws.amazon.com/corretto/

---

**Tóm lại**: Bạn có thể dùng JDK 17 hoặc JDK 21, cả hai đều được hỗ trợ! ✅

