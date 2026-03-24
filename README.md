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

**Taskoryx** là hệ thống REST API hỗ trợ quản lý công việc theo quy trình Agile/Scrum. Backend cung cấp toàn bộ nghiệp vụ: xác thực JWT, phân quyền RBAC, Kanban board, Sprint planning, theo dõi thời gian, thông báo thời gian thực qua WebSocket.

Repository này chứa phần **Backend**. Phần **Frontend** (React + TypeScript) nằm ở repository riêng.

```
Browser (React – port 5173)
        │  HTTP/REST + WebSocket (STOMP/SockJS)
        ▼
Spring Boot API (port 8080)
        │  Spring Data JPA (Hibernate)
        ▼
   PostgreSQL Database (port 5432)
```

---

## Tính năng

### Xác thực & Phân quyền

| Tính năng | Mô tả |
|-----------|-------|
| **JWT Authentication** | Access token 24h, Refresh token stateless, tự động cấp lại khi hết hạn |
| **2FA (TOTP)** | Bật/tắt xác thực 2 bước, tương thích Google Authenticator |
| **RBAC** | Phân quyền theo role dự án: `OWNER` / `MANAGER` / `DEVELOPER` / `VIEWER` |
| **BCrypt** | Mã hóa mật khẩu, buộc đổi mật khẩu khi admin reset |

### Quản lý dự án

| Tính năng | Mô tả |
|-----------|-------|
| **Dự án** | CRUD dự án, phân quyền thành viên, tìm kiếm user để mời |
| **Template** | 4 mẫu dự án có sẵn: Software, Marketing, Design, Event |
| **Nhãn (Label)** | Tạo nhãn màu sắc, gán cho task để phân loại |
| **Danh mục** | Quản lý issue categories theo từng dự án |
| **Activity Feed** | Ghi nhật ký toàn bộ thay đổi trong dự án |

### Task & Board

| Tính năng | Mô tả |
|-----------|-------|
| **Bảng Kanban** | CRUD board và column, di chuyển task giữa cột, giới hạn WIP |
| **Backlog** | Danh sách task chưa gán board; thêm vào Sprint theo yêu cầu |
| **Sprint (Scrum)** | Tạo Sprint, giao task từ Backlog, bắt đầu / hoàn thành Sprint |
| **Task chi tiết** | Assign, priority, deadline, estimate, checklist, attachment, time entry |
| **Task liên kết** | Dependency: `BLOCKS` / `RELATES_TO`; phát hiện vòng tròn tự động |
| **Gantt Chart** | API cung cấp dữ liệu timeline cho các version/milestone |
| **Versions** | Quản lý phiên bản/milestone, theo dõi % hoàn thành |

### Bình luận & @Mention

| Tính năng | Mô tả |
|-----------|-------|
| **Bình luận** | CRUD bình luận, reply lồng nhau 1 cấp |
| **@Mention** | Parse `@username` trong nội dung comment, tự động tạo notification |
| **Tệp đính kèm** | Upload/download/xóa file tối đa 10 MB, lưu local tại `uploads/` |

### Theo dõi thời gian

| Tính năng | Mô tả |
|-----------|-------|
| **Log giờ làm** | CRUD time entry theo task và ngày; tự cập nhật `actualHours` của task |
| **Thống kê ngày** | `GET /time-entries/stats/daily` – chi tiết từng ngày |
| **Thống kê tuần** | `GET /time-entries/stats/weekly` – tổng hợp theo tuần |
| **Thống kê tháng** | `GET /time-entries/stats/monthly` – 12 tháng trong năm |
| **Tổng hợp** | `GET /time-entries/stats/summary` – overview theo dự án & ngày |
| **Thống kê dự án** | `GET /projects/{id}/time-entries/stats` – phân tích theo thành viên & task |

### Thông báo & Realtime

| Tính năng | Mô tả |
|-----------|-------|
| **WebSocket** | STOMP over SockJS; push notification và Kanban live update |
| **Notification** | CRUD notification, đánh dấu đọc từng cái hoặc "Đọc tất cả" |
| **Webhook** | Tích hợp Slack/Discord/hệ thống ngoài qua HTTP (OkHttp) |

### Quản trị & Tiện ích

