-- ==================== TASKORYX DATABASE SCHEMA ====================
-- PostgreSQL 14+
-- Run this script after running database-init.sql
-- This script creates all tables, indexes, and constraints
-- ===================================================================

-- Connect to database first: \c taskoryx_dev

-- ==================== ENABLE EXTENSIONS ====================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==================== DROP TABLES (for clean reinstall) ====================
-- Uncomment below to drop all tables
/*
DROP TABLE IF EXISTS task_dependencies CASCADE;
DROP TABLE IF EXISTS time_tracking CASCADE;
DROP TABLE IF EXISTS activity_logs CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS attachments CASCADE;
DROP TABLE IF EXISTS comment_mentions CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS task_labels CASCADE;
DROP TABLE IF EXISTS labels CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS columns CASCADE;
DROP TABLE IF EXISTS boards CASCADE;
DROP TABLE IF EXISTS project_members CASCADE;
DROP TABLE IF EXISTS projects CASCADE;
DROP TABLE IF EXISTS users CASCADE;
*/

-- ==================== CORE TABLES ====================

-- 1. USERS TABLE
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    timezone VARCHAR(50) DEFAULT 'Asia/Ho_Chi_Minh',
    language VARCHAR(10) DEFAULT 'vi',
    email_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_username_format CHECK (username ~ '^[a-zA-Z0-9_-]+$'),
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_language CHECK (language IN ('vi', 'en'))
);

COMMENT ON TABLE users IS 'Lưu trữ thông tin người dùng';
COMMENT ON COLUMN users.password_hash IS 'Mật khẩu đã hash bằng BCrypt';
COMMENT ON COLUMN users.timezone IS 'Múi giờ của người dùng';

-- 2. PROJECTS TABLE
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    key VARCHAR(10) UNIQUE NOT NULL,
    owner_id UUID NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(7) DEFAULT '#1976d2',
    is_public BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_key_format CHECK (key ~ '^[A-Z0-9]{2,10}$'),
    CONSTRAINT chk_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

COMMENT ON TABLE projects IS 'Lưu trữ thông tin các dự án/workspace';
COMMENT ON COLUMN projects.key IS 'Key dự án (VD: TASK, PROJ) - dùng cho task number';

-- 3. PROJECT_MEMBERS TABLE
CREATE TABLE project_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_members_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_user UNIQUE (project_id, user_id),
    CONSTRAINT chk_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

COMMENT ON TABLE project_members IS 'Quản lý thành viên và phân quyền trong dự án';
COMMENT ON COLUMN project_members.role IS 'OWNER: Full access | ADMIN: Manage members | MEMBER: Edit tasks | VIEWER: Read only';

-- 4. BOARDS TABLE
CREATE TABLE boards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    position INTEGER NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_boards_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_position CHECK (position >= 0)
);

COMMENT ON TABLE boards IS 'Các bảng Kanban trong dự án';

-- 5. COLUMNS TABLE (Status columns)
CREATE TABLE columns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    position INTEGER NOT NULL,
    color VARCHAR(7),
    is_completed BOOLEAN DEFAULT FALSE,
    task_limit INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_columns_board FOREIGN KEY (board_id)
        REFERENCES boards(id) ON DELETE CASCADE,
    CONSTRAINT chk_position CHECK (position >= 0),
    CONSTRAINT chk_task_limit CHECK (task_limit IS NULL OR task_limit > 0)
);

COMMENT ON TABLE columns IS 'Các cột trạng thái trong bảng Kanban (TODO, IN PROGRESS, DONE)';
COMMENT ON COLUMN columns.task_limit IS 'WIP limit - giới hạn số lượng task trong cột';

