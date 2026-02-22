# Taskoryx - Database Design Document

## 📊 Tổng quan

Tài liệu này mô tả chi tiết thiết kế cơ sở dữ liệu cho hệ thống quản lý task Taskoryx, bao gồm các entity, mối quan hệ và quy tắc nghiệp vụ.

**DBMS**: PostgreSQL 14+
**ORM**: Spring Data JPA (Hibernate)
**Migration**: Liquibase/Flyway (optional)

---

## 🗂️ Entity Relationship Diagram (ERD)

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Users     │────────<│  Project    │────────<│   Board     │
│             │         │  Members    │         │             │
└─────────────┘         └─────────────┘         └─────────────┘
      │                       │                        │
      │                       │                        │
      ▼                       ▼                        ▼
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│  Projects   │         │   Tasks     │         │  Columns    │
│             │         │             │         │  (Status)   │
└─────────────┘         └─────────────┘         └─────────────┘
                              │
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
  ┌───────────┐         ┌───────────┐       ┌─────────────┐
  │ Comments  │         │Attachments│       │ Task Labels │
  └───────────┘         └───────────┘       └─────────────┘
        │                                          │
        │                                          │
        ▼                                          ▼
  ┌───────────┐                              ┌───────────┐
  │  Mentions │                              │  Labels   │
  └───────────┘                              └───────────┘

  ┌─────────────┐         ┌─────────────┐
  │Notifications│         │ Activity Log│
  └─────────────┘         └─────────────┘
