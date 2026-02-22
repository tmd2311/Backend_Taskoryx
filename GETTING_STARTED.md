# 🚀 Bắt đầu nhanh với Taskoryx Backend

## 📌 Bạn ở đâu trong hành trình?

Hiện tại bạn đã có:
- ✅ Database schema hoàn chỉnh (15 tables)
- ✅ 11 Entity classes đầy đủ
- ✅ Documentation chi tiết
- ✅ Project structure

---

## ⚡ 5 phút setup database

### Bước 1: Cài PostgreSQL
```bash
# Windows: Download từ postgresql.org
# Mac: brew install postgresql
# Linux: sudo apt-get install postgresql
```

### Bước 2: Tạo database
```bash
# Mở PostgreSQL
psql -U postgres

# Trong psql, chạy:
\i E:/DOAN/BE/database-init.sql
\c taskoryx_dev
\i E:/DOAN/BE/schema.sql

# Verify
\dt
SELECT * FROM users;
```

✅ **Kết quả**: 15 tables, 1 admin user, 1 sample project

### Bước 3: Cấu hình Spring Boot
```yaml
# File: src/main/resources/application.yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskoryx_dev
    username: postgres
    password: your_password  # <-- THAY ĐỔI NÀY
```

### Bước 4: Chạy thử
```bash
./mvnw spring-boot:run
```

✅ **Kết quả**: Server chạy ở http://localhost:8080

---

## 📚 Đọc tài liệu nào đầu tiên?

Tùy theo mục đích:

### 🎯 Muốn hiểu database design?
→ Đọc **DATABASE_DESIGN.md**
- Chi tiết 15 tables
- Mối quan hệ giữa các entity
- Business rules

### 🏗️ Muốn bắt đầu code ngay?
→ Đọc **SETUP_GUIDE.md**
- Setup từng bước
- Code examples
- Troubleshooting

### 📋 Muốn biết làm gì tiếp theo?
→ Đọc **DEVELOPMENT_CHECKLIST.md**
- Task breakdown chi tiết
- 10 phases phát triển
- Progress tracking

### 👀 Muốn overview tổng thể?
→ Đọc **README.md**
- Tổng quan dự án
- Features & roadmap
- Tech stack

### 🗺️ Muốn xem cấu trúc database?
→ Đọc **ERD.md**
- Entity Relationship Diagram
- Visual representation
- Design decisions

---

## 🎯 Roadmap 6 tuần (MVP)

```
Week 1: Repositories & Basic Services
├─ Day 1-2: Create all Repository interfaces
├─ Day 3-4: UserService & AuthService
└─ Day 5-7: ProjectService & TaskService

Week 2: Security & Authentication
├─ Day 1-3: JWT setup & SecurityConfig
├─ Day 4-5: AuthController & login/register
└─ Day 6-7: Testing authentication

Week 3: Core API Controllers
├─ Day 1-2: UserController & ProjectController
├─ Day 3-4: TaskController & BoardController
└─ Day 5-7: CommentController & testing

Week 4: DTOs & Validation
├─ Day 1-3: Request/Response DTOs
├─ Day 4-5: Validation rules
└─ Day 6-7: Exception handling

Week 5: Advanced Features
├─ Day 1-3: File upload
├─ Day 4-5: Notifications
└─ Day 6-7: Activity logging

Week 6: Testing & Documentation
├─ Day 1-3: Integration tests
├─ Day 4-5: API documentation (Swagger)
└─ Day 6-7: Final testing & deployment prep
```

---

## 💻 Lệnh thường dùng

### Maven
```bash
# Build project
./mvnw clean install

# Run application
./mvnw spring-boot:run

# Run tests
./mvnw test

# Skip tests
./mvnw clean install -DskipTests

# Run specific test
./mvnw test -Dtest=UserServiceTest
```

### PostgreSQL
```bash
# Connect to database
psql -U postgres -d taskoryx_dev

# List tables
\dt

# Describe table
\d users

# Run SQL file
\i schema.sql

# Show all databases
\l

# Exit
\q
```

### Git
```bash
# Clone repository (nếu có remote)
git clone <url>

# Create feature branch
git checkout -b feature/user-service

# Commit changes
git add .
git commit -m "Add UserService with CRUD operations"

# Push to remote
git push origin feature/user-service
```

---

## 📝 Checklist: Bắt đầu code

### Setup môi trường
- [ ] Java 17 installed
- [ ] PostgreSQL installed
- [ ] IDE setup (IntelliJ/Eclipse/VS Code)
- [ ] Maven working
- [ ] Database created
- [ ] Tables created
- [ ] Sample data inserted

