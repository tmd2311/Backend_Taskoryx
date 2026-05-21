# Bắt đầu nhanh với Taskoryx Backend

## Yêu cầu

- Java 17+
- Maven 3.8+ (hoặc dùng `./mvnw` đi kèm)
- PostgreSQL 14+

---

## 1. Tạo database

```bash
psql -U postgres
CREATE DATABASE taskoryx;
\q
```

---

## 2. Cấu hình

Mở `src/main/resources/application.yaml`, kiểm tra thông tin kết nối:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskoryx
    username: postgres
    password: 123456   # đổi nếu khác
```

Schema sẽ tự cập nhật khi khởi động (`ddl-auto: update`).

---

## 3. Chạy ứng dụng

```bash
./mvnw spring-boot:run
```

Kiểm tra:
- Health check: `http://localhost:8080/api/actuator/health`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`

---

## 4. Tài khoản admin mặc định

`DemoDataInitializer` tự seed dữ liệu khi khởi động lần đầu. Tài khoản admin được tạo sẵn — xem logs console sau khi start để lấy thông tin.

---

## 5. Lệnh thường dùng

```bash
# Compile nhanh (kiểm tra lỗi)
./mvnw compile

# Chạy tất cả tests
./mvnw test

# Chạy 1 test class
./mvnw test -Dtest=TaskServiceTest

# Build JAR (bỏ qua tests)
./mvnw clean package -DskipTests

# Chạy JAR
java -jar target/taskoryx-backend-1.0.0.jar
```

---

## 6. Lỗi thường gặp

### Connection refused
```
✅ Kiểm tra PostgreSQL đang chạy:
   Windows: Services → PostgreSQL
   Linux/macOS: systemctl status postgresql / brew services list
```

### Port 8080 đã bị dùng
```yaml
# Đổi port trong application.yaml:
server:
  port: 8081
```

### JWT signature does not match
```
✅ Clear token cũ trên client, đăng nhập lại.
   JWT secret phải >= 256 bits.
```

### Table / column không tồn tại
```
✅ ddl-auto: update sẽ tự tạo/cập nhật schema.
   Nếu vẫn lỗi, drop database rồi tạo lại.
```

---

## 7. Tài liệu liên quan

| File | Nội dung |
|------|---------|
| `README.md` | Tổng quan dự án, kiến trúc |
| `CONFIG_GUIDE.md` | Cấu hình nâng cao (email, S3, AI, production) |
| `docs/frontend-api-guide.md` | Hướng dẫn tích hợp API cho Frontend |
| `docs/DATABASE_GUIDE.md` | Schema database chi tiết |