```

---

## 📋 Chi tiết các Entity

### 1. **Users** (Người dùng)
Lưu trữ thông tin người dùng trong hệ thống.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID người dùng |
| username | VARCHAR(50) | UNIQUE, NOT NULL | Tên đăng nhập |
| email | VARCHAR(100) | UNIQUE, NOT NULL | Email |
| password_hash | VARCHAR(255) | NOT NULL | Mật khẩu đã hash |
| full_name | VARCHAR(100) | NOT NULL | Họ và tên |
| avatar_url | VARCHAR(500) | NULL | URL avatar |
| phone | VARCHAR(20) | NULL | Số điện thoại |
| timezone | VARCHAR(50) | DEFAULT 'Asia/Ho_Chi_Minh' | Múi giờ |
| language | VARCHAR(10) | DEFAULT 'vi' | Ngôn ngữ (vi, en) |
| email_verified | BOOLEAN | DEFAULT FALSE | Email đã xác thực? |
| is_active | BOOLEAN | DEFAULT TRUE | Tài khoản còn hoạt động? |
| last_login_at | TIMESTAMP | NULL | Lần đăng nhập cuối |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |

**Indexes:**
- `idx_users_email` ON email
- `idx_users_username` ON username
- `idx_users_is_active` ON is_active

---

### 2. **Projects** (Dự án/Workspace)
Lưu trữ thông tin các dự án.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID dự án |
| name | VARCHAR(100) | NOT NULL | Tên dự án |
| description | TEXT | NULL | Mô tả dự án |
| key | VARCHAR(10) | UNIQUE, NOT NULL | Key dự án (VD: TASK) |
| owner_id | UUID | FK -> Users(id) | Chủ sở hữu dự án |
| icon | VARCHAR(50) | NULL | Icon emoji |
| color | VARCHAR(7) | DEFAULT '#1976d2' | Màu sắc (hex) |
| is_public | BOOLEAN | DEFAULT FALSE | Dự án công khai? |
| is_archived | BOOLEAN | DEFAULT FALSE | Đã archive? |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |

**Indexes:**
- `idx_projects_owner` ON owner_id
- `idx_projects_key` ON key
- `idx_projects_is_archived` ON is_archived

---

### 3. **Project_Members** (Thành viên dự án)
Quản lý thành viên và phân quyền trong dự án.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID |
| project_id | UUID | FK -> Projects(id) | ID dự án |
| user_id | UUID | FK -> Users(id) | ID người dùng |
| role | VARCHAR(20) | NOT NULL | OWNER, ADMIN, MEMBER, VIEWER |
| joined_at | TIMESTAMP | DEFAULT NOW() | Ngày tham gia |

**Unique Constraint:** (project_id, user_id)
**Indexes:**
- `idx_project_members_project` ON project_id
- `idx_project_members_user` ON user_id

---

### 4. **Boards** (Bảng Kanban)
Lưu trữ các bảng kanban trong dự án.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID bảng |
| project_id | UUID | FK -> Projects(id) | ID dự án |
| name | VARCHAR(100) | NOT NULL | Tên bảng |
| description | TEXT | NULL | Mô tả |
| position | INTEGER | NOT NULL | Thứ tự hiển thị |
| is_default | BOOLEAN | DEFAULT FALSE | Bảng mặc định? |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |

**Indexes:**
- `idx_boards_project` ON project_id
- `idx_boards_position` ON (project_id, position)

---

### 5. **Columns** (Cột trạng thái)
Các cột trong bảng Kanban (TODO, IN PROGRESS, DONE, etc.)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID cột |
| board_id | UUID | FK -> Boards(id) | ID bảng |
| name | VARCHAR(50) | NOT NULL | Tên cột |
| position | INTEGER | NOT NULL | Thứ tự hiển thị |
| color | VARCHAR(7) | NULL | Màu sắc |
| is_completed | BOOLEAN | DEFAULT FALSE | Cột hoàn thành? |
| task_limit | INTEGER | NULL | Giới hạn task (WIP) |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |

**Indexes:**
- `idx_columns_board` ON board_id
- `idx_columns_position` ON (board_id, position)

---

### 6. **Tasks** (Công việc)
Lưu trữ thông tin chi tiết các task.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID task |
| project_id | UUID | FK -> Projects(id) | ID dự án |
| board_id | UUID | FK -> Boards(id) | ID bảng |
| column_id | UUID | FK -> Columns(id) | ID cột |
| task_number | INTEGER | NOT NULL | Số thứ tự task |
| title | VARCHAR(255) | NOT NULL | Tiêu đề |
| description | TEXT | NULL | Mô tả chi tiết |
| priority | VARCHAR(20) | DEFAULT 'MEDIUM' | LOW, MEDIUM, HIGH, URGENT |
| position | DECIMAL(10,2) | NOT NULL | Vị trí trong cột |
| assignee_id | UUID | FK -> Users(id) | Người được giao |
| reporter_id | UUID | FK -> Users(id) | Người tạo task |
| start_date | DATE | NULL | Ngày bắt đầu |
| due_date | DATE | NULL | Deadline |
| estimated_hours | DECIMAL(5,2) | NULL | Thời gian ước tính |
| actual_hours | DECIMAL(5,2) | NULL | Thời gian thực tế |
| completed_at | TIMESTAMP | NULL | Ngày hoàn thành |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |

**Unique Constraint:** (project_id, task_number)
**Indexes:**
- `idx_tasks_project` ON project_id
- `idx_tasks_board` ON board_id
- `idx_tasks_column` ON column_id
- `idx_tasks_assignee` ON assignee_id
- `idx_tasks_reporter` ON reporter_id
- `idx_tasks_due_date` ON due_date
- `idx_tasks_priority` ON priority
- `idx_tasks_position` ON (column_id, position)

---

### 7. **Labels** (Nhãn)
Quản lý các nhãn/tag cho task.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID label |
| project_id | UUID | FK -> Projects(id) | ID dự án |
| name | VARCHAR(50) | NOT NULL | Tên label |
| color | VARCHAR(7) | NOT NULL | Màu sắc |
| description | VARCHAR(200) | NULL | Mô tả |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

**Unique Constraint:** (project_id, name)
**Indexes:**
- `idx_labels_project` ON project_id

---

### 8. **Task_Labels** (Gắn nhãn cho task)
Bảng trung gian giữa Tasks và Labels.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID |
| task_id | UUID | FK -> Tasks(id) | ID task |
| label_id | UUID | FK -> Labels(id) | ID label |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày gắn |

**Unique Constraint:** (task_id, label_id)
**Indexes:**
- `idx_task_labels_task` ON task_id
- `idx_task_labels_label` ON label_id

---

### 9. **Comments** (Bình luận)
Lưu trữ bình luận trên task.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID comment |
| task_id | UUID | FK -> Tasks(id) | ID task |
| user_id | UUID | FK -> Users(id) | Người comment |
| content | TEXT | NOT NULL | Nội dung |
| parent_id | UUID | FK -> Comments(id) | ID comment cha (reply) |
| is_edited | BOOLEAN | DEFAULT FALSE | Đã chỉnh sửa? |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |

**Indexes:**
- `idx_comments_task` ON task_id
- `idx_comments_user` ON user_id
- `idx_comments_parent` ON parent_id

---

### 10. **Comment_Mentions** (Tag người dùng trong comment)
Lưu trữ các mentions (@username) trong comment.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID |
| comment_id | UUID | FK -> Comments(id) | ID comment |
| user_id | UUID | FK -> Users(id) | Người được mention |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày mention |

**Unique Constraint:** (comment_id, user_id)
**Indexes:**
- `idx_mentions_comment` ON comment_id
- `idx_mentions_user` ON user_id

---

### 11. **Attachments** (File đính kèm)
Quản lý các file đính kèm trong task.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID attachment |
| task_id | UUID | FK -> Tasks(id) | ID task |
| uploaded_by | UUID | FK -> Users(id) | Người upload |
| file_name | VARCHAR(255) | NOT NULL | Tên file |
| file_size | BIGINT | NOT NULL | Kích thước (bytes) |
| file_type | VARCHAR(100) | NOT NULL | Loại file (MIME) |
| file_url | VARCHAR(500) | NOT NULL | URL file |
| storage_path | VARCHAR(500) | NOT NULL | Đường dẫn lưu trữ |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày upload |

**Indexes:**
- `idx_attachments_task` ON task_id
- `idx_attachments_user` ON uploaded_by

---

### 12. **Notifications** (Thông báo)
Quản lý thông báo cho người dùng.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID thông báo |
| user_id | UUID | FK -> Users(id) | Người nhận |
| type | VARCHAR(50) | NOT NULL | TASK_ASSIGNED, MENTION, etc. |
| title | VARCHAR(255) | NOT NULL | Tiêu đề |
| message | TEXT | NOT NULL | Nội dung |
| related_type | VARCHAR(50) | NULL | TASK, COMMENT, PROJECT |
| related_id | UUID | NULL | ID đối tượng liên quan |
| is_read | BOOLEAN | DEFAULT FALSE | Đã đọc? |
| sent_email | BOOLEAN | DEFAULT FALSE | Đã gửi email? |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| read_at | TIMESTAMP | NULL | Ngày đọc |

**Indexes:**
- `idx_notifications_user` ON user_id
- `idx_notifications_is_read` ON (user_id, is_read)
- `idx_notifications_created` ON created_at

---

### 13. **Activity_Logs** (Lịch sử hoạt động)
Ghi lại mọi thay đổi trong hệ thống.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID log |
| user_id | UUID | FK -> Users(id) | Người thực hiện |
| project_id | UUID | FK -> Projects(id) | ID dự án |
| entity_type | VARCHAR(50) | NOT NULL | TASK, COMMENT, PROJECT |
| entity_id | UUID | NOT NULL | ID đối tượng |
| action | VARCHAR(50) | NOT NULL | CREATE, UPDATE, DELETE |
| old_value | JSONB | NULL | Giá trị cũ |
| new_value | JSONB | NULL | Giá trị mới |
| ip_address | VARCHAR(45) | NULL | IP thực hiện |
| user_agent | VARCHAR(255) | NULL | User agent |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày thực hiện |

**Indexes:**
- `idx_activity_logs_user` ON user_id
- `idx_activity_logs_project` ON project_id
- `idx_activity_logs_entity` ON (entity_type, entity_id)
- `idx_activity_logs_created` ON created_at

---

### 14. **Time_Tracking** (Theo dõi thời gian)
Ghi lại thời gian làm việc cho task.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID |
| task_id | UUID | FK -> Tasks(id) | ID task |
| user_id | UUID | FK -> Users(id) | Người thực hiện |
| description | VARCHAR(255) | NULL | Mô tả công việc |
| hours | DECIMAL(5,2) | NOT NULL | Số giờ |
| work_date | DATE | NOT NULL | Ngày làm việc |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày ghi |

**Indexes:**
- `idx_time_tracking_task` ON task_id
- `idx_time_tracking_user` ON user_id
- `idx_time_tracking_date` ON work_date

---

### 15. **Task_Dependencies** (Phụ thuộc giữa các task)
Quản lý mối quan hệ phụ thuộc giữa các task.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | ID |
| task_id | UUID | FK -> Tasks(id) | Task hiện tại |
| depends_on_task_id | UUID | FK -> Tasks(id) | Task phụ thuộc |
| type | VARCHAR(20) | DEFAULT 'BLOCKS' | BLOCKS, RELATES_TO |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

**Unique Constraint:** (task_id, depends_on_task_id)
**Indexes:**
- `idx_dependencies_task` ON task_id
- `idx_dependencies_depends` ON depends_on_task_id

---

## 🔐 Quy tắc Nghiệp vụ (Business Rules)

### User Management
1. Email và username phải là duy nhất
2. Password phải được hash bằng BCrypt (strength >= 10)
3. User chỉ có thể xóa khi không còn tham gia dự án nào

### Project Management
1. Mỗi project phải có ít nhất 1 OWNER
2. Project key phải là chữ in hoa, 2-10 ký tự
3. Task number tự động tăng theo project (PROJ-1, PROJ-2, ...)

### Task Management
1. Task chỉ có thể được assign cho member trong project
2. Task không thể có due_date < start_date
3. Task position sử dụng decimal để hỗ trợ reorder (1.0, 1.5, 2.0)
4. Khi move task sang cột "Completed", tự động set completed_at

### Permissions
- **OWNER**: Full access, có thể xóa project
- **ADMIN**: Quản lý members, settings, không thể xóa project
- **MEMBER**: Tạo/sửa tasks, comments
- **VIEWER**: Chỉ xem, không được chỉnh sửa

---

## 🚀 Performance Optimization

### Partitioning (Optional - cho production)
```sql
-- Partition Activity_Logs by month
CREATE TABLE activity_logs_y2024m01 PARTITION OF activity_logs
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