### Code structure
- [ ] Package structure created
- [ ] Entity classes reviewed
- [ ] Application.yaml configured
- [ ] Lombok plugin installed

### First implementation
- [ ] Create UserRepository
- [ ] Create UserService
- [ ] Write UserService test
- [ ] Create AuthController
- [ ] Test with Postman/curl

---

## 🎓 Entity Classes Guide

### Quan hệ giữa các entity

```
User
├─ owns → Project
├─ member of → ProjectMember
├─ assigned to → Task
└─ creates → Comment

Project
├─ has → Board
├─ has → Task
├─ has → Label
└─ has → ProjectMember

Board
├─ has → Column
└─ displays → Task

Column
└─ contains → Task

Task
├─ has → Comment
├─ has → Attachment
└─ has → TaskLabel
```

### Sử dụng Entity classes

```java
// Tạo User mới
User user = User.builder()
    .username("john_doe")
    .email("john@example.com")
    .passwordHash(passwordEncoder.encode("password"))
    .fullName("John Doe")
    .build();

// Tạo Project
Project project = Project.builder()
    .name("My Project")
    .key("MYPROJ")
    .owner(user)
    .color("#1976d2")
    .build();

// Tạo Task
Task task = Task.builder()
    .project(project)
    .board(board)
    .column(column)
    .title("Implement login feature")
    .priority(Task.TaskPriority.HIGH)
    .reporter(user)
    .build();
```

---

## 🔥 Tips quan trọng

### 1. Database Migrations
**Hiện tại**: Đang dùng `schema.sql` để tạo tables
**Production**: Nên dùng Flyway hoặc Liquibase

### 2. Security
```yaml
# KHÔNG commit password vào git
# Dùng environment variables
DB_PASSWORD=${DB_PASSWORD:your_default_password}
JWT_SECRET=${JWT_SECRET:your_secret_key}
```

### 3. Testing
```java
// Viết test cho mỗi service method
@Test
void shouldCreateUser() {
    // Given
    User user = createTestUser();

    // When
    User saved = userService.create(user);

    // Then
    assertNotNull(saved.getId());
}
```

### 4. API Design
```
✅ Good:
POST   /api/projects/{id}/tasks
GET    /api/tasks/{id}
PUT    /api/tasks/{id}

❌ Bad:
POST   /api/createTask
GET    /api/getTaskById?id=123
```

---

## 🐛 Lỗi thường gặp

### 1. Lombok không hoạt động
```
Error: Cannot resolve symbol 'builder'
```
**Fix**:
- Install Lombok plugin trong IDE
- Enable annotation processing
- Rebuild project

### 2. Database connection refused
```
Error: Connection refused
```
**Fix**:
- Check PostgreSQL đã chạy chưa
- Verify port 5432
- Check username/password

### 3. Table already exists
```
Error: Table 'users' already exists
```
**Fix**:
- Đổi `spring.jpa.hibernate.ddl-auto` → `validate`
- Hoặc drop database và tạo lại

### 4. Foreign key constraint
```
Error: violates foreign key constraint
```
**Fix**:
- Insert parent entity trước
- Hoặc set child entity's FK = null

---

## 📞 Cần giúp đỡ?

### Tài liệu có sẵn
1. **SETUP_GUIDE.md** - Setup chi tiết
2. **DATABASE_DESIGN.md** - Database design
3. **DEVELOPMENT_CHECKLIST.md** - Task list
4. **SUMMARY.md** - Tổng quan file đã tạo

### Online Resources
- Spring Boot Docs: https://spring.io/projects/spring-boot
- PostgreSQL Docs: https://www.postgresql.org/docs/
- Stack Overflow: Tag `spring-boot`

### Debug Tips
1. Đọc error message kỹ
2. Google full error message
3. Check logs trong console
4. Use debugger trong IDE
5. Print SQL queries (`show-sql: true`)

---

## ✅ Ready to code?

Bạn đã sẵn sàng! Bắt đầu với:

```bash
# 1. Tạo UserRepository
# File: src/main/java/com/taskoryx/backend/repository/UserRepository.java

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
}

# 2. Tạo UserService
# File: src/main/java/com/taskoryx/backend/service/UserService.java

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User create(User user) {
        // TODO: Implement
    }
}

# 3. Test it!
```

---

**Good luck! 🚀**

Bắt đầu với Repository layer, sau đó Services, rồi Controllers.

Nhớ check **DEVELOPMENT_CHECKLIST.md** để track progress!
