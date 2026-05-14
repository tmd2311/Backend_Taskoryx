# Taskoryx – Backend

> Phần mềm quản lý công việc theo mô hình Agile/Scrum, xây dựng bằng Spring Boot + PostgreSQL.

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.8-C71A36?logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-22c55e)

---

## Mục lục

- [Tổng quan](#tổng-quan)
- [Tính năng](#tính-năng)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cài đặt & Chạy thử](#cài-đặt--chạy-thử)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Cấu hình ứng dụng](#cấu-hình-ứng-dụng)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Tài liệu API](#tài-liệu-api)
- [Đóng góp](#đóng-góp)
- [Giấy phép](#giấy-phép)

---

## Tổng quan

**Taskoryx** là hệ thống REST API hỗ trợ quản lý công việc theo quy trình Agile/Scrum. Backend cung cấp toàn bộ nghiệp vụ: xác thực JWT + 2FA, phân quyền RBAC 2 tầng (system + project), Kanban board, Sprint planning, theo dõi thời gian, thông báo thời gian thực qua WebSocket, tích hợp AI để sinh kế hoạch dự án từ ngôn ngữ tự nhiên.

Repository này chứa phần **Backend**. Phần **Frontend** (React + TypeScript) nằm ở repository riêng.

```
Browser (React – port 5173)
        │  HTTP/REST + WebSocket (STOMP/SockJS)
        ▼
Spring Boot API (port 8080)  ─── Gemini / OpenAI API
        │  Spring Data JPA (Hibernate)
        ▼
   PostgreSQL Database (port 5432)
```

---

## Tính năng

### Xác thực & Phân quyền

| Tính năng | Mô tả |
|-----------|-------|
| **JWT Authentication** | Access token 24h, Refresh token 7 ngày, stateless |
| **2FA (TOTP)** | Bật/tắt xác thực 2 bước, tương thích Google Authenticator |
| **System RBAC** | Phân quyền hệ thống: `Role` → `Permission`; flatten thành `GrantedAuthority` |
| **Project-level RBAC** | Custom role per-project; permissions lưu CSV; `ProjectAuthorizationService` kiểm tra 2 tầng |
| **BCrypt** | Mã hóa mật khẩu; admin có thể reset password |

### Quản lý dự án

| Tính năng | Mô tả |
|-----------|-------|
| **Dự án** | CRUD dự án, field `key` (2–10 ký tự hoa), `projectType`, `projectConfig` JSON |
| **Thành viên** | Mời/xóa thành viên; 2 role đặc biệt: `OWNER`, `PM`; tìm kiếm để @mention |
| **Template** | 4 system template sẵn (Software/Marketing/Design/Event); tạo project từ template |
| **Custom Roles** | Tạo role tùy chỉnh per-project với danh sách permission CSV |
| **Nhãn (Label)** | Tạo nhãn màu sắc per-project, gán cho task |
| **Danh mục** | `IssueCategory` per-project, có `defaultAssignee` |
| **Activity Log** | Audit log bất đồng bộ (`@Async`) mọi thay đổi; `oldValue`/`newValue` JSONB |
| **Performance** | Tính điểm năng lực thành viên: onTime, completion, timeAccuracy, engagement |
| **Gantt Chart** | API cung cấp dữ liệu timeline |

### Task & Board

| Tính năng | Mô tả |
|-----------|-------|
| **Bảng Kanban** | CRUD board/column; float position; WIP limit; `mappedStatus` |
| **Backlog** | Task chưa gán sprint/column (`sprint=null, column=null`) |
| **Sprint (Scrum)** | PLANNED→ACTIVE→COMPLETED/CANCELLED; chỉ 1 ACTIVE/project |
| **Task hierarchy** | Self-ref tối đa 3 cấp qua `parentTask`; `getDepth()`, `canHaveChildren()` |
| **Task detail** | Assign, priority, deadline, estimate, position (BigDecimal), taskKey |
| **Task Dependencies** | BLOCKS / DEPENDS_ON / RELATES_TO / DUPLICATES / PRECEDES / FOLLOWS; phát hiện vòng tròn |
| **Task Watchers** | Theo dõi/bỏ theo dõi task; kiểm tra trạng thái theo dõi |
| **Kéo thả** | `PATCH /tasks/{id}/move` với `targetColumnId` + `newPosition` |

### Bình luận & Attachment

| Tính năng | Mô tả |
|-----------|-------|
| **Comment** | CRUD; reply lồng nhau qua `parent` |
| **@Mention** | Parse `@username` → tạo `CommentMention` → push notification |
| **Attachment** | Upload/download/inline; max 10 MB/file, 15 MB/request; `FileCategory` enum |
| **Storage** | Local (`uploads/`) hoặc AWS S3 theo env; `StorageService` interface |

### Theo dõi thời gian

| Tính năng | Mô tả |
|-----------|-------|
| **Log giờ làm** | CRUD `TimeTracking`; `workDate`; tự cập nhật `actualHours` của task |
| **Thống kê** | Daily / Weekly / Monthly / Summary; thống kê theo project |

### Thông báo & Realtime

| Tính năng | Mô tả |
|-----------|-------|
| **WebSocket** | STOMP over SockJS; endpoint `/ws`; broadcast `/topic/project/{id}` |
| **Notification** | TASK_ASSIGNED / TASK_UPDATED / TASK_COMMENTED / MENTION / DUE_DATE_REMINDER / PROJECT_INVITE |
| **Email** | Gmail SMTP qua Spring Mail + Thymeleaf template |
| **Webhook** | Outbound HTTP (OkHttp); events CSV; có `successCount`/`failureCount` |

### AI

| Tính năng | Mô tả |
|-----------|-------|
| **Sinh kế hoạch** | `POST /ai/projects/generate` — mô tả tự nhiên → preview kế hoạch (chưa lưu DB) |
| **Xác nhận tạo** | `POST /ai/projects/confirm` — tạo thật Project + Tasks từ preview |
| **Multi-provider** | `AiChatService` interface; impl: `GeminiChatService`, `OpenAiChatService`; chọn qua `AI_PROVIDER` env |

### Quản trị & Tiện ích

| Tính năng | Mô tả |
|-----------|-------|
| **Admin** | Quản lý users, roles, permissions; kích hoạt/khóa tài khoản |
| **Export Excel** | Xuất danh sách task ra `.xlsx` (Apache POI) |
| **Dashboard** | Thống kê tổng quan cá nhân và project |
| **Search** | Tìm kiếm toàn cục tasks, projects, users |
| **Swagger UI** | Tài liệu API tự động tại `/api/swagger-ui.html` (chỉ dev) |

---

## Công nghệ sử dụng

| Thư viện / Framework | Phiên bản | Mục đích |
|----------------------|-----------|----------|
| Spring Boot | 3.2.1 | Application framework |
| Spring Security | (theo Boot) | Authentication & Authorization |
| Spring Data JPA | (theo Boot) | ORM / Data Access Layer |
| Spring WebSocket | (theo Boot) | Realtime STOMP/SockJS |
| Spring Mail | (theo Boot) | Gửi email qua SMTP |
| Spring Thymeleaf | (theo Boot) | Email template rendering |
| PostgreSQL | 14+ | Cơ sở dữ liệu quan hệ |
| H2 | (test scope) | In-memory DB cho unit test |
| JJWT | 0.12.3 | JWT implementation |
| Lombok | (theo Boot) | Giảm boilerplate code |
| Springdoc OpenAPI | 2.3.0 | Swagger UI / API docs |
| Apache POI | 5.2.5 | Export Excel (.xlsx) |
| totp-spring-boot-starter | 1.7.1 | Two-Factor Authentication (TOTP) |
| OkHttp | 4.12.0 | Webhook HTTP client |
| AWS SDK S3 | 2.25.27 | File storage (production) |
| Jackson Databind | (theo Boot) | JSON serialization |
| Jakarta Bean Validation | (theo Boot) | Validate input request |

---

## Yêu cầu hệ thống

- **Java** >= 17
- **Maven** >= 3.8 (hoặc dùng `./mvnw` đi kèm)
- **PostgreSQL** >= 14

---

## Cài đặt & Chạy thử

### 1. Clone repository

```bash
git clone https://github.com/<your-username>/taskoryx-be.git
cd taskoryx-be
```

### 2. Tạo database

```sql
-- Kết nối PostgreSQL rồi chạy:
CREATE DATABASE taskoryx;
```

### 3. Cấu hình ứng dụng

Mở `src/main/resources/application.yaml`, kiểm tra:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskoryx
    username: postgres
    password: 123456
```

Các thông tin nhạy cảm nên đặt qua biến môi trường (xem mục [Cấu hình ứng dụng](#cấu-hình-ứng-dụng)).

### 4. Chạy ứng dụng

```bash
# Chạy trực tiếp (dev)
./mvnw spring-boot:run

# Hoặc build JAR rồi chạy
./mvnw clean package -DskipTests
java -jar target/taskoryx-backend-1.0.0.jar
```

### 5. Kiểm tra

```bash
# Health check
curl http://localhost:8080/api/actuator/health

# Swagger UI
open http://localhost:8080/api/swagger-ui.html
```

### Tài khoản demo

Ứng dụng seed dữ liệu mẫu khi khởi động lần đầu (`DemoDataInitializer`).

---

## Cấu trúc thư mục

```
src/
├── main/
│   ├── java/com/taskoryx/backend/
│   │   ├── TaskoryxApplication.java        # Entry point (@EnableAsync)
│   │   │
│   │   ├── ai/                             # Module tích hợp LLM
│   │   │   ├── controller/                 # AiProjectPlanController
│   │   │   ├── dto/                        # Request/Response cho AI
│   │   │   ├── parser/                     # AiResponseParser (parse JSON từ LLM)
│   │   │   ├── prompt/                     # ProjectPlanPrompt (template prompt)
│   │   │   ├── service/                    # AiChatService (interface)
│   │   │   │   └── impl/                   # GeminiChatService, OpenAiChatService
│   │   │   └── skill/                      # AiPlanExecutor (thực thi kế hoạch)
│   │   │
│   │   ├── config/                         # SecurityConfig, WebSocketConfig,
│   │   │                                   # OpenApiConfig, ValidationConfig
│   │   ├── security/                       # JwtTokenProvider, JwtAuthFilter,
│   │   │                                   # UserPrincipal, CustomUserDetailsService
│   │   ├── entity/                         # JPA Entities (UUID PKs)
│   │   ├── repository/                     # Spring Data JPA repositories
│   │   ├── dto/
│   │   │   ├── request/                    # Request bodies (sub-packages theo feature)
│   │   │   └── response/                   # Response DTOs (sub-packages theo feature)
│   │   ├── service/                        # Business logic
│   │   ├── controller/                     # REST Controllers
│   │   └── exception/                      # Custom exceptions + GlobalExceptionHandler
│   │
│   └── resources/
│       ├── application.yaml                # Cấu hình dev
│       ├── application-dev.yaml            # Override dev
│       ├── application-prod.yaml           # Production (S3, no Swagger, env vars)
│       └── ValidationMessages.properties  # Validation messages tiếng Việt (UTF-8)
│
└── test/                                   # Unit & integration tests (H2)

uploads/                                    # Thư mục lưu file đính kèm (local)
pom.xml                                     # Maven dependencies
```

---

## Cấu hình ứng dụng

### Biến môi trường quan trọng (production)

| Biến | Mô tả |
|------|-------|
| `AI_PROVIDER` | Provider LLM: `gemini` (mặc định) hoặc `openai` |
| `GEMINI_API_KEY` | API key cho Gemini |
| `OPENAI_API_KEY` | API key cho OpenAI |
| `S3_BUCKET` | Tên S3 bucket (production) |
| `S3_REGION` | AWS region |
| `S3_ACCESS_KEY` | AWS access key |
| `S3_SECRET_KEY` | AWS secret key |

### Các thuộc tính chính trong `application.yaml`

| Thuộc tính | Mặc định | Mô tả |
|------------|----------|-------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/taskoryx` | JDBC URL |
| `spring.datasource.username` | `postgres` | Username DB |
| `spring.datasource.password` | `123456` | Password DB |
| `jwt.secret` | _(đặt qua env)_ | Secret key ký JWT (>= 256 bit) |
| `jwt.expiration` | `86400000` | Access token TTL (ms) – 24h |
| `jwt.refresh-expiration` | `604800000` | Refresh token TTL (ms) – 7 ngày |
| `app.upload-dir` | `uploads` | Thư mục lưu file local |
| `server.servlet.context-path` | `/api` | Context path toàn bộ API |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema tự cập nhật theo entity |
| `spring.jpa.open-in-view` | `false` | Tắt OSIV để tránh lazy load ngoài transaction |

---

## Kiến trúc hệ thống

```
┌──────────────────────────────────────────────────┐
│                REST CONTROLLERS                  │
│  AuthController · ProjectController              │
│  TaskController · BoardController · ...          │
│  AiProjectPlanController                         │
├──────────────────────────────────────────────────┤
│               SERVICE LAYER                      │
│  Business logic, permission check, DTO mapping   │
│  ProjectService · TaskService · SprintService    │
│  ActivityLogService (@Async) · EmailService      │
│  NotificationService · WebhookService            │
│  ProjectAuthorizationService (2-layer check)     │
├──────────────────────────────────────────────────┤
│            REPOSITORY LAYER                      │
│  Spring Data JPA + custom JPQL/native queries    │
├──────────────────────────────────────────────────┤
│               JPA ENTITIES                       │
│  User · Project · Task · Board · Sprint          │
│  ActivityLog · Notification · Webhook · ...      │
├──────────────────────────────────────────────────┤
│           POSTGRESQL DATABASE                    │
│      jdbc:postgresql://localhost:5432/taskoryx   │
└──────────────────────────────────────────────────┘
         ▲                      ▲
         │ JwtAuthFilter         │ STOMP/SockJS
   Spring Security         WebSocket Broker
```

**Nguyên tắc thiết kế:**

- **Layered Architecture** – Controller → Service → Repository; controller không chứa business logic
- **DTO bắt buộc** – Không expose entity thô ra API; dùng `ApiResponse<T>` / `PagedResponse<T>` cho mọi response
- **Stateless Auth** – JWT stateless; không lưu session phía server
- **Global Exception Handling** – `GlobalExceptionHandler` bắt `ResourceNotFoundException` (404), `ForbiddenException` (403), `BadRequestException` (400)
- **Async side-effects** – `ActivityLogService`, `NotificationService`, `EmailService`, `WebhookService` chạy `@Async`; phải load entity đầy đủ trước khi truyền sang async thread
- **Permission model 2 tầng** – System-level qua `GrantedAuthority` + `@PreAuthorize`; Project-level qua `ProjectAuthorizationService.requirePermission()`

---

## Tài liệu API

Swagger UI tự động sinh từ annotation, truy cập sau khi chạy ứng dụng (chỉ môi trường dev):

```
http://localhost:8080/api/swagger-ui.html
```

Hướng dẫn tích hợp cho Frontend: [`docs/frontend-api-guide.md`](./docs/frontend-api-guide.md)

### Các nhóm endpoint chính

| Nhóm | Prefix | Mô tả |
|------|--------|-------|
| Auth | `/auth` | Đăng nhập, refresh token, logout |
| 2FA | `/auth/2fa` | Setup, enable, disable, status |
| User | `/users` | Profile, đổi mật khẩu, avatar, tìm kiếm, performance |
| Project | `/projects` | CRUD dự án, thành viên, roles, activity, gantt, performance |
| Board | `/boards`, `/columns` | Kanban board, quản lý cột, di chuyển cột |
| Task | `/tasks`, `/projects/{id}/tasks` | CRUD task, kéo thả, filter, tìm kiếm, valid-parents |
| Sprint | `/projects/{id}/sprints`, `/sprints` | CRUD sprint, bắt đầu, hoàn thành |
| Comment | `/tasks/{id}/comments`, `/comments` | Bình luận, reply, @mention |
| Attachment | `/tasks/{id}/attachments`, `/attachments` | Upload/download/inline/xóa file |
| Label | `/projects/{id}/labels`, `/labels` | Nhãn per-project |
| Category | `/projects/{id}/categories`, `/categories` | Issue category |
| Dependency | `/tasks/{id}/dependencies` | Liên kết task |
| Watcher | `/tasks/{id}/watchers` | Theo dõi task |
| Time Entry | `/time-entries`, `/tasks/{id}/time-entries` | Log giờ, thống kê |
| Activity | `/projects/{id}/activity`, `/tasks/{id}/activity` | Nhật ký hoạt động |
| Notification | `/notifications` | Danh sách, đánh dấu đọc, đếm chưa đọc |
| Dashboard | `/dashboard` | Thống kê cá nhân và project |
| Performance | `/projects/{id}/performance` | Điểm năng lực thành viên |
| Template | `/templates` | CRUD template; tạo project từ template |
| Project Roles | `/project-roles` | Cập nhật/xóa custom role |
| Webhook | `/projects/{id}/webhooks`, `/webhooks` | CRUD webhook, test ping |
| Search | `/search` | Tìm kiếm toàn cục |
| Export | `/export` | Xuất task ra Excel |
| AI | `/ai/projects` | Sinh kế hoạch từ ngôn ngữ tự nhiên |
| Admin | `/admin` | Quản trị users, roles, permissions `[ADMIN_ACCESS]` |

---

## Đóng góp

Xem hướng dẫn đóng góp tại [CONTRIBUTING.md](./CONTRIBUTING.md).

---

## Giấy phép

Dự án được phát hành dưới giấy phép **MIT**. Xem chi tiết tại [LICENSE](./LICENSE).
