# 📚 Taskoryx - Hướng dẫn Entity Classes

## ✅ Đã hoàn thành: 15 Entity Classes

### 📊 Tổng quan

Tất cả entity classes đã được tạo trong package: `com.taskoryx.backend.entity`

```
entity/
├── User.java                 ✅ Core entity - Người dùng
├── Project.java              ✅ Core entity - Dự án/Workspace
├── ProjectMember.java        ✅ Members & roles
├── Board.java                ✅ Kanban board
├── Column.java               ✅ Status columns
├── Task.java                 ✅ Công việc
├── Label.java                ✅ Tags/labels
├── TaskLabel.java            ✅ Task-Label relationship
├── Comment.java              ✅ Bình luận
├── CommentMention.java       ✅ @mentions
├── Attachment.java           ✅ File đính kèm
├── Notification.java         ✅ Thông báo
├── ActivityLog.java          ✅ Audit trail
├── TimeTracking.java         ✅ Time tracking
└── TaskDependency.java       ✅ Task dependencies
```

---

## 🎯 Entity Details

### 1️⃣ User.java (Core)

**Purpose**: Quản lý người dùng trong hệ thống

**Key Fields**:
- `username` - Tên đăng nhập (unique)
- `email` - Email (unique)
- `passwordHash` - Mật khẩu đã hash
- `fullName` - Họ và tên
- `timezone` - Múi giờ (default: Asia/Ho_Chi_Minh)
- `language` - Ngôn ngữ (vi/en)
- `emailVerified` - Email đã xác thực?
- `isActive` - Tài khoản còn hoạt động?

**Relationships**:
- `ownedProjects` - Projects mà user sở hữu
- `projectMemberships` - Projects mà user là member
- `assignedTasks` - Tasks được assign cho user
- `reportedTasks` - Tasks do user tạo
- `notifications` - Thông báo của user
- `activityLogs` - Lịch sử hoạt động
- `timeTrackings` - Thời gian làm việc

**Validations**:
- Username: chỉ chứa chữ, số, _, -
- Email: phải hợp lệ
- Password: required

---

### 2️⃣ Project.java (Core)

**Purpose**: Workspace/dự án chứa các tasks

**Key Fields**:
- `name` - Tên dự án
- `key` - Project key (VD: TASK, PROJ) - unique
- `owner` - Chủ sở hữu (User)
- `description` - Mô tả
- `icon` - Icon emoji
- `color` - Màu sắc (hex)
- `isPublic` - Công khai?
- `isArchived` - Đã archive?

**Relationships**:
- `members` - Thành viên (qua ProjectMember)
- `boards` - Các bảng Kanban
- `tasks` - Tất cả tasks trong project
- `labels` - Labels của project
- `activityLogs` - Lịch sử hoạt động