-- 6. TASKS TABLE
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    board_id UUID,
    column_id UUID,
    task_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(20) DEFAULT 'TODO',
    position DECIMAL(10,2) NOT NULL,
    assignee_id UUID,
    reporter_id UUID NOT NULL,
    start_date DATE,
    due_date DATE,
    estimated_hours DECIMAL(5,2),
    actual_hours DECIMAL(5,2),
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_board FOREIGN KEY (board_id)
        REFERENCES boards(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_column FOREIGN KEY (column_id)
        REFERENCES columns(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_tasks_reporter FOREIGN KEY (reporter_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_project_task_number UNIQUE (project_id, task_number),
    CONSTRAINT chk_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_dates CHECK (start_date IS NULL OR due_date IS NULL OR start_date <= due_date),
    CONSTRAINT chk_hours CHECK (
        (estimated_hours IS NULL OR estimated_hours > 0) AND
        (actual_hours IS NULL OR actual_hours >= 0)
    )
);

COMMENT ON TABLE tasks IS 'Lưu trữ thông tin chi tiết các task';
COMMENT ON COLUMN tasks.task_number IS 'Số thứ tự task trong project (PROJ-1, PROJ-2)';
COMMENT ON COLUMN tasks.position IS 'Vị trí trong cột - dùng decimal để dễ reorder (1.0, 1.5, 2.0)';

-- 7. LABELS TABLE
CREATE TABLE labels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_labels_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_label_name UNIQUE (project_id, name),
    CONSTRAINT chk_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

COMMENT ON TABLE labels IS 'Quản lý các nhãn/tag cho task';

-- 8. TASK_LABELS TABLE (Many-to-Many)
CREATE TABLE task_labels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    label_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_labels_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_labels_label FOREIGN KEY (label_id)
        REFERENCES labels(id) ON DELETE CASCADE,
    CONSTRAINT uq_task_label UNIQUE (task_id, label_id)
);

COMMENT ON TABLE task_labels IS 'Bảng trung gian giữa Tasks và Labels';

-- 9. COMMENTS TABLE
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    content TEXT NOT NULL,
    parent_id UUID,
    is_edited BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comments_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id)
        REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT chk_content_not_empty CHECK (LENGTH(TRIM(content)) > 0)
);

COMMENT ON TABLE comments IS 'Lưu trữ bình luận trên task';
COMMENT ON COLUMN comments.parent_id IS 'ID comment cha - dùng cho reply/thread';

-- 10. COMMENT_MENTIONS TABLE
CREATE TABLE comment_mentions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mentions_comment FOREIGN KEY (comment_id)
        REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_mentions_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_comment_mention UNIQUE (comment_id, user_id)
);

COMMENT ON TABLE comment_mentions IS 'Lưu trữ các mentions (@username) trong comment';

-- 11. ATTACHMENTS TABLE
CREATE TABLE attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    uploaded_by UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_attachments_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_attachments_user FOREIGN KEY (uploaded_by)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_file_size CHECK (file_size > 0 AND file_size <= 104857600) -- Max 100MB
);

COMMENT ON TABLE attachments IS 'Quản lý các file đính kèm trong task';
COMMENT ON COLUMN attachments.file_size IS 'Kích thước file (bytes) - Max 100MB';

-- 12. NOTIFICATIONS TABLE
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_type VARCHAR(50),
    related_id UUID,
    is_read BOOLEAN DEFAULT FALSE,
    sent_email BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,

    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_type CHECK (type IN (
        'TASK_ASSIGNED', 'TASK_UPDATED', 'TASK_COMMENTED',
        'MENTION', 'DUE_DATE_REMINDER', 'PROJECT_INVITE'
    )),
    CONSTRAINT chk_related_type CHECK (related_type IN ('TASK', 'COMMENT', 'PROJECT') OR related_type IS NULL)
);

COMMENT ON TABLE notifications IS 'Quản lý thông báo cho người dùng';
COMMENT ON COLUMN notifications.type IS 'Loại thông báo';
COMMENT ON COLUMN notifications.related_type IS 'Loại đối tượng liên quan';

-- 13. ACTIVITY_LOGS TABLE
CREATE TABLE activity_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    project_id UUID,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_logs_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_entity_type CHECK (entity_type IN (
        'TASK', 'COMMENT', 'PROJECT', 'BOARD', 'COLUMN', 'ATTACHMENT'
    )),
    CONSTRAINT chk_action CHECK (action IN (
        'CREATE', 'UPDATE', 'DELETE', 'MOVE', 'ASSIGN', 'COMPLETE'
    ))
);

