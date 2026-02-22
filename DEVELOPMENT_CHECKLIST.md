# ✅ Taskoryx - Development Checklist

## 📋 Tiến độ phát triển

Sử dụng checklist này để theo dõi tiến độ phát triển dự án.

---

## 🗄️ Phase 1: Database & Entities (MVP)

### Database Setup
- [x] Tạo `database-init.sql` - Script khởi tạo database
- [x] Tạo `schema.sql` - Schema với 15 tables
- [x] Tạo indexes cho performance
- [x] Tạo triggers (auto-update timestamps, task numbering)
- [x] Insert sample data (admin user, sample project)

### Entity Classes
- [x] User.java
- [x] Project.java
- [x] ProjectMember.java
- [x] Board.java
- [x] Column.java
- [x] Task.java
- [x] Comment.java
- [x] CommentMention.java
- [x] Label.java
- [x] TaskLabel.java
- [x] Attachment.java
- [ ] Notification.java
- [ ] ActivityLog.java
- [ ] TimeTracking.java
- [ ] TaskDependency.java

### Documentation
- [x] DATABASE_DESIGN.md - Chi tiết thiết kế DB
- [x] ERD.md - Entity Relationship Diagram
- [x] SETUP_GUIDE.md - Hướng dẫn setup
- [x] README.md - Overview

---

## 🏗️ Phase 2: Core Backend Structure

### Repository Layer
- [ ] UserRepository.java
- [ ] ProjectRepository.java
- [ ] ProjectMemberRepository.java
- [ ] BoardRepository.java
- [ ] ColumnRepository.java
- [ ] TaskRepository.java
- [ ] CommentRepository.java
- [ ] LabelRepository.java
- [ ] AttachmentRepository.java

### Service Layer
- [ ] UserService.java
  - [ ] Create user
  - [ ] Update user
  - [ ] Delete user
  - [ ] Find user by email/username
  - [ ] Change password

- [ ] ProjectService.java
  - [ ] Create project
  - [ ] Update project
  - [ ] Delete project (soft delete - archive)
  - [ ] Get user's projects
  - [ ] Add member to project
  - [ ] Remove member
  - [ ] Update member role

- [ ] TaskService.java
  - [ ] Create task (auto-generate task number)
  - [ ] Update task
  - [ ] Delete task
  - [ ] Move task to column
  - [ ] Assign task to user
  - [ ] Update task position (reorder)
  - [ ] Get tasks by column
  - [ ] Get tasks by assignee
  - [ ] Get overdue tasks

- [ ] BoardService.java
  - [ ] Create board
  - [ ] Update board
  - [ ] Delete board
  - [ ] Get boards by project
  - [ ] Create default columns

- [ ] CommentService.java
  - [ ] Create comment
  - [ ] Update comment
  - [ ] Delete comment
  - [ ] Reply to comment
  - [ ] Extract mentions (@username)

- [ ] AttachmentService.java
  - [ ] Upload file
  - [ ] Delete file
  - [ ] Get file URL
  - [ ] Validate file size/type

### DTO Classes

#### Request DTOs
- [ ] RegisterRequest.java
- [ ] LoginRequest.java
- [ ] CreateProjectRequest.java
- [ ] UpdateProjectRequest.java
- [ ] CreateTaskRequest.java
- [ ] UpdateTaskRequest.java
- [ ] MoveTaskRequest.java
- [ ] CreateCommentRequest.java
- [ ] UploadFileRequest.java

#### Response DTOs
- [ ] AuthResponse.java (JWT token)
- [ ] UserResponse.java
- [ ] ProjectResponse.java
- [ ] TaskResponse.java
- [ ] CommentResponse.java
- [ ] ErrorResponse.java

---

## 🔐 Phase 3: Security & Authentication

### Security Configuration
- [ ] SecurityConfig.java
  - [ ] Configure HTTP security
  - [ ] Define public endpoints
  - [ ] Enable CORS
  - [ ] Disable CSRF for API

- [ ] JwtTokenProvider.java
  - [ ] Generate JWT token
  - [ ] Validate JWT token
  - [ ] Extract username from token
  - [ ] Get user details from token

- [ ] JwtAuthenticationFilter.java
  - [ ] Intercept requests
  - [ ] Validate JWT in header
  - [ ] Set authentication in context

- [ ] UserDetailsServiceImpl.java
  - [ ] Load user by username
  - [ ] Map User entity to UserDetails

### Authentication Controller
- [ ] AuthController.java
  - [ ] POST /auth/register
  - [ ] POST /auth/login
  - [ ] POST /auth/refresh
  - [ ] POST /auth/logout

### Password Management
- [ ] BCryptPasswordEncoder configuration
- [ ] Password validation rules
- [ ] Forgot password functionality (future)

