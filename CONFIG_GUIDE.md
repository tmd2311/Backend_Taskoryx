# Hướng dẫn cấu hình Taskoryx Backend

## 1. Database

```yaml
# src/main/resources/application.yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskoryx
    username: postgres
    password: 123456
  jpa:
    hibernate:
      ddl-auto: update   # tự cập nhật schema
    open-in-view: false
```

Tạo database nếu chưa có:
```bash
psql -U postgres -c "CREATE DATABASE taskoryx;"
```

---

## 2. JWT

```yaml
jwt:
  secret: <base64-key-min-256-bits>
  expiration: 86400000        # access token: 24h
  refresh-expiration: 604800000  # refresh token: 7 ngày
```

Tạo secret key:
```bash
openssl rand -base64 64
```

---

## 3. Email (Gmail SMTP)

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@gmail.com
    password: your_16_char_app_password   # Gmail App Password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

Tạo App Password: Google Account → Security → App passwords.

---

## 4. File upload

```yaml
app:
  upload-dir: uploads   # thư mục local (dev)
  max-file-size: 10MB
  max-request-size: 15MB
```

---

## 5. AI Provider

```yaml
# Chọn provider qua env var AI_PROVIDER (mặc định: gemini)
```

| Biến môi trường | Mô tả |
|---|---|
| `AI_PROVIDER` | `gemini` hoặc `openai` |
| `GEMINI_API_KEY` | API key Gemini |
| `OPENAI_API_KEY` | API key OpenAI |

---

## 6. AWS S3 (production)

Khi `AI_PROVIDER` không set, file upload dùng local. Production dùng S3:

| Biến môi trường | Mô tả |
|---|---|
| `S3_BUCKET` | Tên bucket |
| `S3_REGION` | VD: `ap-southeast-1` |
| `S3_ACCESS_KEY` | AWS Access Key ID |
| `S3_SECRET_KEY` | AWS Secret Access Key |

---

## 7. CORS

```yaml
application:
  cors:
    allowed-origins:
      - http://localhost:5173   # Vite dev server
      - http://localhost:3000
      - https://yourdomain.com
```

---

## 8. Logging

Logs lưu tại `logs/taskoryx.log` (rotation 10MB, giữ 30 ngày).

```bash
# Xem realtime
tail -f logs/taskoryx.log

# Lọc lỗi
grep "ERROR" logs/taskoryx.log
```

---

## 9. Chạy production

```bash
# Build
./mvnw clean package -DskipTests

# Set env vars rồi chạy
export SPRING_PROFILES_ACTIVE=prod
export S3_BUCKET=my-bucket
export S3_REGION=ap-southeast-1
export S3_ACCESS_KEY=...
export S3_SECRET_KEY=...
export GEMINI_API_KEY=...
java -jar target/taskoryx-backend-1.0.0.jar
```

`application-prod.yaml` tự động: tắt Swagger, bật S3, đọc DB từ env vars.

---

## 10. Security checklist (production)

- [ ] Đổi `jwt.secret` thành key ngẫu nhiên >= 256 bits
- [ ] Đổi `spring.datasource.password`
- [ ] Set `ddl-auto: validate` (không dùng `update` trên prod)
- [ ] Swagger đã tắt tự động theo `application-prod.yaml`
- [ ] Bật HTTPS (reverse proxy hoặc Spring SSL)
- [ ] Giới hạn CORS chỉ domain thực tế
- [ ] Backup database định kỳ

---

## 11. Actuator

```
GET http://localhost:8080/api/actuator/health
GET http://localhost:8080/api/actuator/info
```

Chỉ expose `health` và `info` (cấu hình sẵn trong `application.yaml`).
