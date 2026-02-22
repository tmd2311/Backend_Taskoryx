-- ==================== TASKORYX DATABASE INITIALIZATION ====================
-- Run this script to initialize the PostgreSQL database

-- ==================== CREATE DATABASE ====================

-- For Development
CREATE DATABASE taskoryx_dev;

-- For Production
CREATE DATABASE taskoryx_prod;

-- ==================== CREATE USER (OPTIONAL) ====================

-- Create dedicated user for the application
CREATE USER taskoryx_user WITH PASSWORD 'change_this_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE taskoryx_dev TO taskoryx_user;
GRANT ALL PRIVILEGES ON DATABASE taskoryx_prod TO taskoryx_user;

-- ==================== CONNECT TO DATABASE ====================
-- \c taskoryx_dev

-- ==================== ENABLE EXTENSIONS ====================

-- UUID Extension (for generating UUIDs)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- pgcrypto Extension (for encryption)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==================== CREATE SCHEMAS (IF NEEDED) ====================

-- CREATE SCHEMA IF NOT EXISTS taskoryx;
-- SET search_path TO taskoryx, public;

-- ==================== INDEXES FOR PERFORMANCE ====================
-- Spring Boot JPA will create tables automatically based on @Entity classes
-- But you can add additional indexes here for performance optimization

-- Example indexes (uncomment after tables are created):

-- CREATE INDEX idx_users_email ON users(email);
-- CREATE INDEX idx_users_username ON users(username);
-- CREATE INDEX idx_tasks_status ON tasks(status);
-- CREATE INDEX idx_tasks_priority ON tasks(priority);
-- CREATE INDEX idx_tasks_due_date ON tasks(due_date);
-- CREATE INDEX idx_tasks_assigned_user ON tasks(assigned_user_id);
-- CREATE INDEX idx_tasks_created_by ON tasks(created_by_id);
-- CREATE INDEX idx_projects_owner ON projects(owner_id);

-- ==================== INITIAL DATA (OPTIONAL) ====================
-- You can add seed data here after tables are created

-- Example: Insert default roles
-- INSERT INTO roles (id, name, description) VALUES
--   (gen_random_uuid(), 'ROLE_ADMIN', 'Administrator role'),
--   (gen_random_uuid(), 'ROLE_USER', 'Regular user role'),
--   (gen_random_uuid(), 'ROLE_MANAGER', 'Project manager role');

-- ==================== VERIFICATION QUERIES ====================
-- Run these to verify your database setup

-- List all databases
-- SELECT datname FROM pg_database WHERE datistemplate = false;

-- List all tables in current database
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';

-- Check database size
-- SELECT pg_database.datname, pg_size_pretty(pg_database_size(pg_database.datname))
-- FROM pg_database;

-- ==================== BACKUP COMMANDS ====================
-- Regular backup (run from command line, not in psql)

-- Backup database:
-- pg_dump -U postgres -d taskoryx_prod -F c -f taskoryx_backup.dump

-- Restore database:
-- pg_restore -U postgres -d taskoryx_prod -c taskoryx_backup.dump

-- ==================== MAINTENANCE ====================
-- Run these periodically for database health

-- Vacuum and analyze
-- VACUUM ANALYZE;

-- Reindex all tables
-- REINDEX DATABASE taskoryx_prod;

-- ==================== MONITORING QUERIES ====================

-- Active connections
-- SELECT count(*) FROM pg_stat_activity WHERE datname = 'taskoryx_prod';

-- Table sizes
-- SELECT
--   schemaname,
--   tablename,
--   pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
-- FROM pg_tables
-- WHERE schemaname = 'public'
-- ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- ==================== END OF SCRIPT ====================