COMMENT ON TABLE activity_logs IS 'Ghi lại mọi thay đổi trong hệ thống (audit log)';
COMMENT ON COLUMN activity_logs.old_value IS 'Giá trị trước khi thay đổi (JSON)';
COMMENT ON COLUMN activity_logs.new_value IS 'Giá trị sau khi thay đổi (JSON)';

-- 14. TIME_TRACKING TABLE
CREATE TABLE time_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    description VARCHAR(255),
    hours DECIMAL(5,2) NOT NULL,
    work_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_time_tracking_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_time_tracking_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_hours CHECK (hours > 0 AND hours <= 24)
);

COMMENT ON TABLE time_tracking IS 'Theo dõi thời gian làm việc cho task';

-- 15. TASK_DEPENDENCIES TABLE
CREATE TABLE task_dependencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    depends_on_task_id UUID NOT NULL,
    type VARCHAR(20) DEFAULT 'BLOCKS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dependencies_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_dependencies_depends FOREIGN KEY (depends_on_task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT uq_task_dependency UNIQUE (task_id, depends_on_task_id),
    CONSTRAINT chk_no_self_dependency CHECK (task_id != depends_on_task_id),
    CONSTRAINT chk_type CHECK (type IN ('BLOCKS', 'RELATES_TO'))
);

COMMENT ON TABLE task_dependencies IS 'Quản lý mối quan hệ phụ thuộc giữa các task';

-- ==================== INDEXES ====================

-- Users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Projects
CREATE INDEX idx_projects_owner ON projects(owner_id);
CREATE INDEX idx_projects_key ON projects(key);
CREATE INDEX idx_projects_is_archived ON projects(is_archived);
CREATE INDEX idx_projects_created_at ON projects(created_at);

-- Project Members
CREATE INDEX idx_project_members_project ON project_members(project_id);
CREATE INDEX idx_project_members_user ON project_members(user_id);
CREATE INDEX idx_project_members_role ON project_members(role);

-- Boards
CREATE INDEX idx_boards_project ON boards(project_id);
CREATE INDEX idx_boards_position ON boards(project_id, position);

-- Columns
CREATE INDEX idx_columns_board ON columns(board_id);
CREATE INDEX idx_columns_position ON columns(board_id, position);

-- Tasks
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_board ON tasks(board_id);
CREATE INDEX idx_tasks_column ON tasks(column_id);
CREATE INDEX idx_tasks_assignee ON tasks(assignee_id);
CREATE INDEX idx_tasks_reporter ON tasks(reporter_id);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_position ON tasks(column_id, position);
CREATE INDEX idx_tasks_completed_at ON tasks(completed_at);
CREATE INDEX idx_tasks_backlog ON tasks(project_id) WHERE column_id IS NULL;
CREATE INDEX idx_tasks_created_at ON tasks(created_at);

-- Labels
CREATE INDEX idx_labels_project ON labels(project_id);

-- Task Labels
CREATE INDEX idx_task_labels_task ON task_labels(task_id);
CREATE INDEX idx_task_labels_label ON task_labels(label_id);

-- Comments
CREATE INDEX idx_comments_task ON comments(task_id);
CREATE INDEX idx_comments_user ON comments(user_id);
CREATE INDEX idx_comments_parent ON comments(parent_id);
CREATE INDEX idx_comments_created_at ON comments(created_at);

-- Comment Mentions
CREATE INDEX idx_mentions_comment ON comment_mentions(comment_id);
CREATE INDEX idx_mentions_user ON comment_mentions(user_id);

-- Attachments
CREATE INDEX idx_attachments_task ON attachments(task_id);
CREATE INDEX idx_attachments_user ON attachments(uploaded_by);

-- Notifications
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_type ON notifications(type);

-- Activity Logs
CREATE INDEX idx_activity_logs_user ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_project ON activity_logs(project_id);
CREATE INDEX idx_activity_logs_entity ON activity_logs(entity_type, entity_id);
CREATE INDEX idx_activity_logs_created_at ON activity_logs(created_at);

-- Time Tracking
CREATE INDEX idx_time_tracking_task ON time_tracking(task_id);
CREATE INDEX idx_time_tracking_user ON time_tracking(user_id);
CREATE INDEX idx_time_tracking_date ON time_tracking(work_date);