### Materialized Views
```sql
-- View thống kê task theo project
CREATE MATERIALIZED VIEW project_task_stats AS
SELECT
    project_id,
    COUNT(*) as total_tasks,
    COUNT(CASE WHEN completed_at IS NOT NULL THEN 1 END) as completed_tasks,
    AVG(actual_hours) as avg_hours
FROM tasks
GROUP BY project_id;

-- Refresh định kỳ
REFRESH MATERIALIZED VIEW project_task_stats;
```

---

## 🔄 Migration Strategy

### Phase 1: Core Tables (MVP)
1. Users, Projects, Project_Members
2. Boards, Columns, Tasks
3. Comments, Attachments

### Phase 2: Enhanced Features
4. Labels, Task_Labels
5. Notifications
6. Activity_Logs

### Phase 3: Advanced Features
7. Time_Tracking
8. Task_Dependencies
9. Comment_Mentions

---

## 📝 Naming Conventions

- **Tables**: snake_case, plural (users, tasks)
- **Columns**: snake_case (created_at, user_id)
- **Indexes**: `idx_{table}_{column}`
- **Foreign Keys**: `fk_{table}_{ref_table}`
- **Unique Constraints**: `uq_{table}_{columns}`

---

## 📚 References

- PostgreSQL Best Practices: https://wiki.postgresql.org/wiki/Don%27t_Do_This
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Database Normalization: https://en.wikipedia.org/wiki/Database_normalization

---

**Version**: 1.0.0
**Last Updated**: 2025-02-05
**Author**: Taskoryx Development Team
