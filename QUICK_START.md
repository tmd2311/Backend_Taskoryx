# 🚀 Hướng Dẫn Khởi Động Nhanh Taskoryx Backend

## Bước 1: Cài Đặt PostgreSQL

### Windows
```bash
# Download từ: https://www.postgresql.org/download/windows/
# Hoặc dùng chocolatey:
choco install postgresql
```

### macOS
```bash
brew install postgresql
brew services start postgresql
```

### Linux
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

## Bước 2: Tạo Database

```bash
# Mở PostgreSQL command line
psql -U postgres

# Trong psql, chạy:
CREATE DATABASE taskoryx_dev;
\q
```

## Bước 3: Cấu Hình Application

### Tạo file .env từ template
```bash
copy .env.example .env
```

### Chỉnh sửa file .env
Mở `.env` và cập nhật các giá trị:

```env
# Database
DATABASE_PASSWORD=postgres

# JWT (Giữ nguyên hoặc tạo mới nếu muốn)
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# Email (Bỏ qua nếu chưa cần)
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

### Hoặc chỉnh sửa trực tiếp application.yaml
Mở `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    username: postgres
    password: postgres  # Đổi thành password PostgreSQL của bạn
```

## Bước 4: Chạy Ứng Dụng

### Sử dụng Maven Wrapper (Khuyến nghị)
```bash
# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

# macOS/Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Hoặc sử dụng Maven đã cài
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Hoặc chạy từ IDE
1. Mở project trong IntelliJ IDEA / Eclipse
2. Tìm main class (class có @SpringBootApplication)
3. Right click → Run
4. Set VM options: `-Dspring.profiles.active=dev`

## Bước 5: Kiểm Tra

### 1. Check Console
Sau khi chạy, bạn sẽ thấy:
```
Started TaskoryxBackendApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

### 2. Test API
Mở trình duyệt và truy cập:

- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/api-docs
- **Health Check**: http://localhost:8080/api/actuator/health (nếu có actuator)

### 3. Test Database
Check console logs, phải thấy:
```
HikariPool-1 - Start completed
Hibernate: create table ...
```

## Bước 6: Test API với Postman/cURL

### Tạo User Mới (Register)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Đăng Nhập (Login)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

## ⚠️ Troubleshooting

### Lỗi: "Connection refused"
```
✅ Giải pháp:
1. Kiểm tra PostgreSQL đang chạy:
   - Windows: Services → PostgreSQL
   - macOS/Linux: systemctl status postgresql

2. Test connection:
   psql -U postgres -d taskoryx_dev
```

### Lỗi: "Port 8080 already in use"
```
✅ Giải pháp:
1. Tìm process đang dùng port:
   netstat -ano | findstr :8080  (Windows)
   lsof -i :8080                  (macOS/Linux)

2. Hoặc đổi port trong application.yaml:
   server:
     port: 8081
```

### Lỗi: "Table doesn't exist"
```
✅ Giải pháp:
Đảm bảo trong application.yaml hoặc application-dev.yaml:
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Hoặc update
```

### Lỗi: "JWT signature does not match"
```
✅ Giải pháp:
1. Clear browser/storage
2. Đảm bảo JWT secret có ít nhất 256 bits
3. Restart application
```

## 📝 Các Lệnh Hữu Ích

### Maven
```bash
# Clean và build
mvnw clean install

# Skip tests
mvnw clean install -DskipTests

# Run specific profile
mvnw spring-boot:run -Dspring.profiles.active=dev
```

### Database
```bash
# Backup database
pg_dump -U postgres taskoryx_dev > backup.sql

# Restore database
psql -U postgres taskoryx_dev < backup.sql

# Drop và tạo lại database
psql -U postgres -c "DROP DATABASE taskoryx_dev;"
psql -U postgres -c "CREATE DATABASE taskoryx_dev;"
```

### Logs
```bash
# Xem logs realtime
tail -f logs/taskoryx.log

# Xem 100 dòng cuối
tail -n 100 logs/taskoryx.log

# Tìm errors trong logs
grep "ERROR" logs/taskoryx.log
```

## 🎯 Next Steps

1. ✅ Xem Swagger UI để hiểu các API endpoints
2. ✅ Đọc `CONFIG_GUIDE.md` để cấu hình chi tiết
3. ✅ Cấu hình email để test gửi mail
4. ✅ Tạo entities và repositories cho business logic
5. ✅ Test APIs với Postman
6. ✅ Kết nối với frontend

## 📚 Tài Liệu Thêm

- [CONFIG_GUIDE.md](CONFIG_GUIDE.md) - Hướng dẫn cấu hình chi tiết
- [database-init.sql](database-init.sql) - SQL scripts khởi tạo database
- Spring Boot Docs: https://docs.spring.io/spring-boot/docs/current/reference/html/

## 🆘 Cần Trợ Giúp?

Nếu gặp vấn đề:
1. Check console logs
2. Check `logs/taskoryx.log`
3. Check PostgreSQL logs
4. Google error message
5. Check Spring Boot documentation

---

**Happy Coding! 🎉**
