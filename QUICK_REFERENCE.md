# ⚡ Quick Reference - Taskoryx Backend

## 📁 Cấu trúc Files

```
BE/
├── 📄 README.md                    → Overview dự án
├── 📄 GETTING_STARTED.md           → Bắt đầu nhanh (ĐỌC ĐẦU TIÊN!)
├── 📄 SETUP_GUIDE.md               → Hướng dẫn setup chi tiết
├── 📄 DATABASE_DESIGN.md           → Thiết kế database
├── 📄 ERD.md                       → Entity diagram
├── 📄 ENTITY_GUIDE.md              → Chi tiết 15 entities
├── 📄 DEVELOPMENT_CHECKLIST.md     → Task checklist
├── 📄 COMPLETION_REPORT.md         → Báo cáo hoàn thành Phase 1
├── 📄 QUICK_REFERENCE.md           → File này
│
├── 🗄️ database-init.sql            → Tạo database
├── 🗄️ schema.sql                   → Tạo 15 tables
│
└── src/main/java/com/taskoryx/backend/
    ├── 📦 entity/                  → ✅ 15 entity classes
    ├── 📦 repository/              → ⏳ Chưa có
    ├── 📦 service/                 → ⏳ Chưa có
    ├── 📦 controller/              → ⏳ Chưa có
    ├── 📦 dto/                     → ⏳ Chưa có
    ├── 📦 security/                → ⏳ Chưa có
    ├── 📦 exception/               → ⏳ Chưa có
    └── 📦 config/                  → ⏳ Chưa có
```

---

## 🚀 Bắt đầu (3 bước)

### 1️⃣ Setup Database (5 phút)
```bash
# Mở PostgreSQL
psql -U postgres

# Tạo database
\i E:/DOAN/BE/database-init.sql

# Kết nối database
\c taskoryx_dev

# Tạo tables
\i E:/DOAN/BE/schema.sql

# Verify
\dt
```

### 2️⃣ Cấu hình Spring Boot
```yaml
# File: src/main/resources/application.yaml
spring:
  datasource:
    username: postgres
    password: YOUR_PASSWORD  # ← THAY ĐỔI
```

### 3️⃣ Chạy thử
```bash
./mvnw spring-boot:run
```

---

## 📚 Đọc tài liệu nào?

| Mục đích | File | Thời gian |
|----------|------|-----------|
| Bắt đầu ngay | GETTING_STARTED.md | 10 phút |
| Hiểu database | DATABASE_DESIGN.md | 30 phút |
| Hiểu entities | ENTITY_GUIDE.md | 20 phút |
| Setup chi tiết | SETUP_GUIDE.md | 30 phút |
| Xem ERD | ERD.md | 10 phút |
| Biết làm gì | DEVELOPMENT_CHECKLIST.md | 10 phút |

---

## 💻 15 Entity Classes

### Core (3)
1. **User** - Người dùng, authentication
2. **Project** - Workspace/dự án
3. **ProjectMember** - Members & roles (OWNER/ADMIN/MEMBER/VIEWER)

### Kanban (3)
4. **Board** - Bảng Kanban
5. **Column** - Cột trạng thái (TODO, IN PROGRESS, DONE)
6. **Task** - Công việc (với auto task numbering)

### Collaboration (3)
7. **Comment** - Bình luận (với nested replies)
8. **CommentMention** - @mentions
9. **Attachment** - File đính kèm (max 100MB)

### Organization (2)
10. **Label** - Tags/nhãn
11. **TaskLabel** - Task-Label mapping

### Advanced (4)
12. **Notification** - Thông báo (email & in-app)
13. **ActivityLog** - Audit trail (JSONB)
14. **TimeTracking** - Theo dõi thời gian
15. **TaskDependency** - Task phụ thuộc

---

## 🔑 Key Concepts

### Task Numbering
```
Project key: PROJ
Tasks: PROJ-1, PROJ-2, PROJ-3, ...
Auto-generated per project
```

### Roles
```
OWNER   → Full access, delete project
ADMIN   → Manage members & settings
MEMBER  → Create & edit tasks
VIEWER  → Read only
```

