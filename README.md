# 🎯 Taskoryx - Task Management System

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-green?style=for-the-badge&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue?style=for-the-badge&logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

**Hệ thống quản lý công việc mạnh mẽ, linh hoạt và mã nguồn mở**

[🚀 Bắt đầu](#-quick-start) • [📖 Tài liệu](#-documentation) • [✨ Tính năng](#-features) • [🤝 Đóng góp](#-contributing)

</div>

---

## 📋 Mục lục

- [Giới thiệu](#-giới-thiệu)
- [Tính năng](#-features)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [Documentation](#-documentation)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Giới thiệu

**Taskoryx** là một hệ thống quản lý task hiện đại, được thiết kế để giúp các team làm việc hiệu quả hơn. Lấy cảm hứng từ Trello và Jira, Taskoryx kết hợp tính đơn giản của Kanban board với sức mạnh của project management.

### Tại sao chọn Taskoryx?

✅ **Mã nguồn mở** - Hoàn toàn miễn phí và có thể tùy chỉnh
✅ **Hiện đại** - Sử dụng các công nghệ mới nhất (Spring Boot 3, Java 17)
✅ **Linh hoạt** - Dễ dàng mở rộng và tích hợp
✅ **Hiệu năng cao** - Tối ưu cho cả small team và enterprise
✅ **Tiếng Việt** - Hỗ trợ đa ngôn ngữ, ưu tiên tiếng Việt

---

## ✨ Features

### 🎯 Phiên bản hiện tại (MVP)

#### Core Features
- ✅ **Quản lý User** - Đăng ký, đăng nhập, phân quyền
- ✅ **Quản lý Project** - Tạo workspace, quản lý members
- ✅ **Kanban Board** - Drag & drop tasks giữa các columns
- ✅ **Task Management** - Tạo, sửa, xóa, assign tasks
- ✅ **Deadline & Reminder** - Theo dõi timeline
- ✅ **Comments & Mentions** - Collaboration với @mentions
- ✅ **File Attachments** - Đính kèm files vào tasks
- ✅ **Labels & Tags** - Phân loại tasks
- ✅ **Search & Filter** - Tìm kiếm nhanh
- ✅ **Multi-language** - Tiếng Việt & English

#### Security
- ✅ JWT Authentication
- ✅ Role-based Access Control (OWNER, ADMIN, MEMBER, VIEWER)
- ✅ Secure password hashing (BCrypt)

#### Performance
- ✅ Database indexing
- ✅ Optimized queries
- ✅ Efficient pagination

### 🚀 Roadmap (Tính năng sắp tới)

#### Phase 2 - Enhanced Features
- ⏳ **Notifications** - Email & in-app notifications
- ⏳ **Activity Log** - Theo dõi mọi thay đổi
- ⏳ **Time Tracking** - Ghi lại thời gian làm việc
- ⏳ **Calendar View** - Xem tasks theo lịch
- ⏳ **List View** - Alternative view mode
- ⏳ **Google Calendar Integration**

#### Phase 3 - Advanced Features
- 📅 **Task Dependencies** - Quản lý phụ thuộc giữa tasks
- 📅 **Gantt Chart** - Timeline visualization
- 📅 **Automation** - Tự động hóa công việc lặp lại
- 📅 **Custom Dashboard** - Dashboard tùy chỉnh
- 📅 **Reports & Analytics** - Báo cáo chi tiết
- 📅 **Mobile App** - iOS & Android

#### Phase 4 - Integrations
- 📅 **REST API** - Public API
- 📅 **Webhooks** - Event-driven integrations
- 📅 **Slack Integration**
- 📅 **Microsoft Teams Integration**
- 📅 **GitHub Integration**

---

## 🛠️ Tech Stack

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
- **JJWT** - JWT implementation
- **PostgreSQL Driver** - Database connectivity

### Tools
- **Git** - Version control
- **Maven** - Dependency management
- **Docker** - Containerization (planned)
- **GitHub Actions** - CI/CD (planned)

---

## 🚀 Quick Start

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

## 📖 Documentation

### 📚 Available Documents

| Document | Description |
|----------|-------------|
| [DATABASE_DESIGN.md](DATABASE_DESIGN.md) | Chi tiết thiết kế database, entities, relationships |
| [ERD.md](ERD.md) | Entity Relationship Diagram với Mermaid |
| [SETUP_GUIDE.md](SETUP_GUIDE.md) | Hướng dẫn chi tiết setup và development |
| [CONFIG_GUIDE.md](CONFIG_GUIDE.md) | Configuration options và environment setup |
| [QUICK_START.md](QUICK_START.md) | Quick start guide |

### 📊 Database Schema

Hệ thống sử dụng 15 tables chính:

**Core Tables:**
- `users` - Người dùng
- `projects` - Dự án/Workspace
- `project_members` - Thành viên dự án
- `boards` - Bảng Kanban
- `columns` - Cột trạng thái
- `tasks` - Công việc

**Collaboration Tables:**
- `comments` - Bình luận
- `comment_mentions` - Mentions (@user)
- `attachments` - File đính kèm

**Organization Tables:**
- `labels` - Nhãn/tags
- `task_labels` - Task-Label relationship

**Advanced Tables:**
- `notifications` - Thông báo
- `activity_logs` - Lịch sử hoạt động
- `time_tracking` - Theo dõi thời gian
- `task_dependencies` - Phụ thuộc tasks

Xem chi tiết trong [DATABASE_DESIGN.md](DATABASE_DESIGN.md)

### 🔗 API Endpoints

```
Authentication:
  POST   /api/auth/register
  POST   /api/auth/login
  POST   /api/auth/refresh

Users:
  GET    /api/users
  GET    /api/users/{id}
  PUT    /api/users/{id}
  DELETE /api/users/{id}

Projects:
  GET    /api/projects
  POST   /api/projects
  GET    /api/projects/{id}
  PUT    /api/projects/{id}
  DELETE /api/projects/{id}

Tasks:
  GET    /api/projects/{projectId}/tasks
  POST   /api/projects/{projectId}/tasks
  GET    /api/tasks/{id}
  PUT    /api/tasks/{id}
  DELETE /api/tasks/{id}
  PUT    /api/tasks/{id}/move
```

Full API documentation: http://localhost:8080/api/swagger-ui.html

---

## 🏗️ Project Structure

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

## 🎯 Roadmap

### ✅ Phase 1: MVP (Hoàn thành)
- [x] Database design
- [x] Entity classes
- [x] Basic CRUD operations
- [x] Authentication & Authorization
- [x] Kanban board functionality
- [x] Task management

### 🚧 Phase 2: Enhanced Features (Đang phát triển)
- [ ] Notifications system
- [ ] Activity logging
- [ ] Time tracking
- [ ] Advanced search
- [ ] File upload to cloud
- [ ] Email integration

### 📅 Phase 3: Advanced Features (Q2 2025)
- [ ] Task dependencies
- [ ] Gantt chart
- [ ] Automation rules
- [ ] Custom dashboards
- [ ] Advanced analytics

### 📅 Phase 4: Integrations (Q3 2025)
- [ ] Public REST API
- [ ] Webhooks
- [ ] Third-party integrations
- [ ] Mobile app
- [ ] Desktop app

---

## 🤝 Contributing

Chúng tôi rất hoan nghênh mọi đóng góp! Đây là dự án mã nguồn mở.

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

- 🐛 Bug fixes
- ✨ New features
- 📝 Documentation
- 🧪 Testing
- 🌐 Translations
- 🎨 UI/UX improvements

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 Taskoryx Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

## 👥 Team

- **Project Lead**: [Your Name]
- **Backend Developer**: [Names]
- **Frontend Developer**: [Names]
- **Database Designer**: [Names]

---

## 📞 Contact & Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/taskoryx/issues)
- **Email**: support@taskoryx.com
- **Documentation**: [Wiki](https://github.com/yourusername/taskoryx/wiki)

---

## 🙏 Acknowledgments

- Spring Boot team for the amazing framework
- PostgreSQL community
- All contributors to this project

---

<div align="center">

**⭐ Nếu project này hữu ích, hãy cho chúng tôi một star! ⭐**

Made with ❤️ by Taskoryx Team

[⬆ Back to top](#-taskoryx---task-management-system)

</div>
