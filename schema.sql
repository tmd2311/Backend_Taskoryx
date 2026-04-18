-- ==================== TASKORYX DATABASE SCHEMA ====================
-- PostgreSQL 14+
-- Sinh từ toàn bộ 25 JPA Entity (Spring Boot 3.2.1 / Hibernate 6)
-- Cập nhật lần cuối: 2026-04-11
-- ===================================================================

-- ==================== EXTENSIONS ====================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==================== DROP (clean reinstall) ====================
-- Bỏ comment block dưới đây nếu muốn xóa sạch để tạo lại
/*
DROP TABLE IF EXISTS user_project_performance CASCADE;
DROP TABLE IF EXISTS project_roles         CASCADE;
DROP TABLE IF EXISTS sprint_tasks          CASCADE;
DROP TABLE IF EXISTS sprints               CASCADE;
DROP TABLE IF EXISTS task_watchers         CASCADE;
DROP TABLE IF EXISTS task_dependencies     CASCADE;
DROP TABLE IF EXISTS time_tracking         CASCADE;
DROP TABLE IF EXISTS comment_mentions      CASCADE;
DROP TABLE IF EXISTS comments              CASCADE;
DROP TABLE IF EXISTS task_labels           CASCADE;
DROP TABLE IF EXISTS attachments           CASCADE;
DROP TABLE IF EXISTS tasks                 CASCADE;
DROP TABLE IF EXISTS issue_categories      CASCADE;
DROP TABLE IF EXISTS versions              CASCADE;
DROP TABLE IF EXISTS columns               CASCADE;
DROP TABLE IF EXISTS boards                CASCADE;
DROP TABLE IF EXISTS webhooks              CASCADE;
DROP TABLE IF EXISTS project_templates     CASCADE;
DROP TABLE IF EXISTS project_members       CASCADE;
DROP TABLE IF EXISTS labels                CASCADE;
DROP TABLE IF EXISTS projects              CASCADE;
DROP TABLE IF EXISTS activity_logs         CASCADE;
DROP TABLE IF EXISTS notifications         CASCADE;
DROP TABLE IF EXISTS user_roles            CASCADE;
DROP TABLE IF EXISTS role_permissions      CASCADE;
DROP TABLE IF EXISTS roles                 CASCADE;
DROP TABLE IF EXISTS permissions           CASCADE;
DROP TABLE IF EXISTS users                 CASCADE;
*/

-- ==================== 1. USERS ====================
CREATE TABLE users (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username            VARCHAR(50)  UNIQUE NOT NULL,
    email               VARCHAR(100) UNIQUE NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(100) NOT NULL,
    avatar_url          VARCHAR(500),
    phone               VARCHAR(20),
    timezone            VARCHAR(50)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    language            VARCHAR(10)  NOT NULL DEFAULT 'vi',
    email_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN     NOT NULL DEFAULT FALSE,
    last_login_at       TIMESTAMP,
    two_factor_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    two_factor_secret   VARCHAR(100),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_username_format CHECK (username ~ '^[a-zA-Z0-9_-]+$'),
    CONSTRAINT chk_users_email_format    CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_users_language        CHECK (language IN ('vi', 'en'))
);

COMMENT ON TABLE  users                      IS 'Người dùng hệ thống';
COMMENT ON COLUMN users.password_hash        IS 'BCrypt hash';
COMMENT ON COLUMN users.two_factor_secret    IS 'TOTP secret key (Base32), null khi chưa bật 2FA';

-- ==================== 2. PERMISSIONS ====================
CREATE TABLE permissions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    resource    VARCHAR(50)  NOT NULL    -- nhóm: USER, PROJECT, TASK, BOARD, REPORT, ADMIN
);

COMMENT ON TABLE  permissions          IS 'Quyền chi tiết trong hệ thống RBAC';
COMMENT ON COLUMN permissions.resource IS 'Nhóm tài nguyên: USER | PROJECT | TASK | BOARD | REPORT | ADMIN';

