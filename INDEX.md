# Taskoryx Backend — Mục lục tài liệu

## Đọc file nào trước?

| Mục đích | File | Thời gian |
|----------|------|-----------|
| Setup & chạy ngay | `GETTING_STARTED.md` | 5 phút |
| Hiểu tổng quan dự án | `README.md` | 15 phút |
| Cấu hình nâng cao (S3, AI, email, production) | `CONFIG_GUIDE.md` | 10 phút |
| Tích hợp API từ Frontend | `docs/frontend-api-guide.md` | theo nhu cầu |
| Tham khảo nhanh lệnh / endpoint | `QUICK_REFERENCE.md` | 5 phút |
| Schema database | `docs/DATABASE_GUIDE.md` | 20 phút |

---

## Tất cả tài liệu

### Root

| File | Nội dung |
|------|---------|
| `README.md` | Tổng quan, tính năng, tech stack, kiến trúc |
| `GETTING_STARTED.md` | Hướng dẫn setup & chạy nhanh |
| `CONFIG_GUIDE.md` | Cấu hình DB, JWT, email, S3, AI, CORS, production |
| `QUICK_REFERENCE.md` | Lệnh hay dùng, endpoint chính, enums |
| `CONTRIBUTING.md` | Quy trình đóng góp, coding convention |
| `CLAUDE.md` | Hướng dẫn cho Claude Code AI assistant |

### docs/

| File | Nội dung |
|------|---------|
| `docs/frontend-api-guide.md` | Hướng dẫn tích hợp API đầy đủ cho Frontend (30+ sections) |
| `docs/DATABASE_GUIDE.md` | Thiết kế schema, indexes, relationships |
| `docs/FEATURES.md` | Chi tiết tính năng |
| `docs/TASK_BUSINESS.md` | Nghiệp vụ task (hierarchy, placement, dependencies) |
| `docs/FLOW_DIAGRAMS.md` | Sơ đồ luồng các chức năng chính |
| `docs/performance-system.md` | Hệ thống tính điểm năng lực thành viên |
| `docs/template-business-logic.md` | Nghiệp vụ Project Template |

---

## Trạng thái dự án

### Đã hoàn thành

- [x] JWT Authentication + 2FA (TOTP)
- [x] System RBAC (Role / Permission)
- [x] Project-level RBAC (custom roles per project)
- [x] Quản lý dự án, thành viên, template
- [x] Kanban Board (KANBAN / SCRUM / PERSONAL)
- [x] Sprint management (PLANNED → ACTIVE → COMPLETED)
- [x] Task CRUD, hierarchy 3 cấp, drag & drop, dependencies
- [x] Comment, @mention, reply lồng nhau
- [x] File attachment (local + S3)
- [x] Time tracking + thống kê
- [x] Notification (WebSocket STOMP + email)
- [x] Webhook outbound
- [x] Activity log (audit trail)
- [x] Performance scoring
- [x] Gantt chart API
- [x] Dashboard & Search & Export Excel
- [x] AI — sinh kế hoạch dự án (Gemini / OpenAI)
- [x] Admin panel (users, roles, permissions)

### Công nghệ

| | |
|---|---|
| **Runtime** | Java 17, Spring Boot 3.2.1 |
| **Database** | PostgreSQL 14+, Hibernate / Spring Data JPA |
| **Security** | Spring Security, JJWT 0.12.3, TOTP |
| **Realtime** | WebSocket STOMP / SockJS |
| **Storage** | Local filesystem / AWS S3 |
| **AI** | Google Gemini / OpenAI |
| **Email** | Spring Mail + Gmail SMTP |
| **Build** | Maven 3.8+ |

---

**Last Updated**: 2026-05-20