**Validations**:
- Key: 2-10 ký tự, chữ in hoa
- Color: phải là hex color (#RRGGBB)

---

### 3️⃣ ProjectMember.java

**Purpose**: Quản lý members và roles trong project

**Key Fields**:
- `project` - Dự án
- `user` - Người dùng
- `role` - Vai trò (OWNER, ADMIN, MEMBER, VIEWER)
- `joinedAt` - Ngày tham gia

**Roles**:
- `OWNER` - Full access, có thể xóa project
- `ADMIN` - Quản lý members & settings
- `MEMBER` - Tạo và edit tasks
- `VIEWER` - Chỉ xem, không edit

**Unique Constraint**: (project, user) - Một user chỉ có 1 role/project

---

### 4️⃣ Board.java (Kanban)

**Purpose**: Bảng Kanban trong project

**Key Fields**:
- `project` - Dự án chứa board
- `name` - Tên board
- `description` - Mô tả
- `position` - Thứ tự hiển thị
- `isDefault` - Bảng mặc định?

**Relationships**:
- `columns` - Các cột trong board
- `tasks` - Tasks hiển thị trên board

---

### 5️⃣ Column.java (Status)

**Purpose**: Cột trạng thái trong Kanban (TODO, IN PROGRESS, DONE)

**Key Fields**:
- `board` - Board chứa column
- `name` - Tên cột
- `position` - Thứ tự hiển thị
- `color` - Màu sắc
- `isCompleted` - Cột hoàn thành? (trigger auto completed_at)
- `taskLimit` - WIP limit (giới hạn số task)

**Relationships**:
- `tasks` - Tasks trong cột

**Helper Methods**:
- `hasReachedLimit()` - Check WIP limit

---

### 6️⃣ Task.java (Core)

**Purpose**: Công việc/task cần làm

**Key Fields**:
- `project` - Dự án
- `board` - Board hiển thị
- `column` - Cột hiện tại
- `taskNumber` - Số thứ tự (auto-increment per project)
- `title` - Tiêu đề
- `description` - Mô tả chi tiết
- `priority` - Độ ưu tiên (LOW, MEDIUM, HIGH, URGENT)
- `position` - Vị trí trong cột (decimal)
- `assignee` - Người được assign
- `reporter` - Người tạo task
- `startDate` - Ngày bắt đầu
- `dueDate` - Deadline
- `estimatedHours` - Thời gian ước tính
- `actualHours` - Thời gian thực tế
- `completedAt` - Ngày hoàn thành

**Relationships**:
- `comments` - Bình luận
- `attachments` - Files đính kèm
- `taskLabels` - Labels
- `timeEntries` - Time tracking entries
- `dependencies` - Tasks phụ thuộc
- `dependents` - Tasks bị block bởi task này

**Helper Methods**:
- `getTaskKey()` - Trả về PROJ-123
- `isOverdue()` - Kiểm tra quá hạn
- `isCompleted()` - Kiểm tra đã hoàn thành

**Unique Constraint**: (project, taskNumber)

---

### 7️⃣ Label.java

**Purpose**: Tags/nhãn để phân loại tasks

**Key Fields**:
- `project` - Dự án
- `name` - Tên label
- `color` - Màu sắc (hex)
- `description` - Mô tả

**Relationships**:
- `taskLabels` - Tasks có label này

**Unique Constraint**: (project, name)

---

### 8️⃣ TaskLabel.java

**Purpose**: Many-to-many relationship giữa Task và Label

**Key Fields**:
- `task` - Task
- `label` - Label
- `createdAt` - Ngày gắn label

**Unique Constraint**: (task, label)

---

### 9️⃣ Comment.java

**Purpose**: Bình luận trên task

**Key Fields**:
- `task` - Task
- `user` - Người comment
- `content` - Nội dung
- `parent` - Comment cha (cho reply)
- `isEdited` - Đã chỉnh sửa?

**Relationships**:
- `replies` - Các reply cho comment này
- `mentions` - Users được mention (@username)

**Helper Methods**:
- `isReply()` - Kiểm tra có phải reply không

---

### 🔟 CommentMention.java

**Purpose**: Tag người dùng trong comment (@username)

**Key Fields**:
- `comment` - Comment
- `user` - User được mention
- `createdAt` - Ngày mention

**Unique Constraint**: (comment, user)

---

### 1️⃣1️⃣ Attachment.java

**Purpose**: File đính kèm trong task

**Key Fields**:
- `task` - Task
- `uploadedBy` - Người upload
- `fileName` - Tên file
- `fileSize` - Kích thước (bytes)
- `fileType` - MIME type
- `fileUrl` - URL truy cập
- `storagePath` - Đường dẫn lưu trữ

**Helper Methods**:
- `getFormattedFileSize()` - Format: "2.5 MB"
- `isImage()` - Kiểm tra file ảnh

**Constraint**: fileSize <= 100MB

---

### 1️⃣2️⃣ Notification.java

**Purpose**: Thông báo cho người dùng

**Key Fields**:
- `user` - Người nhận
- `type` - Loại thông báo (TASK_ASSIGNED, MENTION, etc.)
- `title` - Tiêu đề
- `message` - Nội dung
- `relatedType` - Loại đối tượng liên quan (TASK, COMMENT, PROJECT)
- `relatedId` - ID đối tượng
- `isRead` - Đã đọc?
- `sentEmail` - Đã gửi email?
- `readAt` - Thời gian đọc

**Notification Types**:
- `TASK_ASSIGNED` - Task được assign
- `TASK_UPDATED` - Task được update
- `TASK_COMMENTED` - Có comment mới
- `MENTION` - Được mention
- `DUE_DATE_REMINDER` - Nhắc deadline
- `PROJECT_INVITE` - Mời vào project

**Helper Methods**:
- `markAsRead()` - Đánh dấu đã đọc
- `isUnread()` - Kiểm tra chưa đọc

---

### 1️⃣3️⃣ ActivityLog.java

**Purpose**: Audit trail - ghi lại mọi thay đổi

**Key Fields**:
- `user` - Người thực hiện
- `project` - Dự án
- `entityType` - Loại đối tượng (TASK, COMMENT, etc.)
- `entityId` - ID đối tượng
- `action` - Hành động (CREATE, UPDATE, DELETE, etc.)
- `oldValue` - Giá trị cũ (JSON)
- `newValue` - Giá trị mới (JSON)
- `ipAddress` - IP address
- `userAgent` - User agent
- `createdAt` - Thời gian

**Actions**:
- `CREATE` - Tạo mới
- `UPDATE` - Cập nhật
- `DELETE` - Xóa
- `MOVE` - Di chuyển
- `ASSIGN` - Assign
- `COMPLETE` - Hoàn thành

**Helper Methods**:
- `getActionDescription()` - Format: "John Doe updated task"

---

### 1️⃣4️⃣ TimeTracking.java

**Purpose**: Theo dõi thời gian làm việc

**Key Fields**:
- `task` - Task
- `user` - Người thực hiện
- `description` - Mô tả công việc
- `hours` - Số giờ (decimal)
- `workDate` - Ngày làm việc
- `createdAt` - Ngày ghi

**Helper Methods**:
- `getFormattedHours()` - Format: "2h 30m"
- `isToday()` - Kiểm tra hôm nay
- `isPast()` - Kiểm tra quá khứ

**Constraint**: hours > 0 AND hours <= 24

---

### 1️⃣5️⃣ TaskDependency.java

**Purpose**: Quản lý phụ thuộc giữa các tasks

**Key Fields**:
- `task` - Task hiện tại
- `dependsOnTask` - Task phụ thuộc
- `type` - Loại phụ thuộc (BLOCKS, RELATES_TO)
- `createdAt` - Ngày tạo

**Dependency Types**:
- `BLOCKS` - Task này block task khác
- `RELATES_TO` - Task liên quan

**Helper Methods**:
- `isBlocking()` - Kiểm tra loại BLOCKS
- `getDescription()` - Format: "PROJ-1 blocks PROJ-2"

**Unique Constraint**: (task, dependsOnTask)
**Constraint**: task != dependsOnTask (không self-dependency)

---

## 🔗 Relationships Map

### User relationships:
```
User
├─→ ownedProjects (OneToMany)
├─→ projectMemberships (OneToMany)
├─→ assignedTasks (OneToMany)
├─→ reportedTasks (OneToMany)
├─→ notifications (OneToMany)
├─→ activityLogs (OneToMany)
└─→ timeTrackings (OneToMany)
```

### Project relationships:
```
Project
├─→ members (OneToMany)
├─→ boards (OneToMany)
├─→ tasks (OneToMany)
├─→ labels (OneToMany)
└─→ activityLogs (OneToMany)
```

### Task relationships:
```
Task
├─→ comments (OneToMany)
├─→ attachments (OneToMany)
├─→ taskLabels (OneToMany)
├─→ timeEntries (OneToMany)
├─→ dependencies (OneToMany)
└─→ dependents (OneToMany)
```

---

## 📝 Annotations Được Sử Dụng

### JPA Annotations:
- `@Entity` - Đánh dấu class là entity
- `@Table` - Cấu hình table name và indexes
- `@Id` - Primary key
- `@GeneratedValue` - Auto-generate ID (UUID)
- `@Column` - Cấu hình column
- `@ManyToOne` - Many-to-one relationship
- `@OneToMany` - One-to-many relationship
- `@JoinColumn` - Foreign key column
- `@Enumerated` - Enum mapping
- `@Transient` - Không lưu vào DB

### Lombok Annotations:
- `@Getter` / `@Setter` - Auto generate getters/setters
- `@NoArgsConstructor` - Constructor không tham số
- `@AllArgsConstructor` - Constructor đầy đủ tham số
- `@Builder` - Builder pattern
- `@ToString` - Auto generate toString()

### Hibernate Annotations:
- `@CreationTimestamp` - Auto set created time
- `@UpdateTimestamp` - Auto update modified time

### Validation Annotations:
- `@NotBlank` - Không được rỗng
- `@Email` - Phải là email hợp lệ
- `@Pattern` - Regex validation

---

## 🎯 Best Practices Được Áp Dụng

### 1. Naming Conventions
- Entity class: PascalCase (User, Project, TaskLabel)
- Field names: camelCase (fullName, createdAt)
- Table names: snake_case (users, task_labels)

### 2. Relationships
- Sử dụng `FetchType.LAZY` cho performance
- Cascade operations hợp lý
- Orphan removal khi cần

### 3. Equals & HashCode
- Sử dụng ID cho equals/hashCode
- Tránh infinite loop với circular references

### 4. Helper Methods
- `@Transient` methods cho business logic
- Không lưu vào database

### 5. Validation
- Bean Validation ở entity level
- Database constraints ở schema level

---

## 🚀 Sử dụng Entity Classes

### Tạo User mới:
```java
User user = User.builder()
    .username("john_doe")
    .email("john@example.com")
    .passwordHash(passwordEncoder.encode("password"))
    .fullName("John Doe")
    .language("vi")
    .timezone("Asia/Ho_Chi_Minh")
    .build();
```

### Tạo Project:
```java
Project project = Project.builder()
    .name("My Project")
    .key("MYPROJ")
    .owner(user)
    .color("#1976d2")
    .build();
```

### Tạo Task:
```java
Task task = Task.builder()
    .project(project)
    .board(board)
    .column(column)
    .title("Implement login feature")
    .description("Add JWT authentication")
    .priority(Task.TaskPriority.HIGH)
    .reporter(user)
    .dueDate(LocalDate.now().plusDays(7))
    .estimatedHours(BigDecimal.valueOf(8))
    .build();
```

### Tạo Comment với Mention:
```java
Comment comment = Comment.builder()
    .task(task)
    .user(currentUser)
    .content("@john_doe Please review this")
    .build();

// Extract mentions và tạo CommentMention
CommentMention mention = CommentMention.builder()
    .comment(comment)
    .user(mentionedUser)
    .build();
```

### Log Activity:
```java
ActivityLog log = ActivityLog.builder()
    .user(currentUser)
    .project(project)
    .entityType(ActivityLog.EntityType.TASK)
    .entityId(task.getId())
    .action(ActivityLog.Action.CREATE)
    .newValue("{\"title\":\"New task\"}")
    .ipAddress(request.getRemoteAddr())
    .build();
```

---

## ✅ Checklist Setup

### Đã hoàn thành:
- [x] 15 Entity classes
- [x] Relationships configured
- [x] Validation annotations
- [x] Helper methods
- [x] Lombok annotations
- [x] JPA annotations
- [x] Application class moved to correct package
- [x] Test class created
- [x] Old package removed

### Tiếp theo:
- [ ] Create Repository interfaces
- [ ] Create Service classes
- [ ] Setup Security & JWT
- [ ] Create Controllers
- [ ] Write tests

---

## 📚 Tài liệu liên quan

- [DATABASE_DESIGN.md](DATABASE_DESIGN.md) - Chi tiết database schema
- [ERD.md](ERD.md) - Entity Relationship Diagram
- [SETUP_GUIDE.md](SETUP_GUIDE.md) - Hướng dẫn setup
- [DEVELOPMENT_CHECKLIST.md](DEVELOPMENT_CHECKLIST.md) - Task list

---

**Version**: 1.0.0
**Last Updated**: 2025-02-05
**Status**: ✅ All entities complete