-- ==================== 3. ROLES ====================
CREATE TABLE roles (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(100) UNIQUE NOT NULL,
    description    VARCHAR(255),
    is_system_role BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  roles                IS 'Role hệ thống (RBAC)';
COMMENT ON COLUMN roles.is_system_role IS 'TRUE = role mặc định, không cho phép xóa';

-- ==================== 4. ROLE_PERMISSIONS (M-M) ====================
CREATE TABLE role_permissions (
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,

    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uq_role_permission UNIQUE (role_id, permission_id)
);

-- ==================== 5. USER_ROLES ====================
CREATE TABLE user_roles (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID      NOT NULL,
    role_id     UUID      NOT NULL,
    assigned_by UUID,                   -- null = tự động gán bởi hệ thống
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_roles_user        FOREIGN KEY (user_id)     REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role        FOREIGN KEY (role_id)     REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);

-- ==================== 6. PROJECTS ====================
CREATE TABLE projects (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    key         VARCHAR(10)  UNIQUE NOT NULL,
    owner_id    UUID         NOT NULL,
    icon        VARCHAR(50),
    color       VARCHAR(7)   DEFAULT '#1976d2',
    is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_archived BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_projects_owner    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_projects_key     CHECK (key ~ '^[A-Z0-9]{2,10}$'),
    CONSTRAINT chk_projects_color   CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);

COMMENT ON COLUMN projects.key IS 'Mã dự án viết hoa (VD: TX, PROJ) — dùng làm prefix cho task key';

-- ==================== 7. PROJECT_MEMBERS ====================
CREATE TABLE project_members (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID      NOT NULL,
    user_id    UUID      NOT NULL,
    role       VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT uq_project_user UNIQUE (project_id, user_id),
    CONSTRAINT chk_pm_role CHECK (role IN ('OWNER','ADMIN','MEMBER','VIEWER'))
);

-- ==================== 8. BOARDS ====================
CREATE TABLE boards (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID         NOT NULL,
    owner_id    UUID,                    -- null = board chung; non-null = board cá nhân (PERSONAL)
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    position    INTEGER      NOT NULL DEFAULT 0,
    board_type  VARCHAR(10)  NOT NULL DEFAULT 'KANBAN',
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_boards_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_boards_owner   FOREIGN KEY (owner_id)   REFERENCES users(id)    ON DELETE SET NULL,
    CONSTRAINT chk_boards_type   CHECK (board_type IN ('KANBAN','SCRUM','SPRINT','PERSONAL')),
    CONSTRAINT chk_boards_pos    CHECK (position >= 0)
);

COMMENT ON COLUMN boards.board_type IS 'KANBAN=board chung | SPRINT=tự động tạo theo sprint | PERSONAL=board cá nhân';
COMMENT ON COLUMN boards.owner_id   IS 'Chỉ có giá trị với board PERSONAL';

-- ==================== 9. COLUMNS ====================
CREATE TABLE columns (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id       UUID        NOT NULL,
    name           VARCHAR(50) NOT NULL,
    position       INTEGER     NOT NULL DEFAULT 0,
    color          VARCHAR(7),
    is_completed   BOOLEAN     NOT NULL DEFAULT FALSE,
    mapped_status  VARCHAR(20),          -- ánh xạ sang TaskStatus; null = không tự động đổi trạng thái
    task_limit     INTEGER,              -- WIP limit
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_columns_board      FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    CONSTRAINT chk_columns_pos       CHECK (position >= 0),
    CONSTRAINT chk_columns_limit     CHECK (task_limit IS NULL OR task_limit > 0),
    CONSTRAINT chk_columns_status    CHECK (mapped_status IS NULL OR mapped_status IN
        ('TODO','IN_PROGRESS','IN_REVIEW','RESOLVED','DONE','CANCELLED'))
);

COMMENT ON COLUMN columns.mapped_status IS 'Khi task kéo vào cột này, task.status tự động set theo giá trị này (chỉ dùng cho board SPRINT)';
COMMENT ON COLUMN columns.task_limit    IS 'WIP Limit — giới hạn số task tối đa trong cột';

-- ==================== 10. LABELS ====================
CREATE TABLE labels (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID         NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    color       VARCHAR(7)   NOT NULL,
    description VARCHAR(200),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_labels_project    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_label_name UNIQUE (project_id, name),
    CONSTRAINT chk_labels_color     CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

-- ==================== 11. VERSIONS ====================
CREATE TABLE versions (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID        NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    due_date     DATE,
    release_date DATE,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_versions_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_versions_status CHECK (status IN ('OPEN','LOCKED','CLOSED'))
);

-- ==================== 12. ISSUE_CATEGORIES ====================
CREATE TABLE issue_categories (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          UUID         NOT NULL,
    name                VARCHAR(100) NOT NULL,
    default_assignee_id UUID,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_categories_project  FOREIGN KEY (project_id)          REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_categories_assignee FOREIGN KEY (default_assignee_id) REFERENCES users(id)    ON DELETE SET NULL
);

-- ==================== 13. TASKS ====================
CREATE TABLE tasks (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID           NOT NULL,
    board_id        UUID,
    column_id       UUID,
    parent_id       UUID,                         -- subtask: task cha
    version_id      UUID,
    category_id     UUID,
    task_number     INTEGER        NOT NULL,
    title           VARCHAR(255)   NOT NULL,
    description     TEXT,
    priority        VARCHAR(20)    NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20)    NOT NULL DEFAULT 'TODO',
    position        NUMERIC(10,2)  NOT NULL DEFAULT 0,
    assignee_id     UUID,
    reporter_id     UUID           NOT NULL,
    start_date      DATE,
    due_date        DATE,
    estimated_hours NUMERIC(5,2),
    actual_hours    NUMERIC(5,2),
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tasks_project  FOREIGN KEY (project_id)  REFERENCES projects(id)         ON DELETE CASCADE,
    CONSTRAINT fk_tasks_board    FOREIGN KEY (board_id)    REFERENCES boards(id)            ON DELETE SET NULL,
    CONSTRAINT fk_tasks_column   FOREIGN KEY (column_id)   REFERENCES columns(id)           ON DELETE SET NULL,
    CONSTRAINT fk_tasks_parent   FOREIGN KEY (parent_id)   REFERENCES tasks(id)             ON DELETE SET NULL,
    CONSTRAINT fk_tasks_version  FOREIGN KEY (version_id)  REFERENCES versions(id)          ON DELETE SET NULL,
    CONSTRAINT fk_tasks_category FOREIGN KEY (category_id) REFERENCES issue_categories(id)  ON DELETE SET NULL,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users(id)             ON DELETE SET NULL,
    CONSTRAINT fk_tasks_reporter FOREIGN KEY (reporter_id) REFERENCES users(id)             ON DELETE RESTRICT,
    CONSTRAINT uq_project_task_number UNIQUE (project_id, task_number),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    CONSTRAINT chk_tasks_status   CHECK (status   IN ('TODO','IN_PROGRESS','IN_REVIEW','RESOLVED','DONE','CANCELLED')),
    CONSTRAINT chk_tasks_dates    CHECK (start_date IS NULL OR due_date IS NULL OR start_date <= due_date),
    CONSTRAINT chk_tasks_no_self_parent CHECK (parent_id IS NULL OR parent_id != id)
);

COMMENT ON COLUMN tasks.task_number IS 'Số thứ tự trong project, kết hợp project.key thành task key (TX-1, TX-2...)';
COMMENT ON COLUMN tasks.position    IS 'Vị trí trong cột (BigDecimal để dễ reorder, dùng midpoint algorithm)';
COMMENT ON COLUMN tasks.parent_id   IS 'Subtask: trỏ về task cha; null = task độc lập';

-- ==================== 14. TASK_LABELS (M-M) ====================
CREATE TABLE task_labels (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id    UUID      NOT NULL,
    label_id   UUID      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_labels_task  FOREIGN KEY (task_id)  REFERENCES tasks(id)  ON DELETE CASCADE,
    CONSTRAINT fk_task_labels_label FOREIGN KEY (label_id) REFERENCES labels(id) ON DELETE CASCADE,
    CONSTRAINT uq_task_label UNIQUE (task_id, label_id)
);

-- ==================== 15. COMMENTS ====================
CREATE TABLE comments (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id    UUID      NOT NULL,
    user_id    UUID      NOT NULL,
    content    TEXT      NOT NULL,
    parent_id  UUID,                   -- reply/thread
    is_edited  BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comments_task   FOREIGN KEY (task_id)   REFERENCES tasks(id)    ON DELETE CASCADE,
    CONSTRAINT fk_comments_user   FOREIGN KEY (user_id)   REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT chk_comments_content CHECK (LENGTH(TRIM(content)) > 0)
);

-- ==================== 16. COMMENT_MENTIONS ====================
CREATE TABLE comment_mentions (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id UUID      NOT NULL,
    user_id    UUID      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mentions_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_mentions_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT uq_comment_mention  UNIQUE (comment_id, user_id)
);

-- ==================== 17. ATTACHMENTS ====================
CREATE TABLE attachments (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id      UUID         NOT NULL,
    uploaded_by  UUID         NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    file_size    BIGINT       NOT NULL,
    file_type    VARCHAR(100) NOT NULL,
    file_url     VARCHAR(500) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_attachments_task FOREIGN KEY (task_id)     REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_attachments_user FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_attachments_size CHECK (file_size > 0 AND file_size <= 10485760)  -- max 10MB
);

COMMENT ON COLUMN attachments.storage_path IS 'Đường dẫn vật lý trên server (uploads/)';

-- ==================== 18. TASK_WATCHERS ====================
CREATE TABLE task_watchers (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id    UUID      NOT NULL,
    user_id    UUID      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_watchers_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_watchers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_task_watcher  UNIQUE (task_id, user_id)
);

-- ==================== 20. TASK_DEPENDENCIES ====================
CREATE TABLE task_dependencies (
    id                UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id           UUID      NOT NULL,
    depends_on_task_id UUID     NOT NULL,
    type              VARCHAR(20) NOT NULL DEFAULT 'BLOCKS',
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dep_task    FOREIGN KEY (task_id)            REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_dep_depends FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT uq_task_dependency UNIQUE (task_id, depends_on_task_id),
    CONSTRAINT chk_dep_no_self CHECK (task_id != depends_on_task_id),
    CONSTRAINT chk_dep_type   CHECK (type IN ('BLOCKS','DEPENDS_ON','RELATES_TO','DUPLICATES','PRECEDES','FOLLOWS'))
);

-- ==================== 21. TIME_TRACKING ====================
CREATE TABLE time_tracking (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id     UUID          NOT NULL,
    user_id     UUID          NOT NULL,
    description VARCHAR(255),
    hours       NUMERIC(5,2)  NOT NULL,
    work_date   DATE          NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_time_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_time_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_time_hours CHECK (hours > 0 AND hours <= 24)
);

-- ==================== 22. SPRINTS ====================
CREATE TABLE sprints (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID         NOT NULL,
    board_id     UUID,                    -- board SPRINT tự động tạo kèm theo
    name         VARCHAR(255) NOT NULL,
    goal         TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    start_date   DATE,
    end_date     DATE,
    completed_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sprints_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_sprints_board   FOREIGN KEY (board_id)   REFERENCES boards(id)   ON DELETE SET NULL,
    CONSTRAINT chk_sprints_status CHECK (status IN ('PLANNED','ACTIVE','COMPLETED','CANCELLED')),
    CONSTRAINT chk_sprints_dates  CHECK (start_date IS NULL OR end_date IS NULL OR start_date < end_date)
);

COMMENT ON COLUMN sprints.board_id IS 'Board SPRINT tự động tạo khi sprint được tạo, xóa sprint → xóa board';

-- ==================== 23. SPRINT_TASKS (M-M) ====================
CREATE TABLE sprint_tasks (
    sprint_id UUID NOT NULL,
    task_id   UUID NOT NULL,

    PRIMARY KEY (sprint_id, task_id),
    CONSTRAINT fk_st_sprint FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE,
    CONSTRAINT fk_st_task   FOREIGN KEY (task_id)   REFERENCES tasks(id)   ON DELETE CASCADE
);

-- ==================== 24. NOTIFICATIONS ====================
CREATE TABLE notifications (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    message      TEXT         NOT NULL,
    related_type VARCHAR(50),
    related_id   UUID,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    sent_email   BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at      TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ==================== 25. ACTIVITY_LOGS ====================
CREATE TABLE activity_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    project_id  UUID,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   UUID        NOT NULL,
    action      VARCHAR(50) NOT NULL,
    old_value   JSONB,
    new_value   JSONB,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(255),
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_activity_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_activity_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- ==================== 26. WEBHOOKS ====================
CREATE TABLE webhooks (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id       UUID         NOT NULL,
    created_by       UUID         NOT NULL,
    name             VARCHAR(255) NOT NULL,
    url              VARCHAR(500) NOT NULL,
    secret_token     VARCHAR(255),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    events           TEXT,                  -- comma-separated: TASK_CREATED,TASK_UPDATED,...
    last_triggered_at TIMESTAMP,
    success_count    INTEGER      NOT NULL DEFAULT 0,
    failure_count    INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_webhooks_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_webhooks_user    FOREIGN KEY (created_by) REFERENCES users(id)    ON DELETE CASCADE
);

COMMENT ON COLUMN webhooks.events IS 'Danh sách event lắng nghe, lưu dạng CSV: TASK_CREATED,SPRINT_STARTED,...';

-- ==================== 27. PROJECT_TEMPLATES ====================
CREATE TABLE project_templates (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    category       VARCHAR(100),          -- Software, Marketing, Design...
    icon           VARCHAR(10),
    color          VARCHAR(7),
    columns_config TEXT,                  -- JSON array cấu hình cột mặc định
    is_public      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by     UUID,                  -- null = system template
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_templates_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

COMMENT ON COLUMN project_templates.columns_config IS 'JSON array: [{"name":"To Do","color":"#gray","isCompleted":false},...]';
COMMENT ON COLUMN project_templates.created_by     IS 'null = template hệ thống, non-null = template người dùng tạo';

-- ==================== INDEXES ====================

-- users
CREATE INDEX idx_users_email     ON users(email);
CREATE INDEX idx_users_username  ON users(username);
CREATE INDEX idx_users_is_active ON users(is_active);

-- permissions / roles
CREATE INDEX idx_permissions_name ON permissions(name);
CREATE INDEX idx_roles_name       ON roles(name);
CREATE INDEX idx_user_roles_user  ON user_roles(user_id);
CREATE INDEX idx_user_roles_role  ON user_roles(role_id);

-- projects
CREATE INDEX idx_projects_owner      ON projects(owner_id);
CREATE INDEX idx_projects_key        ON projects(key);
CREATE INDEX idx_projects_is_archived ON projects(is_archived);

-- project_members
CREATE INDEX idx_pm_project ON project_members(project_id);
CREATE INDEX idx_pm_user    ON project_members(user_id);

-- boards
CREATE INDEX idx_boards_project ON boards(project_id);
CREATE INDEX idx_boards_owner   ON boards(owner_id);

-- columns
CREATE INDEX idx_columns_board ON columns(board_id);

-- labels
CREATE INDEX idx_labels_project ON labels(project_id);

-- versions
CREATE INDEX idx_versions_project ON versions(project_id);
CREATE INDEX idx_versions_status  ON versions(status);

-- issue_categories
CREATE INDEX idx_categories_project ON issue_categories(project_id);

-- tasks
CREATE INDEX idx_tasks_project   ON tasks(project_id);
CREATE INDEX idx_tasks_board     ON tasks(board_id);
CREATE INDEX idx_tasks_column    ON tasks(column_id);
CREATE INDEX idx_tasks_parent    ON tasks(parent_id);
CREATE INDEX idx_tasks_version   ON tasks(version_id);
CREATE INDEX idx_tasks_category  ON tasks(category_id);
CREATE INDEX idx_tasks_assignee  ON tasks(assignee_id);
CREATE INDEX idx_tasks_reporter  ON tasks(reporter_id);
CREATE INDEX idx_tasks_due_date  ON tasks(due_date);
CREATE INDEX idx_tasks_priority  ON tasks(priority);
CREATE INDEX idx_tasks_status    ON tasks(status);
CREATE INDEX idx_tasks_backlog   ON tasks(project_id) WHERE column_id IS NULL;

-- task_labels
CREATE INDEX idx_task_labels_task  ON task_labels(task_id);
CREATE INDEX idx_task_labels_label ON task_labels(label_id);

-- comments
CREATE INDEX idx_comments_task       ON comments(task_id);
CREATE INDEX idx_comments_user       ON comments(user_id);
CREATE INDEX idx_comments_created_at ON comments(created_at);

-- comment_mentions
CREATE INDEX idx_mentions_comment ON comment_mentions(comment_id);
CREATE INDEX idx_mentions_user    ON comment_mentions(user_id);

-- attachments
CREATE INDEX idx_attachments_task ON attachments(task_id);

-- task_watchers
CREATE INDEX idx_watchers_task ON task_watchers(task_id);
CREATE INDEX idx_watchers_user ON task_watchers(user_id);

-- task_dependencies
CREATE INDEX idx_dep_task    ON task_dependencies(task_id);
CREATE INDEX idx_dep_depends ON task_dependencies(depends_on_task_id);

-- time_tracking
CREATE INDEX idx_time_task ON time_tracking(task_id);
CREATE INDEX idx_time_user ON time_tracking(user_id);
CREATE INDEX idx_time_date ON time_tracking(work_date);

-- sprints
CREATE INDEX idx_sprints_project ON sprints(project_id);
CREATE INDEX idx_sprints_status  ON sprints(status);

-- sprint_tasks
CREATE INDEX idx_st_sprint ON sprint_tasks(sprint_id);
CREATE INDEX idx_st_task   ON sprint_tasks(task_id);

-- notifications
CREATE INDEX idx_notifications_user    ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_type    ON notifications(type);

-- activity_logs
CREATE INDEX idx_activity_user    ON activity_logs(user_id);
CREATE INDEX idx_activity_project ON activity_logs(project_id);
CREATE INDEX idx_activity_entity  ON activity_logs(entity_type, entity_id);
CREATE INDEX idx_activity_created ON activity_logs(created_at);

-- webhooks
CREATE INDEX idx_webhooks_project ON webhooks(project_id);

-- ==================== TRIGGERS ====================
-- Lưu ý: updated_at được quản lý bởi @UpdateTimestamp (Hibernate) trong ứng dụng.
--        task_number được sinh tự động trong TaskService.
--        Nếu muốn dùng trigger DB thuần (không qua ứng dụng), chạy riêng file triggers.sql.

-- ==================== SEED DATA ====================

-- System admin (password: Admin@123)
INSERT INTO users (id, username, email, password_hash, full_name, is_active, email_verified, must_change_password)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@taskoryx.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8UeVrS9NdHsaLSwyHW',
    'System Administrator',
    TRUE, TRUE, FALSE
);

-- System roles
INSERT INTO roles (id, name, description, is_system_role) VALUES
    ('00000000-0000-0000-0001-000000000001', 'ROLE_ADMIN', 'Quản trị viên hệ thống', TRUE),
    ('00000000-0000-0000-0001-000000000002', 'ROLE_USER',  'Người dùng thông thường',  TRUE);

-- Gán admin role cho tài khoản admin
INSERT INTO user_roles (user_id, role_id)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000001');

-- ==================== 28. PROJECT_ROLES ====================
CREATE TABLE IF NOT EXISTS project_roles (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    permissions TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_role_name UNIQUE (project_id, name)
);

CREATE INDEX IF NOT EXISTS idx_project_roles_project ON project_roles(project_id);

-- ==================== 29. USER_PROJECT_PERFORMANCE ====================
CREATE TABLE IF NOT EXISTS user_project_performance (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id           UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    sprint_id            UUID         REFERENCES sprints(id) ON DELETE SET NULL,
    on_time_score        NUMERIC(5,2),
    completion_score     NUMERIC(5,2),
    time_accuracy_score  NUMERIC(5,2),
    engagement_score     NUMERIC(5,2),
    total_score          NUMERIC(5,2),
    rank                 INTEGER,
    task_count           INTEGER,
    completed_count      INTEGER,
    overdue_count        INTEGER,
    evaluated_at         TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_perf_project ON user_project_performance(project_id);
CREATE INDEX IF NOT EXISTS idx_perf_user    ON user_project_performance(user_id);
CREATE INDEX IF NOT EXISTS idx_perf_sprint  ON user_project_performance(sprint_id);

-- Partial unique index: đảm bảo mỗi user chỉ có 1 record project-level (sprint_id IS NULL)
-- Hibernate không tự tạo được partial index, phải chạy thủ công
CREATE UNIQUE INDEX IF NOT EXISTS uq_perf_project_level
    ON user_project_performance (user_id, project_id)
    WHERE sprint_id IS NULL;

-- ==================== END ====================
-- Schema version : 3.0.0
-- Entities       : 27 (thêm ProjectRole, UserProjectPerformance)
-- Cập nhật       : thêm board_type + owner_id (boards), mapped_status (columns),
--                  parent_id + version_id + category_id (tasks),
--                  two_factor_* (users), bảng sprints/sprint_tasks,
--                  task_watchers, versions, issue_categories, webhooks,
--                  project_templates, roles, permissions, role_permissions, user_roles,
--                  project_roles, user_project_performance
