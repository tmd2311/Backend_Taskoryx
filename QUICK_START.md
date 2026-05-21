# Quick Start — Taskoryx Backend

## 1. Tạo database

```bash
psql -U postgres -c "CREATE DATABASE taskoryx;"
```

## 2. Chạy ứng dụng

```bash
./mvnw spring-boot:run
```

Schema tự tạo khi khởi động (`ddl-auto: update`). Dữ liệu mẫu (admin account, templates) được seed tự động lần đầu.

## 3. Kiểm tra

```bash
# Health check
curl http://localhost:8080/api/actuator/health

# Swagger UI
open http://localhost:8080/api/swagger-ui.html
```

## 4. Đăng nhập

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@taskoryx.com", "password": "admin123"}'
```

Response trả về `accessToken` và `refreshToken`. Gắn vào header:
```
Authorization: Bearer <accessToken>
```

---

Xem thêm:
- **`GETTING_STARTED.md`** — hướng dẫn đầy đủ, troubleshooting
- **`CONFIG_GUIDE.md`** — cấu hình email, S3, AI, production
- **`docs/frontend-api-guide.md`** — tài liệu API cho Frontend
