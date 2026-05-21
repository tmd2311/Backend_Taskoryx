# Quick Reference — Taskoryx Backend

## Chạy nhanh

```bash
# Tạo database (1 lần)
psql -U postgres -c "CREATE DATABASE taskoryx;"

# Chạy ứng dụng
./mvnw spring-boot:run
```

- API: `http://localhost:8080/api`
- Swagger: `http://localhost:8080/api/swagger-ui.html`
- Health: `http://localhost:8080/api/actuator/health`

---

## Lệnh Maven thường dùng

```bash
./mvnw compile                          # Kiểm tra lỗi nhanh
./mvnw spring-boot:run                  # Chạy dev
./mvnw test                             # Chạy tất cả tests
./mvnw test -Dtest=TaskServiceTest      # Chạy 1 test class
./mvnw clean package -DskipTests        # Build JAR
```

---

## Cấu trúc package

```
com.taskoryx.backend/
├── ai/              # Module AI (Gemini / OpenAI)
├── config/          # SecurityConfig, WebSocketConfig, ...
├── controller/      # REST Controllers
├── dto/
│   ├── request/     # Request bodies
│   └── response/    # Response DTOs
├── entity/          # JPA Entities (UUID PKs)
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── repository/      # Spring Data JPA
├── security/        # JWT, UserPrincipal, JwtAuthFilter
└── service/         # Business logic
```

---

## Các endpoint chính

| Nhóm | Prefix |
|------|--------|
| Auth | `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` |
| User | `GET /users/me`, `GET /users/me/permissions` |
| Project | `GET /projects`, `POST /projects`, `GET /projects/{id}` |
| Board | `GET /projects/{id}/boards`, `GET /boards/{id}/kanban` |
| Task | `POST /projects/{id}/tasks`, `GET /tasks/{id}`, `PATCH /tasks/{id}/move` |
| Sprint | `POST /projects/{id}/sprints`, `POST /sprints/{id}/start` |
| Comment | `GET /tasks/{id}/comments`, `POST /tasks/{id}/comments` |
| Attachment | `POST /tasks/{id}/attachments`, `GET /attachments/{id}/download` |
| Notification | `GET /notifications`, `PATCH /notifications/read-all` |
| AI | `POST /ai/projects/generate`, `POST /ai/projects/confirm` |
| Admin | `GET /admin/users`, `GET /admin/roles` |

---

## Response format

```json
{
  "success": true,
  "message": "Thành công",
  "data": { ... },
  "timestamp": "2026-05-20T00:00:00"
}
```

Phân trang: `?page=0&size=20` → trả `PagedResponse<T>`.

---

## Auth

```
Authorization: Bearer <accessToken>
```

- Access token: 24h
- Refresh token: 7 ngày
- Endpoint refresh: `POST /auth/refresh`

---

## Enums quan trọng

```
TaskStatus:   TODO | IN_PROGRESS | IN_REVIEW | RESOLVED | DONE | CANCELLED
TaskPriority: LOW | MEDIUM | HIGH | URGENT
BoardType:    KANBAN | SCRUM | PERSONAL
SprintStatus: PLANNED | ACTIVE | COMPLETED | CANCELLED
```

---

## Task placement

| Vị trí | sprint | column |
|--------|--------|--------|
| Product Backlog | null | null |
| Sprint Backlog | != null | null |
| On Board | any | != null |

---

## Tài liệu

| File | Nội dung |
|------|---------|
| `README.md` | Tổng quan, tính năng, kiến trúc |
| `GETTING_STARTED.md` | Setup nhanh |
| `CONFIG_GUIDE.md` | Cấu hình chi tiết (DB, JWT, S3, AI, email) |
| `docs/frontend-api-guide.md` | Hướng dẫn API cho Frontend (30+ sections) |
| `docs/DATABASE_GUIDE.md` | Schema database |