| Tính năng | Mô tả |
|-----------|-------|
| **Admin** | Quản lý users, roles, permissions; kích hoạt/khóa tài khoản |
| **Export Excel** | Xuất danh sách task của dự án ra file `.xlsx` (13 cột) |
| **Dashboard** | Thống kê tổng quan cá nhân theo dự án |
| **Search** | Tìm kiếm toàn cục tasks, projects, users |
| **Swagger UI** | Tài liệu API tự động tại `/api/swagger-ui.html` |

---

## Công nghệ sử dụng

| Thư viện / Framework | Phiên bản | Mục đích |
|----------------------|-----------|----------|
| [Spring Boot](https://spring.io/projects/spring-boot) | 3.2.1 | Application framework |
| [Spring Security](https://spring.io/projects/spring-security) | (theo Boot) | Authentication & Authorization |
| [Spring Data JPA](https://spring.io/projects/spring-data-jpa) | (theo Boot) | ORM / Data Access Layer |
| [Spring WebSocket](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket) | (theo Boot) | Realtime STOMP/SockJS |
| [PostgreSQL](https://www.postgresql.org) | 14+ | Cơ sở dữ liệu quan hệ |
| [JJWT](https://github.com/jwtk/jjwt) | 0.12.3 | JWT implementation |
| [Lombok](https://projectlombok.org) | (theo Boot) | Giảm boilerplate code |
| [Springdoc OpenAPI](https://springdoc.org) | 2.3.0 | Swagger UI / API docs |
| [Apache POI](https://poi.apache.org) | 5.2.5 | Export Excel (.xlsx) |
| [totp-spring-boot-starter](https://github.com/samdjstevens/java-totp) | 1.7.1 | Two-Factor Authentication (TOTP) |
| [OkHttp](https://square.github.io/okhttp) | 4.12.0 | Webhook HTTP client |
| [Jakarta Bean Validation](https://beanvalidation.org) | (theo Boot) | Validate input request |

---

## Yêu cầu hệ thống

- **Java** >= 17
- **Maven** >= 3.8
- **PostgreSQL** >= 14

---

## Cài đặt & Chạy thử

### 1. Clone repository

```bash
git clone https://github.com/<your-username>/taskoryx-be.git
cd taskoryx-be
```

### 2. Tạo database

```bash
# Kết nối PostgreSQL
psql -U postgres

# Tạo database và schema
\i database-init.sql
\c taskoryx_dev
\i schema.sql
```

### 3. Cấu hình ứng dụng

Sửa file `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskoryx_dev
    username: postgres
    password: your_password
```

### 4. Chạy ứng dụng

```bash
# Chạy trực tiếp với Maven Wrapper
./mvnw spring-boot:run

# Hoặc build JAR rồi chạy
./mvnw clean package
java -jar target/taskoryx-backend-1.0.0.jar
```

### 5. Kiểm tra

```bash
# Health check
curl http://localhost:8080/api/actuator/health

# Swagger UI
# Truy cập: http://localhost:8080/api/swagger-ui.html
```

### Tài khoản mặc định

```
Email:    admin@taskoryx.com
Password: Admin@123
```

---

## Cấu trúc thư mục

```
src/
├── main/
│   ├── java/com/taskoryx/backend/
│   │   ├── TaskoryxApplication.java   # Entry point
│   │   │
│   │   ├── config/                    # Cấu hình Spring (Security, WebSocket, CORS, Swagger)
│   │   ├── security/                  # JWT filter, UserDetailsService, token provider
│   │   ├── entity/                    # JPA Entities (ánh xạ bảng database)
│   │   ├── repository/                # Spring Data JPA repositories
│   │   ├── dto/                       # Request / Response DTOs
│   │   ├── service/                   # Business logic
│   │   ├── controller/                # REST Controllers (@RestController)
│   │   └── exception/                 # Global exception handler (@ControllerAdvice)
│   │
│   └── resources/
│       ├── application.yaml           # Cấu hình chung
│       ├── application-dev.yaml       # Cấu hình môi trường development
│       └── application-prod.yaml      # Cấu hình môi trường production
│
└── test/                              # Unit & integration tests

database-init.sql                      # Khởi tạo database
schema.sql                             # Tạo toàn bộ bảng
uploads/                               # Thư mục lưu file đính kèm
pom.xml                                # Maven dependencies
```

---

## Cấu hình ứng dụng

Các thuộc tính quan trọng trong `application.yaml`:

| Thuộc tính | Mặc định | Mô tả |
|------------|----------|-------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/taskoryx_dev` | JDBC URL kết nối PostgreSQL |
| `spring.datasource.username` | `postgres` | Username database |
| `spring.datasource.password` | _(bắt buộc đặt)_ | Password database |
| `app.jwt.secret` | _(bắt buộc đặt)_ | Secret key ký JWT (>= 256 bit) |
| `app.jwt.expiration` | `86400000` | Thời hạn access token (ms) – mặc định 24h |
| `app.jwt.refresh-expiration` | `604800000` | Thời hạn refresh token (ms) – mặc định 7 ngày |
| `app.upload.dir` | `uploads/` | Thư mục lưu file đính kèm |
| `server.servlet.context-path` | `/api` | Context path của toàn bộ API |

---

## Kiến trúc hệ thống

```
┌──────────────────────────────────────────┐
│           REST CONTROLLERS               │
│  AuthController · ProjectController      │
│  TaskController · BoardController ...    │
├──────────────────────────────────────────┤
│             SERVICE LAYER                │
│  Business logic, validation, mapping     │
│  ProjectService · TaskService ...        │
├──────────────────────────────────────────┤
│           REPOSITORY LAYER               │
│  Spring Data JPA repositories            │
│  Custom JPQL / native queries            │
├──────────────────────────────────────────┤
│              JPA ENTITIES                │
│  User · Project · Task · Board ...       │
├──────────────────────────────────────────┤
│            POSTGRESQL DATABASE           │
│         jdbc:postgresql://localhost:5432 │
└──────────────────────────────────────────┘
         ▲                    ▲
         │ JWT Filter         │ STOMP/SockJS
   Spring Security      WebSocket Broker
```

**Nguyên tắc thiết kế:**

- **Layered Architecture** – Controller → Service → Repository, mỗi tầng chỉ phụ thuộc tầng dưới
- **DTO Pattern** – Không expose entity trực tiếp ra ngoài; dùng Request/Response DTO riêng biệt
- **Stateless Auth** – JWT stateless, không lưu session phía server
- **Global Exception Handling** – `@ControllerAdvice` bắt toàn bộ exception, trả về lỗi chuẩn
- **Soft constraints** – Kiểm tra quyền ở tầng Service, không chỉ dựa vào annotation

---

## Tài liệu API

Swagger UI tự động sinh từ annotation, truy cập sau khi chạy ứng dụng:

```
http://localhost:8080/api/swagger-ui.html
```

### Các nhóm endpoint chính

| Nhóm | Prefix | Mô tả |
|------|--------|-------|
| Auth | `/auth` | Đăng nhập, đăng ký, refresh token, logout, 2FA |
| User | `/users` | Profile, đổi mật khẩu, tìm kiếm user |
| Project | `/projects` | CRUD dự án, thành viên, labels, thống kê giờ |
| Board | `/boards`, `/columns` | Kanban board, quản lý cột, di chuyển cột |
| Task | `/tasks`, `/projects/{id}/tasks` | CRUD task, kéo thả, lọc, tìm kiếm |
| Comment | `/tasks/{id}/comments`, `/comments` | Bình luận, reply, @mention |
| Attachment | `/tasks/{id}/attachments`, `/attachments` | Upload/download/xóa file |
| Checklist | `/tasks/{id}/checklist` | CRUD checklist items |
| Dependency | `/tasks/{id}/dependencies` | Liên kết BLOCKS / RELATES_TO |
| Watcher | `/tasks/{id}/watchers` | Theo dõi / bỏ theo dõi task |
| Time Entry | `/time-entries`, `/tasks/{id}/time-entries` | Log giờ, sửa, xóa, thống kê |
| Sprint | `/projects/{id}/sprints` | CRUD sprint, bắt đầu, hoàn thành |
| Version | `/projects/{id}/versions`, `/gantt` | Milestone, Gantt chart |
| Category | `/projects/{id}/categories` | Danh mục issue |
| Activity | `/projects/{id}/activity` | Nhật ký hoạt động |
| Notification | `/notifications` | Danh sách, đánh dấu đọc, đếm chưa đọc |
| Dashboard | `/dashboard/me` | Thống kê tổng quan cá nhân |
| Template | `/templates/public` | Template dự án công khai |
| Search | `/search` | Tìm kiếm toàn cục |
| Admin | `/admin/*` | Quản trị users, roles, permissions |

---

## Đóng góp

Xem hướng dẫn đóng góp tại [CONTRIBUTING.md](./CONTRIBUTING.md).

---

## Giấy phép

Dự án được phát hành dưới giấy phép **MIT**. Xem chi tiết tại [LICENSE](./LICENSE).
