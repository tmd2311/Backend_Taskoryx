# Taskoryx - Phần mền quản lí công việc

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-green?style=for-the-badge&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue?style=for-the-badge&logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

**Hệ thống quản lý công việc mạnh mẽ, linh hoạt và mã nguồn mở**

[🚀 Bắt đầu](#-quick-start)  • [✨ Tính năng](#-) • [🤝 Đóng góp](#đóng-góp)

</div>

---

##  Mục lục

- [Giới thiệu](#giới-thiệu)
- [Chức năng](#chức-năng)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Đóng góp](#đóng-góp)
- [License](#license)
- [Liên hệ hỗ trợ](#liên-hệ-hỗ-trợ)

---

## Giới thiệu

**Taskoryx** là một hệ thống quản lý task hiện đại, được thiết kế để giúp các team làm việc hiệu quả hơn. Lấy cảm hứng từ Trello và Jira, Taskoryx kết hợp tính đơn giản của Kanban board với sức mạnh của project management.

## Chức năng

### Tính năng đã implement (v1.0)

#### Chức năng gốc
-  **Quản lý User** - Đăng ký, đăng nhập, phân quyền RBAC
-  **Two-Factor Auth (2FA)** - TOTP / Google Authenticator
-  **Quản lý Project** - Tạo workspace, quản lý members
-  **Project Templates** - 4 mẫu dự án có sẵn (Software, Marketing, Design, Event)
-  **Kanban Board** - Drag & drop tasks giữa các columns (WIP limit)
-  **Task Management** - Tạo, sửa, xóa, assign, priority, deadline
-  **Backlog** - Task chưa được lên board
-  **Task Dependencies** - Phụ thuộc giữa tasks (circular detection)
-  **Checklist** - Danh sách việc nhỏ trong task (với progress %)
-  **Sprint / Milestone** - Lập kế hoạch sprint, burndown
-  **Time Tracking** - Ghi nhận giờ làm việc, tự cập nhật actualHours
-  **Comments & @Mentions** - Collaboration, gửi notification khi mention
-  **File Attachments** - Đính kèm files (max 10MB)
-  **Labels & Tags** - Phân loại tasks
-  **Activity Feed** - Lịch sử mọi thay đổi trong project
-  **Dashboard & Stats** - Thống kê theo project và cá nhân
-  **Search & Filter** - Tìm kiếm nhanh
-  **Export Excel** - Xuất danh sách task ra .xlsx (13 cột)
-  **Webhooks** - Tích hợp Slack/Discord/hệ thống ngoài
-  **WebSocket Real-time** - Kanban live update, notification real-time

#### Hiệu năng và bảo mật
-  JWT Authentication (stateless)
-  Two-Factor Authentication (TOTP/HOTP)
-  Role-based Access Control (OWNER, ADMIN, MEMBER, VIEWER)
-  BCrypt password hashing
-  Database indexing & query optimization
-  Pagination & async processing

### Roadmap

#### Đang kế hoạch
- 📅 **Gantt Chart** - Timeline visualization
- 📅 **Calendar View** - Xem tasks theo lịch
- 📅 **Automation Rules** - Tự động hóa (kéo vào Done → gửi email)
- 📅 **Google Calendar Integration** - Đồng bộ deadline
- 📅 **Mobile App** - iOS & Android
- 📅 **Docker Compose** - Triển khai 1 lệnh

---

##  Tech Stack

### Backend
- **Framework**: Spring Boot 3.2.1
- **Language**: Java 17
- **ORM**: Spring Data JPA (Hibernate)
- **Database**: PostgreSQL 14+
- **Security**: Spring Security + JWT
- **Validation**: Jakarta Bean Validation
- **Documentation**: Swagger/OpenAPI (Springdoc)
- **Build Tool**: Maven

### Libraries
- **Lombok** - Reduce boilerplate code
- **JJWT 0.12.3** - JWT implementation
- **PostgreSQL Driver** - Database connectivity
- **Apache POI 5.2.5** - Excel export
- **TOTP Spring Boot Starter 1.7.1** - Two-Factor Authentication
- **OkHttp 4.12.0** - Webhook HTTP client
- **Spring WebSocket** - Real-time STOMP/SockJS

### Tools
- **Git** - Version control
- **Maven** - Dependency management
- **Docker** - Containerization (planned)
- **GitHub Actions** - CI/CD (planned)

---

## Quick Start

### Prerequisites

- Java 17+
- PostgreSQL 14+
- Maven 3.8+

### Installation

#### 1. Clone repository

```bash
git clone https://github.com/yourusername/taskoryx-backend.git
cd taskoryx-backend
```

#### 2. Setup database

```bash
# Kết nối PostgreSQL
psql -U postgres

# Tạo database
\i database-init.sql

# Tạo schema
\c taskoryx_dev
\i schema.sql
```

#### 3. Configure application

Sửa file `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskoryx_dev
    username: postgres
    password: your_password
```

#### 4. Run application

```bash
# Sử dụng Maven
./mvnw spring-boot:run

# Hoặc build JAR
./mvnw clean package
java -jar target/taskoryx-backend-1.0.0.jar
```

#### 5. Verify

```bash
# Check health
curl http://localhost:8080/api/actuator/health

# Open Swagger UI
# Browser: http://localhost:8080/api/swagger-ui.html
```

### Default Account

```
Email: admin@taskoryx.com
Password: Admin@123
```


---

##  Cấu trúc dự án

```
taskoryx-backend/
├── src/
│   ├── main/
│   │   ├── java/com/taskoryx/backend/
│   │   │   ├── entity/          # JPA Entities
│   │   │   ├── repository/      # Data Access Layer
│   │   │   ├── service/         # Business Logic
│   │   │   ├── controller/      # REST Controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── security/        # Security & JWT
│   │   │   ├── exception/       # Exception Handling
│   │   │   └── config/          # Configuration
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       └── application-prod.yaml
│   └── test/                    # Tests
│
├── database-init.sql            # DB initialization
├── schema.sql                   # DB schema
├── DATABASE_DESIGN.md           # Design document
├── ERD.md                       # ER Diagram
├── SETUP_GUIDE.md               # Setup guide
├── pom.xml                      # Maven config
└── README.md                    # This file
```

---
## Đóng góp

Mình rất hoan nghênh mọi đóng góp! Đây là dự án mã nguồn mở.

### Cách đóng góp:

1. **Fork** repository này
2. **Clone** fork của bạn
3. **Create** một feature branch (`git checkout -b feature/AmazingFeature`)
4. **Commit** changes (`git commit -m 'Add some AmazingFeature'`)
5. **Push** to branch (`git push origin feature/AmazingFeature`)
6. **Open** Pull Request

### Coding Guidelines:

- Follow Java code conventions
- Write meaningful commit messages
- Add unit tests for new features
- Update documentation
- Comment complex logic

### Areas needing help:

-  Bug fixes
-  New features
-  Documentation
-  Testing
-  Translations
-  UI/UX improvements

---

##  License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 Taskoryx Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

## Liên hệ hỗ trợ

- **Issues**: [GitHub Issues](https://github.com/yourusername/taskoryx/issues)
- **Email**: dung.kayc@gmail.com

---

##  Acknowledgments

- Spring Boot team for the amazing framework
- PostgreSQL community
- All contributors to this project

---

<div align="center">

**⭐ Nếu project này hữu ích, hãy cho mình một star! ⭐**

Made with ❤️ by DungTM

[⬆ Back to top](#-taskoryx---task-management-system)

</div>
