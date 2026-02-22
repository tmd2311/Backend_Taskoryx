# Hướng Dẫn Cấu Hình Taskoryx Backend

## 1. Cài Đặt Database PostgreSQL

### Tạo Database

```sql
-- Kết nối vào PostgreSQL
psql -U postgres

-- Tạo database cho môi trường development
CREATE DATABASE taskoryx_dev;

-- Tạo database cho môi trường production
CREATE DATABASE taskoryx_prod;

-- Tạo user (tuỳ chọn)
CREATE USER taskoryx_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE taskoryx_dev TO taskoryx_user;
GRANT ALL PRIVILEGES ON DATABASE taskoryx_prod TO taskoryx_user;
```

### Cấu Hình Database

Mở file `src/main/resources/application.yaml` và cập nhật thông tin database:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskoryx_db
    username: postgres
    password: your_password_here
```

## 2. Cấu Hình Email (Gmail)

### Tạo App Password cho Gmail

1. Truy cập: https://myaccount.google.com/security
2. Bật "2-Step Verification"
3. Tạo "App Password" tại: https://myaccount.google.com/apppasswords
4. Copy mật khẩu 16 ký tự

### Cập nhật cấu hình email

```yaml
spring:
  mail:
    username: your_email@gmail.com
    password: your_16_char_app_password
```

## 3. Cấu Hình JWT Secret

### Tạo JWT Secret Key mới

Sử dụng tool online hoặc chạy code Java:

```java
import java.security.SecureRandom;
import java.util.Base64;

public class JwtSecretGenerator {
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64]; // 512 bits
        random.nextBytes(bytes);
        String secret = Base64.getEncoder().encodeToString(bytes);
        System.out.println("JWT Secret: " + secret);
    }
}
```

Hoặc dùng OpenSSL:

```bash
openssl rand -base64 64
```

Cập nhật trong `application.yaml`:

```yaml
jwt:
  secret: your_generated_secret_key_here
  expiration: 86400000          # 24 hours in milliseconds
  refresh-expiration: 604800000  # 7 days in milliseconds
```

## 4. Chạy Ứng Dụng

### Development Mode

```bash
# Sử dụng Maven
mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Hoặc
mvnw clean install
java -jar target/taskoryx-backend-1.0.0.jar --spring.profiles.active=dev
```

### Production Mode

```bash
# Set environment variables
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_PASSWORD=your_secure_password
export JWT_SECRET=your_jwt_secret
export MAIL_PASSWORD=your_mail_password

# Run application
java -jar target/taskoryx-backend-1.0.0.jar
```

## 5. Kiểm Tra Cấu Hình

### API Endpoints

- **Health Check**: http://localhost:8080/api/actuator/health
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/api-docs

### Test Database Connection

```bash
# Check logs khi start application
# Phải thấy:
# - "HikariPool-1 - Start completed"
# - "Hibernate: ..." (SQL queries)
```

## 6. Troubleshooting

### Lỗi kết nối database

```
Error: Connection refused
```

**Giải pháp:**
- Kiểm tra PostgreSQL đã chạy: `pg_isready`
- Kiểm tra port: `netstat -an | findstr 5432`
- Kiểm tra username/password trong application.yaml

### Lỗi JWT

```
Error: JWT signature does not match
```

**Giải pháp:**
- Đảm bảo JWT secret ít nhất 256 bits
- Không thay đổi secret khi đã có user đăng nhập

### Lỗi Email

```
Error: AuthenticationFailedException
```

**Giải pháp:**
- Kiểm tra đã bật 2FA và tạo App Password
- Kiểm tra username là email đầy đủ
- Kiểm tra port 587 không bị block

## 7. Environment Variables (Production)

Tạo file `.env` (không commit lên git):

```bash
cp .env.example .env
# Sau đó chỉnh sửa các giá trị trong .env
```

## 8. Security Checklist

- [ ] Đổi JWT secret mặc định
- [ ] Đổi database password
- [ ] Sử dụng App Password cho Gmail
- [ ] Không commit file .env
- [ ] Set `ddl-auto: validate` trong production
- [ ] Disable Swagger trong production
- [ ] Enable HTTPS trong production
- [ ] Set up firewall rules
- [ ] Regular backup database

## 9. CORS Configuration

Nếu frontend chạy trên domain khác, cập nhật:

```yaml
application:
  cors:
    allowed-origins:
      - http://localhost:3000
      - https://yourdomain.com
```

## 10. Logging

Logs được lưu tại: `logs/taskoryx.log`

Xem logs realtime:

```bash
tail -f logs/taskoryx.log
```

## Liên Hệ Hỗ Trợ

Nếu gặp vấn đề, kiểm tra:
1. Console logs khi start application
2. File logs/taskoryx.log
3. PostgreSQL logs