---

## 🎮 Phase 4: REST API Controllers

### UserController.java
- [ ] GET /api/users - Get all users (admin only)
- [ ] GET /api/users/{id} - Get user by ID
- [ ] GET /api/users/me - Get current user
- [ ] PUT /api/users/{id} - Update user
- [ ] DELETE /api/users/{id} - Delete user
- [ ] PUT /api/users/{id}/password - Change password

### ProjectController.java
- [ ] GET /api/projects - Get user's projects
- [ ] POST /api/projects - Create project
- [ ] GET /api/projects/{id} - Get project details
- [ ] PUT /api/projects/{id} - Update project
- [ ] DELETE /api/projects/{id} - Archive project
- [ ] GET /api/projects/{id}/members - Get members
- [ ] POST /api/projects/{id}/members - Add member
- [ ] DELETE /api/projects/{id}/members/{userId} - Remove member
- [ ] PUT /api/projects/{id}/members/{userId}/role - Update role

### BoardController.java
- [ ] GET /api/projects/{projectId}/boards - Get boards
- [ ] POST /api/projects/{projectId}/boards - Create board
- [ ] GET /api/boards/{id} - Get board with columns
- [ ] PUT /api/boards/{id} - Update board
- [ ] DELETE /api/boards/{id} - Delete board

### ColumnController.java
- [ ] GET /api/boards/{boardId}/columns - Get columns
- [ ] POST /api/boards/{boardId}/columns - Create column
- [ ] PUT /api/columns/{id} - Update column
- [ ] DELETE /api/columns/{id} - Delete column
- [ ] PUT /api/columns/{id}/position - Reorder column

### TaskController.java
- [ ] GET /api/projects/{projectId}/tasks - Get all tasks
- [ ] GET /api/columns/{columnId}/tasks - Get tasks by column
- [ ] POST /api/projects/{projectId}/tasks - Create task
- [ ] GET /api/tasks/{id} - Get task details
- [ ] PUT /api/tasks/{id} - Update task
- [ ] DELETE /api/tasks/{id} - Delete task
- [ ] PUT /api/tasks/{id}/move - Move to column
- [ ] PUT /api/tasks/{id}/assign - Assign user
- [ ] PUT /api/tasks/{id}/position - Reorder task

### CommentController.java
- [ ] GET /api/tasks/{taskId}/comments - Get comments
- [ ] POST /api/tasks/{taskId}/comments - Create comment
- [ ] PUT /api/comments/{id} - Update comment
- [ ] DELETE /api/comments/{id} - Delete comment
- [ ] POST /api/comments/{id}/reply - Reply to comment

### LabelController.java
- [ ] GET /api/projects/{projectId}/labels - Get labels
- [ ] POST /api/projects/{projectId}/labels - Create label
- [ ] PUT /api/labels/{id} - Update label
- [ ] DELETE /api/labels/{id} - Delete label
- [ ] POST /api/tasks/{taskId}/labels/{labelId} - Add label
- [ ] DELETE /api/tasks/{taskId}/labels/{labelId} - Remove label

### AttachmentController.java
- [ ] GET /api/tasks/{taskId}/attachments - Get attachments
- [ ] POST /api/tasks/{taskId}/attachments - Upload file
- [ ] DELETE /api/attachments/{id} - Delete file
- [ ] GET /api/attachments/{id}/download - Download file

---

## ⚙️ Phase 5: Configuration & Utils

### Configuration Classes
- [ ] CorsConfig.java - CORS configuration
- [ ] SwaggerConfig.java - API documentation
- [ ] WebConfig.java - Web MVC configuration
- [ ] JacksonConfig.java - JSON serialization

### Exception Handling
- [ ] GlobalExceptionHandler.java
  - [ ] Handle validation errors
  - [ ] Handle authentication errors
  - [ ] Handle authorization errors
  - [ ] Handle not found errors
  - [ ] Handle database errors
  - [ ] Handle file upload errors

- [ ] Custom Exceptions
  - [ ] ResourceNotFoundException.java
  - [ ] UnauthorizedException.java
  - [ ] BadRequestException.java
  - [ ] DuplicateResourceException.java
  - [ ] FileUploadException.java

### Utility Classes
- [ ] ValidationUtils.java
- [ ] DateUtils.java
- [ ] FileUtils.java
- [ ] StringUtils.java

---

## 🧪 Phase 6: Testing

### Unit Tests

#### Service Tests
- [ ] UserServiceTest.java
- [ ] ProjectServiceTest.java
- [ ] TaskServiceTest.java
- [ ] BoardServiceTest.java
- [ ] CommentServiceTest.java