-- Task Dependencies
CREATE INDEX idx_dependencies_task ON task_dependencies(task_id);
CREATE INDEX idx_dependencies_depends ON task_dependencies(depends_on_task_id);

-- ==================== FUNCTIONS & TRIGGERS ====================

-- Function to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to tables with updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_projects_updated_at BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_boards_updated_at BEFORE UPDATE ON boards
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_columns_updated_at BEFORE UPDATE ON columns
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tasks_updated_at BEFORE UPDATE ON tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_comments_updated_at BEFORE UPDATE ON comments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to auto-generate task number
CREATE OR REPLACE FUNCTION generate_task_number()
RETURNS TRIGGER AS $$
BEGIN
    SELECT COALESCE(MAX(task_number), 0) + 1
    INTO NEW.task_number
    FROM tasks
    WHERE project_id = NEW.project_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_task_number BEFORE INSERT ON tasks
    FOR EACH ROW
    WHEN (NEW.task_number IS NULL)
    EXECUTE FUNCTION generate_task_number();

-- Function to set completed_at when task is moved to completed column
CREATE OR REPLACE FUNCTION set_task_completed()
RETURNS TRIGGER AS $$
DECLARE
    column_is_completed BOOLEAN;
BEGIN
    SELECT is_completed INTO column_is_completed
    FROM columns
    WHERE id = NEW.column_id;

    IF column_is_completed = TRUE AND OLD.completed_at IS NULL THEN
        NEW.completed_at = CURRENT_TIMESTAMP;
    ELSIF column_is_completed = FALSE THEN
        NEW.completed_at = NULL;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER task_completion_trigger BEFORE UPDATE ON tasks
    FOR EACH ROW
    WHEN (OLD.column_id IS DISTINCT FROM NEW.column_id)
    EXECUTE FUNCTION set_task_completed();

-- ==================== INITIAL DATA ====================

-- Insert sample user (password: Admin@123)
INSERT INTO users (id, username, email, password_hash, full_name, is_active, email_verified, must_change_password)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@taskoryx.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8UeVrS9NdHsaLSwyHW', -- Admin@123
    'System Administrator',
    TRUE,
    TRUE,
    FALSE
);

-- Insert sample project
INSERT INTO projects (id, name, description, key, owner_id, color)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Sample Project',
    'This is a sample project to get you started',
    'SAMPLE',
    '00000000-0000-0000-0000-000000000001',
    '#1976d2'
);

-- Add admin as owner
INSERT INTO project_members (project_id, user_id, role)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'OWNER'
);

-- Insert default board
INSERT INTO boards (id, project_id, name, description, position, is_default)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'Main Board',
    'Default Kanban board',
    0,
    TRUE
);

-- Insert default columns
INSERT INTO columns (board_id, name, position, color, is_completed) VALUES
    ('00000000-0000-0000-0000-000000000001', 'To Do', 0, '#42526E', FALSE),
    ('00000000-0000-0000-0000-000000000001', 'In Progress', 1, '#0052CC', FALSE),
    ('00000000-0000-0000-0000-000000000001', 'Review', 2, '#FF991F', FALSE),
    ('00000000-0000-0000-0000-000000000001', 'Done', 3, '#00875A', TRUE);

-- Insert sample labels
INSERT INTO labels (project_id, name, color, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Bug', '#d32f2f', 'Something is not working'),
    ('00000000-0000-0000-0000-000000000001', 'Feature', '#1976d2', 'New feature request'),
    ('00000000-0000-0000-0000-000000000001', 'Documentation', '#388e3c', 'Documentation improvements');

-- ==================== VERIFICATION ====================

-- Count tables
SELECT
    schemaname,
    COUNT(*) as table_count
FROM pg_tables
WHERE schemaname = 'public'
GROUP BY schemaname;

-- List all tables
SELECT
    table_name,
    pg_size_pretty(pg_total_relation_size(quote_ident(table_name))) as size
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- ==================== END OF SCRIPT ====================
-- Schema version: 1.0.0
-- Created: 2025-02-05