### Task Priority
```
URGENT → Đỏ, cao nhất
HIGH   → Cam
MEDIUM → Vàng (default)
LOW    → Xanh
```

### Task Position
```
Sử dụng DECIMAL để dễ reorder:
1.0, 2.0, 3.0
Insert giữa: 1.5, 1.75, 1.875
```

---

## 📝 Code Examples

### Tạo User
```java
User user = User.builder()
    .username("john_doe")
    .email("john@example.com")
    .passwordHash(passwordEncoder.encode("password"))
    .fullName("John Doe")
    .build();
```

### Tạo Project
```java
Project project = Project.builder()
    .name("My Project")
    .key("MYPROJ")
    .owner(user)
    .build();
```

### Tạo Task
```java
Task task = Task.builder()
    .project(project)
    .board(board)
    .column(column)
    .title("Implement feature X")
    .priority(Task.TaskPriority.HIGH)
    .reporter(user)
    .dueDate(LocalDate.now().plusDays(7))
    .build();

// Task key: MYPROJ-1, MYPROJ-2, ...
String taskKey = task.getTaskKey();
```

---

## 🎯 Roadmap

### ✅ Phase 1: Database & Entities (DONE)
- Database schema
- 15 Entity classes
- Documentation

### 🚧 Phase 2: Repository Layer (NEXT - 3 days)
- UserRepository
- ProjectRepository
- TaskRepository
- ... (15 repositories total)

### ⏳ Phase 3: Service Layer (5-7 days)
- UserService & AuthService
- ProjectService
- TaskService
- ... (8 services)

### ⏳ Phase 4: Security & JWT (3-4 days)
- JWT authentication
- SecurityConfig
- Role-based access

### ⏳ Phase 5: REST Controllers (5-7 days)
- AuthController (/auth/login, /auth/register)
- UserController
- ProjectController
- TaskController
- ...

---

## 🛠️ Commands Thường Dùng

### Maven
```bash
./mvnw clean install    # Build
./mvnw spring-boot:run  # Run
./mvnw test             # Test
```

### PostgreSQL
```bash
psql -U postgres -d taskoryx_dev  # Connect
\dt                                # List tables
\d users                           # Describe table
SELECT * FROM users;               # Query
\q                                 # Exit
```

### Git
```bash
git status
git add .
git commit -m "Complete Phase 1: Entity layer"
git push
```

---

## ⚠️ Common Issues

### 1. Lombok không hoạt động
**Fix**: Install Lombok plugin + Enable annotation processing

### 2. Database connection refused
**Fix**: Check PostgreSQL running + Verify credentials

### 3. Table already exists
**Fix**: Drop database hoặc đổi `ddl-auto` → `validate`

---

## 📊 Progress

| Phase | Status | Progress |
|-------|--------|----------|
| Database & Entities | ✅ | 100% |
| Repositories | ⏳ | 0% |
| Services | ⏳ | 0% |
| Security | ⏳ | 0% |
| Controllers | ⏳ | 0% |
| **Overall** | **🚧** | **25%** |

---

## 🎯 Next Action

**BẮT ĐẦU PHASE 2:**

1. Đọc: DEVELOPMENT_CHECKLIST.md → Phase 2
2. Tạo: UserRepository.java
3. Tạo: ProjectRepository.java
4. Tạo: TaskRepository.java
5. ... (tiếp tục 12 repositories còn lại)

**Template:**
```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

---

## 📞 Help

- ❓ Không biết bắt đầu? → GETTING_STARTED.md
- 🗄️ Database issues? → SETUP_GUIDE.md
- 💻 Entity questions? → ENTITY_GUIDE.md
- 📋 Task tracking? → DEVELOPMENT_CHECKLIST.md

---

**Quick Start**: `GETTING_STARTED.md`
**Full Guide**: `SETUP_GUIDE.md`
**Next Phase**: `DEVELOPMENT_CHECKLIST.md` → Phase 2

---

Last Updated: 2025-02-05