#### Repository Tests
- [ ] UserRepositoryTest.java
- [ ] ProjectRepositoryTest.java
- [ ] TaskRepositoryTest.java

### Integration Tests

#### Controller Tests
- [ ] AuthControllerTest.java
- [ ] UserControllerTest.java
- [ ] ProjectControllerTest.java
- [ ] TaskControllerTest.java

#### Security Tests
- [ ] JwtTokenProviderTest.java
- [ ] SecurityConfigTest.java

### Test Coverage
- [ ] Minimum 70% code coverage
- [ ] All critical paths tested
- [ ] Edge cases covered

---

## 📚 Phase 7: Documentation

### API Documentation
- [ ] Swagger/OpenAPI annotations
- [ ] API examples
- [ ] Request/response schemas
- [ ] Error codes documentation

### Code Documentation
- [ ] Javadoc for all public methods
- [ ] Class-level documentation
- [ ] Package documentation
- [ ] README updates

### User Documentation
- [ ] API usage guide
- [ ] Authentication guide
- [ ] Common use cases
- [ ] Troubleshooting guide

---

## 🚀 Phase 8: Deployment Preparation

### Environment Configuration
- [ ] Development environment (application-dev.yaml)
- [ ] Production environment (application-prod.yaml)
- [ ] Environment variables (.env)
- [ ] Sensitive data management

### Database Migration
- [ ] Flyway/Liquibase setup
- [ ] Migration scripts
- [ ] Rollback procedures

### Containerization
- [ ] Dockerfile for backend
- [ ] docker-compose.yml
- [ ] Docker Hub repository

### CI/CD
- [ ] GitHub Actions workflow
- [ ] Automated testing
- [ ] Automated deployment
- [ ] Code quality checks

### Monitoring & Logging
- [ ] Application logging (SLF4J)
- [ ] Performance monitoring
- [ ] Error tracking
- [ ] Health checks

---

## 🎯 Phase 9: Advanced Features

### Notifications
- [ ] NotificationService.java
- [ ] Email notifications
- [ ] In-app notifications
- [ ] Notification preferences

### Activity Logging
- [ ] ActivityLogService.java
- [ ] Log all CRUD operations
- [ ] Log user actions
- [ ] Activity timeline

### Time Tracking
- [ ] TimeTrackingService.java
- [ ] Log work hours
- [ ] Calculate total time
- [ ] Time reports

### Search & Filter
- [ ] Advanced search
- [ ] Filter by multiple criteria
- [ ] Full-text search
- [ ] Saved filters

### File Storage
- [ ] Local file storage
- [ ] AWS S3 integration
- [ ] File compression
- [ ] Image thumbnails

---

## 🔧 Phase 10: Optimization

### Performance
- [ ] Database query optimization
- [ ] Add database indexes
- [ ] Implement caching (Redis)
- [ ] Lazy loading optimization
- [ ] Pagination for large datasets

### Security Hardening
- [ ] Rate limiting
- [ ] Input sanitization
- [ ] SQL injection prevention
- [ ] XSS prevention
- [ ] CSRF protection

### Code Quality
- [ ] Code review
- [ ] Refactoring
- [ ] Remove code duplication
- [ ] Follow SOLID principles
- [ ] SonarQube analysis

---

## 📊 Progress Tracking

### Overall Progress

| Phase | Status | Progress |
|-------|--------|----------|
| Phase 1: Database & Entities | 🚧 In Progress | 90% |
| Phase 2: Core Backend | ⏳ Pending | 0% |
| Phase 3: Security | ⏳ Pending | 0% |
| Phase 4: REST API | ⏳ Pending | 0% |
| Phase 5: Configuration | ⏳ Pending | 0% |
| Phase 6: Testing | ⏳ Pending | 0% |
| Phase 7: Documentation | 🚧 In Progress | 40% |
| Phase 8: Deployment | ⏳ Pending | 0% |
| Phase 9: Advanced Features | ⏳ Pending | 0% |
| Phase 10: Optimization | ⏳ Pending | 0% |

### Legend
- ✅ Completed
- 🚧 In Progress
- ⏳ Pending
- ❌ Blocked

---

## 📝 Notes

### Current Sprint Goals
1. Hoàn thành tất cả Entity classes
2. Tạo Repository interfaces
3. Implement UserService và AuthService
4. Setup JWT authentication

### Known Issues
- [ ] None yet

### Tech Debt
- [ ] None yet

---

## 🎯 Next Steps

1. Hoàn thành các Entity còn lại (Notification, ActivityLog, TimeTracking, TaskDependency)
2. Tạo tất cả Repository interfaces
3. Implement Service layer
4. Setup JWT authentication
5. Tạo REST Controllers
6. Write tests

---

**Last Updated**: 2025-02-05
**Version**: 1.0.0
