# 🚀 Taskoryx Backend - Hướng dẫn Setup

## 📚 Mục lục

1. [Giới thiệu](#giới-thiệu)
2. [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
3. [Cài đặt Database](#cài-đặt-database)
4. [Cấu hình Spring Boot](#cấu-hình-spring-boot)
5. [Chạy ứng dụng](#chạy-ứng-dụng)
6. [Cấu trúc dự án](#cấu-trúc-dự-án)
7. [Entity Classes](#entity-classes)
8. [Repositories](#repositories)
9. [API Documentation](#api-documentation)
10. [Testing](#testing)

---

## 🎯 Giới thiệu

Taskoryx là hệ thống quản lý task (Task Management System) giống như Trello/Jira, được xây dựng bằng:

- **Backend**: Spring Boot 3.2.1, Java 17
- **Database**: PostgreSQL 14+
- **ORM**: Spring Data JPA (Hibernate)
- **Security**: Spring Security + JWT
- **Documentation**: Swagger/OpenAPI

---

## 💻 Yêu cầu hệ thống

### Phần mềm cần cài đặt

- **Java**: JDK 17 hoặc cao hơn
- **Maven**: 3.8+ (hoặc dùng Maven wrapper có sẵn)
- **PostgreSQL**: 14+
- **IDE**: IntelliJ IDEA / Eclipse / VS Code
- **Git**: Để clone repository

### Kiểm tra phiên bản

```bash
# Java
java -version
# Output: java version "17.x.x"

# Maven
mvn -version
# Output: Apache Maven 3.8.x

# PostgreSQL
psql --version
# Output: psql (PostgreSQL) 14.x
```

---

## 🗄️ Cài đặt Database

### Bước 1: Tạo Database

```bash
# Kết nối PostgreSQL
psql -U postgres

# Hoặc trên Windows với pgAdmin
```

### Bước 2: Chạy script khởi tạo

```sql
-- File: database-init.sql
-- Tạo database và user

\i E:/DOAN/BE/database-init.sql
```

Hoặc copy-paste nội dung file `database-init.sql` vào psql.

### Bước 3: Tạo schema (tables)

```sql
-- Kết nối vào database vừa tạo
\c taskoryx_dev

-- Chạy script tạo tables
\i E:/DOAN/BE/schema.sql
```

### Bước 4: Verify database

```sql
-- Kiểm tra các tables đã tạo
\dt

-- Kiểm tra dữ liệu mẫu
SELECT * FROM users;
SELECT * FROM projects;
```

**Kết quả mong đợi:**
- 15 tables được tạo
- 1 user admin
- 1 sample project
- 1 default board với 4 columns

---

## ⚙️ Cấu hình Spring Boot

### Bước 1: Cấu hình Database Connection

Mở file `src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: taskoryx-backend

  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/taskoryx_dev
    username: postgres  # Hoặc taskoryx_user
    password: your_password_here  # Thay đổi password
    driver-class-name: org.postgresql.Driver

  # JPA/Hibernate Configuration
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # validate, update, create-drop
    show-sql: true  # Hiển thị SQL queries
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

  # Security
  security:
    jwt:
      secret-key: your-secret-key-change-this-in-production
      expiration: 86400000  # 24 hours in milliseconds

# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /api

# Logging
logging:
  level:
    com.taskoryx: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
```

### Bước 2: Tạo file .env (Optional)

Tạo file `.env` ở root directory:

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/taskoryx_dev
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your-jwt-secret-key-min-256-bits
JWT_EXPIRATION=86400000

# Server
SERVER_PORT=8080
```

---

## 🏃 Chạy ứng dụng

### Sử dụng Maven

```bash
# Clean và build project
./mvnw clean install

# Chạy application
./mvnw spring-boot:run

# Hoặc trên Windows
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

### Sử dụng IDE

1. **IntelliJ IDEA**:
   - Open project → Wait for Maven import
   - Find `TaskoryxApplication.java`
   - Right click → Run 'TaskoryxApplication'

2. **Eclipse**:
   - Import → Maven → Existing Maven Project
   - Right click on project → Run As → Spring Boot App

### Verify Application

Sau khi start thành công, kiểm tra:

```bash
# Health check
curl http://localhost:8080/api/actuator/health

# Swagger UI
# Mở browser: http://localhost:8080/api/swagger-ui.html
```

**Console output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.1)

2025-02-05 INFO Tomcat started on port(s): 8080 (http)
2025-02-05 INFO Started TaskoryxApplication in 5.432 seconds
```

---

## 📁 Cấu trúc dự án

```
BE/
├── src/
│   ├── main/
│   │   ├── java/com/taskoryx/backend/
│   │   │   ├── entity/              # Entity classes (JPA)
│   │   │   │   ├── User.java
│   │   │   │   ├── Project.java
│   │   │   │   ├── Task.java
│   │   │   │   ├── Board.java
│   │   │   │   ├── Column.java
│   │   │   │   ├── Comment.java
│   │   │   │   ├── Label.java
│   │   │   │   └── ...
│   │   │   │
│   │   │   ├── repository/          # Spring Data JPA Repositories
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── ProjectRepository.java
│   │   │   │   └── ...
│   │   │   │
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   │
│   │   │   ├── service/             # Business Logic Layer
│   │   │   │   ├── UserService.java
│   │   │   │   ├── ProjectService.java
│   │   │   │   └── ...
│   │   │   │
│   │   │   ├── controller/          # REST API Controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── ProjectController.java
│   │   │   │   └── ...
│   │   │   │
│   │   │   ├── security/            # Security Configuration
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── SecurityConfig.java
│   │   │   │
│   │   │   ├── exception/           # Exception Handling
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── CustomExceptions.java
│   │   │   │
│   │   │   └── config/              # Configuration Classes
│   │   │       ├── SwaggerConfig.java
│   │   │       └── CorsConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       └── application-prod.yaml
│   │
│   └── test/                        # Unit & Integration Tests
│       └── java/com/taskoryx/backend/
│
├── database-init.sql                # Database initialization
├── schema.sql                       # Database schema
├── DATABASE_DESIGN.md               # Database design document
├── ERD.md                           # Entity Relationship Diagram
├── pom.xml                          # Maven dependencies
└── README.md
```

---

## 🎨 Entity Classes

### 1. Core Entities

#### User.java
```java
@Entity
@Table(name = "users")
public class User {
    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    // ... other fields

    // Relationships
    private Set<Project> ownedProjects;
    private Set<Task> assignedTasks;
}
```

**Chức năng chính:**
- Quản lý thông tin người dùng
- Authentication & Authorization
- Relationships với Projects và Tasks

#### Project.java
```java
@Entity
@Table(name = "projects")
public class Project {
    private UUID id;
    private String name;
    private String key;  // VD: TASK, PROJ
    private User owner;

    // Relationships
    private Set<Board> boards;
    private Set<Task> tasks;
    private Set<ProjectMember> members;
}
```

**Chức năng chính:**
- Quản lý workspace/dự án
- Project key để tạo task number (PROJ-123)
- Quản lý members và permissions

#### Task.java
```java
@Entity
@Table(name = "tasks")
public class Task {
    private UUID id;
    private Integer taskNumber;
    private String title;
    private TaskPriority priority;
    private User assignee;
    private LocalDate dueDate;

    // Helper methods
    public String getTaskKey() {
        return project.getKey() + "-" + taskNumber;
    }

    public boolean isOverdue() {
        return dueDate != null && LocalDate.now().isAfter(dueDate);
    }
}
```

**Chức năng chính:**
- Quản lý công việc
- Auto-generate task number
- Tracking progress, deadline, time

### 2. Kanban Entities

- **Board.java**: Bảng Kanban
- **Column.java**: Cột trạng thái (TODO, IN PROGRESS, DONE)
- Hỗ trợ drag & drop với position (decimal)

### 3. Collaboration Entities

- **Comment.java**: Bình luận trên task
- **CommentMention.java**: Tag người dùng (@username)
- **Attachment.java**: File đính kèm

### 4. Organization Entities

- **Label.java**: Nhãn/tag cho task
- **TaskLabel.java**: Many-to-Many relationship
- **ProjectMember.java**: Quản lý members và roles

---

## 🔍 Repositories

### Tạo Repository Interface

```java
package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Query methods
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // Custom query
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findAllActiveUsers();
}
```

### Repositories cần tạo

```java
// Core
UserRepository.java
ProjectRepository.java
TaskRepository.java

// Kanban
BoardRepository.java
ColumnRepository.java

// Collaboration
CommentRepository.java
AttachmentRepository.java

// Organization
LabelRepository.java
ProjectMemberRepository.java
```

---

## 📖 API Documentation

### Swagger UI

Sau khi start application, truy cập:

```
http://localhost:8080/api/swagger-ui.html
```

### API Endpoints (Ví dụ)

#### Authentication
```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh
```

#### Users
```
GET    /api/users
GET    /api/users/{id}
PUT    /api/users/{id}
DELETE /api/users/{id}
```

#### Projects
```
GET    /api/projects
POST   /api/projects
GET    /api/projects/{id}
PUT    /api/projects/{id}
DELETE /api/projects/{id}

# Members
GET    /api/projects/{id}/members
POST   /api/projects/{id}/members
DELETE /api/projects/{id}/members/{userId}
```

#### Tasks
```
GET    /api/projects/{projectId}/tasks
POST   /api/projects/{projectId}/tasks
GET    /api/tasks/{id}
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}

# Move task
PUT    /api/tasks/{id}/move
```

---

## 🧪 Testing

### Unit Tests

```java
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void shouldCreateUser() {
        // Arrange
        User user = User.builder()
            .username("testuser")
            .email("test@example.com")
            .build();

        // Act
        User saved = userService.create(user);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("testuser", saved.getUsername());
    }
}
```

### Integration Tests

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=UserServiceTest
```

---

## 🔧 Troubleshooting

### Lỗi thường gặp

#### 1. Connection refused to database

```
Caused by: org.postgresql.util.PSQLException: Connection refused
```

**Giải pháp:**
- Kiểm tra PostgreSQL đã start chưa
- Verify port 5432
- Check username/password trong `application.yaml`

#### 2. Table already exists

```
org.hibernate.tool.schema.spi.SchemaManagementException: Table 'users' already exists
```

**Giải pháp:**
- Đổi `spring.jpa.hibernate.ddl-auto` thành `validate`
- Hoặc drop database và tạo lại

#### 3. Lombok not working

```
Cannot resolve symbol 'builder'
```

**Giải pháp:**
- Install Lombok plugin trong IDE
- Enable annotation processing
- Rebuild project

---

## 📌 Next Steps

1. **Implement Services**: Tạo business logic layer
2. **Create Controllers**: REST API endpoints
3. **Add Security**: JWT authentication
4. **Write Tests**: Unit và integration tests
5. **Add Validation**: Bean validation
6. **Error Handling**: Global exception handler
7. **Logging**: Add logging với SLF4J
8. **Caching**: Redis cho performance
9. **File Upload**: AWS S3 hoặc local storage
10. **Email Service**: Gửi notifications

---

## 📚 Tài liệu tham khảo

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Lombok](https://projectlombok.org/)
- [Swagger/OpenAPI](https://swagger.io/)

---

## 🤝 Contributing

Đây là dự án mã nguồn mở. Mọi đóng góp đều được chào đón!

### Quy trình contribute:

1. Fork repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

### Code style:

- Follow Java conventions
- Use meaningful variable names
- Add javadoc comments
- Write unit tests

---

## 📝 License

MIT License - Dự án mã nguồn mở

---

## 📞 Support

Nếu gặp vấn đề, vui lòng:

1. Kiểm tra [Troubleshooting](#troubleshooting)
2. Xem [Documentation](#api-documentation)
3. Tạo issue trên GitHub
4. Liên hệ team

---

**Happy Coding! 🚀**
