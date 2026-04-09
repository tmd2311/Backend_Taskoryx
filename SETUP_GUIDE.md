# 🚀 Taskoryx – Hướng dẫn Triển khai (Backend + Frontend)

> Cập nhật: 2026-03-24

## 📚 Mục lục

**Backend**
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

**Frontend**
11. [Yêu cầu & Cài đặt Frontend](#11-yêu-cầu--cài-đặt-frontend)
12. [Cấu hình môi trường FE](#12-cấu-hình-môi-trường-fe)
13. [Cấu hình Axios & Auth](#13-cấu-hình-axios--auth)
14. [Tích hợp WebSocket](#14-tích-hợp-websocket)
15. [Tích hợp Time Tracking & Thống kê](#15-tích-hợp-time-tracking--thống-kê)
16. [Troubleshooting chung](#16-troubleshooting-chung)

---

## 🎯 Giới thiệu

Taskoryx là hệ thống quản lý task (Task Management System) giống như Trello/Jira, được xây dựng bằng:

- **Backend**: Spring Boot 3.2.1, Java 17
- **Database**: PostgreSQL 14+
- **ORM**: Spring Data JPA (Hibernate)
- **Security**: Spring Security + JWT
- **Documentation**: Swagger/OpenAPI 2.3.0
- **Real-time**: STOMP WebSocket (SockJS)
- **Email**: Spring Mail + Thymeleaf templates
- **2FA**: TOTP (Google Authenticator compatible)

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

---

# Frontend Deployment Guide

---

## 11. Yêu cầu & Cài đặt Frontend

### Yêu cầu
- **Node.js**: 18+ (LTS)
- **npm**: 9+ hoặc **yarn** 1.22+
- **Backend** đang chạy ở `http://localhost:8080`

### Kiểm tra phiên bản
```bash
node -v    # v18.x.x trở lên
npm -v     # 9.x.x trở lên
```

### Tạo project React (nếu chưa có)
```bash
# Vite + React (khuyên dùng)
npm create vite@latest taskoryx-fe -- --template react
cd taskoryx-fe
npm install

# Hoặc Next.js
npx create-next-app@latest taskoryx-fe
```

### Cài đặt thư viện cần thiết
```bash
# HTTP client
npm install axios

# WebSocket (real-time)
npm install @stomp/stompjs sockjs-client

# Biểu đồ thống kê
npm install recharts

# UI (tùy chọn – dùng 1 trong các thư viện sau)
npm install @shadcn/ui        # shadcn/ui
npm install antd               # Ant Design
npm install @mui/material      # Material UI

# Quản lý state (tùy chọn)
npm install zustand            # Zustand (nhẹ, đơn giản)
npm install @tanstack/react-query  # React Query (server state)

# Date handling
npm install dayjs
```

---

## 12. Cấu hình môi trường FE

Tạo file `.env` (Vite) hoặc `.env.local` (Next.js):

```env
# Vite
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=http://localhost:8080/api/ws

# Next.js
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_WS_URL=http://localhost:8080/api/ws
```

> Khi deploy production, thay `localhost:8080` bằng domain thực tế.

---

## 13. Cấu hình Axios & Auth

### File `src/api/axios.js`
```js
import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,   // hoặc process.env.NEXT_PUBLIC_API_URL
  headers: { 'Content-Type': 'application/json' },
})

// Tự động gắn accessToken
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Tự động refresh khi hết hạn (401)
let isRefreshing = false
let queue = []

api.interceptors.response.use(
  (res) => res.data,   // unwrap: trả về { success, data, message }
  async (err) => {
    const original = err.config
    if (err.response?.status === 401 && !original._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          queue.push({ resolve, reject })
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`
          return api(original)
        })
      }
      original._retry = true
      isRefreshing = true
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        const { data } = await axios.post(
          `${import.meta.env.VITE_API_URL}/auth/refresh`,
          { refreshToken }
        )
        localStorage.setItem('accessToken', data.accessToken)
        queue.forEach(({ resolve }) => resolve(data.accessToken))
        queue = []
        original.headers.Authorization = `Bearer ${data.accessToken}`
        return api(original)
      } catch {
        queue.forEach(({ reject }) => reject())
        queue = []
        localStorage.clear()
        window.location.href = '/login'
      } finally {
        isRefreshing = false
      }
    }
    // Lấy message lỗi từ backend
    const message = err.response?.data?.message || 'Đã có lỗi xảy ra'
    return Promise.reject(new Error(message))
  }
)

export default api
```

### Sử dụng API
```js
// Đăng nhập
const login = async (email, password) => {
  const result = await api.post('/auth/login', { email, password })
  // result = { success, data: { accessToken, refreshToken, user } }
  localStorage.setItem('accessToken', result.data.accessToken)
  localStorage.setItem('refreshToken', result.data.refreshToken)
  return result.data.user
}

// Lấy task
const getTask = async (taskId) => {
  const result = await api.get(`/tasks/${taskId}`)
  return result.data  // TaskResponse
}
```

---

## 14. Tích hợp WebSocket

### File `src/hooks/useWebSocket.js`
```js
import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function useWebSocket({ projectId, onNotification, onProjectUpdate }) {
  const clientRef = useRef(null)

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) return

    const client = new Client({
      webSocketFactory: () =>
        new SockJS(import.meta.env.VITE_WS_URL),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        // Thông báo cá nhân
        client.subscribe('/user/queue/notifications', (msg) => {
          onNotification?.(JSON.parse(msg.body))
        })
        // Cập nhật real-time project
        if (projectId) {
          client.subscribe(`/topic/project/${projectId}`, (msg) => {
            onProjectUpdate?.(JSON.parse(msg.body))
          })
        }
      },
    })

    client.activate()
    clientRef.current = client

    return () => client.deactivate()
  }, [projectId])

  return clientRef
}
```

### Sử dụng trong component
```jsx
function KanbanBoard({ projectId }) {
  const [tasks, setTasks] = useState([])

  useWebSocket({
    projectId,
    onNotification: (notif) => toast.info(notif.message),
    onProjectUpdate: (event) => {
      // Refresh khi có thay đổi (task tạo mới, move, update...)
      if (['TASK_CREATED', 'TASK_UPDATED', 'TASK_MOVED'].includes(event.type)) {
        fetchTasks()
      }
    },
  })
  // ...
}
```

---

## 15. Tích hợp Time Tracking & Thống kê

### API calls

```js
// src/api/timeTracking.js
import api from './axios'

// --- Ghi nhận giờ ---
export const logTime = (data) => api.post('/time-entries', data)
// data: { taskId, hours, description, workDate }

export const updateTimeEntry = (id, data) => api.put(`/time-entries/${id}`, data)
export const deleteTimeEntry = (id) => api.delete(`/time-entries/${id}`)
export const getTaskTimeEntries = (taskId) => api.get(`/tasks/${taskId}/time-entries`)

// --- Thống kê ---
export const getDailyStats = (start, end) =>
  api.get('/time-entries/stats/daily', { params: { start, end } })

export const getWeeklyStats = (start, end) =>
  api.get('/time-entries/stats/weekly', { params: { start, end } })

export const getMonthlyStats = (year) =>
  api.get('/time-entries/stats/monthly', { params: { year } })

export const getSummaryStats = (start, end) =>
  api.get('/time-entries/stats/summary', { params: { start, end } })

export const getProjectStats = (projectId, start, end) =>
  api.get(`/projects/${projectId}/time-entries/stats`, { params: { start, end } })
```

### Component biểu đồ ngày (Recharts)

```jsx
// src/components/TimeTracking/DailyHoursChart.jsx
import { BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from 'recharts'

export function DailyHoursChart({ data }) {
  // data = DailyTimeStatsResponse[]
  const chartData = data.map(d => ({
    label: d.date.slice(5),        // "03-24"
    hours: Number(d.totalHours),
    count: d.entryCount,
  }))

  return (
    <ResponsiveContainer width="100%" height={260}>
      <BarChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} />
        <XAxis dataKey="label" tick={{ fontSize: 12 }} />
        <YAxis unit="h" tick={{ fontSize: 12 }} />
        <Tooltip
          formatter={(v, _name, props) => [
            `${v}h (${props.payload.count} entries)`,
            'Giờ làm',
          ]}
        />
        <Bar dataKey="hours" fill="#3b82f6" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}
```

### Trang báo cáo thời gian

```jsx
// src/pages/TimeReportPage.jsx
import { useState, useEffect } from 'react'
import dayjs from 'dayjs'
import { getSummaryStats, getMonthlyStats } from '../api/timeTracking'
import { DailyHoursChart } from '../components/TimeTracking/DailyHoursChart'

export default function TimeReportPage() {
  const [tab, setTab] = useState('summary')  // 'summary' | 'monthly' | 'project'
  const [summary, setSummary] = useState(null)
  const [monthly, setMonthly] = useState([])
  const [year, setYear] = useState(dayjs().year())
  const [range, setRange] = useState({
    start: dayjs().startOf('month').format('YYYY-MM-DD'),
    end: dayjs().format('YYYY-MM-DD'),
  })

  useEffect(() => {
    if (tab === 'summary') {
      getSummaryStats(range.start, range.end)
        .then(r => setSummary(r.data))
    } else if (tab === 'monthly') {
      getMonthlyStats(year).then(r => setMonthly(r.data))
    }
  }, [tab, range, year])

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">Báo cáo thời gian</h1>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        {['summary', 'monthly', 'project'].map(t => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 rounded ${tab === t ? 'bg-blue-500 text-white' : 'bg-gray-100'}`}
          >
            {{ summary: 'Tổng hợp', monthly: 'Theo tháng', project: 'Theo project' }[t]}
          </button>
        ))}
      </div>

      {tab === 'summary' && summary && (
        <div>
          {/* Cards tổng hợp */}
          <div className="grid grid-cols-4 gap-4 mb-6">
            <StatCard label="Tổng giờ" value={summary.formattedTotalHours} />
            <StatCard label="Ngày có log" value={`${summary.activeDays} ngày`} />
            <StatCard label="TB/ngày active" value={`${summary.avgHoursPerActiveDay}h`} />
            <StatCard label="Số entries" value={summary.totalEntries} />
          </div>

          {/* Biểu đồ ngày */}
          <div className="bg-white rounded-lg p-4 shadow mb-6">
            <h3 className="font-semibold mb-3">Giờ làm theo ngày</h3>
            <DailyHoursChart data={summary.byDay} />
          </div>

          {/* Breakdown theo project */}
          <div className="bg-white rounded-lg p-4 shadow">
            <h3 className="font-semibold mb-3">Theo project</h3>
            {summary.byProject.map(p => (
              <div key={p.projectId} className="flex items-center gap-3 mb-2">
                <span className="font-mono text-sm bg-gray-100 px-2 py-0.5 rounded">
                  {p.projectKey}
                </span>
                <span className="flex-1">{p.projectName}</span>
                <span className="font-semibold">{p.formattedHours}</span>
                <div className="w-32 h-2 bg-gray-100 rounded overflow-hidden">
                  <div
                    className="h-full bg-blue-500 rounded"
                    style={{
                      width: `${(p.totalHours / summary.totalHours) * 100}%`
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {tab === 'monthly' && (
        <div>
          <div className="flex items-center gap-2 mb-4">
            <label>Năm:</label>
            <input
              type="number"
              value={year}
              onChange={e => setYear(Number(e.target.value))}
              className="border rounded px-2 py-1 w-24"
            />
          </div>
          <div className="grid grid-cols-4 gap-4">
            {monthly.map(m => (
              <div key={m.month} className="bg-white rounded-lg p-4 shadow">
                <div className="text-sm text-gray-500">{m.monthName}</div>
                <div className="text-xl font-bold">{m.formattedHours}</div>
                <div className="text-sm text-gray-400">
                  {m.activeDays} ngày • {m.entryCount} entries
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function StatCard({ label, value }) {
  return (
    <div className="bg-white rounded-lg p-4 shadow">
      <div className="text-sm text-gray-500">{label}</div>
      <div className="text-2xl font-bold mt-1">{value}</div>
    </div>
  )
}
```

---

## 16. Troubleshooting chung

### Backend

| Lỗi | Nguyên nhân | Giải pháp |
|-----|------------|-----------|
| `Connection refused` | PostgreSQL chưa chạy | `pg_ctl start` hoặc kiểm tra service |
| `Table already exists` | `ddl-auto: create` chạy lại | Đổi sang `validate` |
| `Lombok not working` | Chưa bật annotation processing | IDE → Enable AP |
| Port 8080 occupied | Port bị chiếm | `lsof -i :8080` rồi kill hoặc đổi port |

### Frontend

| Lỗi | Nguyên nhân | Giải pháp |
|-----|------------|-----------|
| CORS blocked | BE chưa allow origin FE | Kiểm tra `SecurityConfig.corsConfigurationSource()` |
| 401 liên tục | Token hết hạn & refresh thất bại | Kiểm tra `refreshToken` còn hạn (7 ngày) |
| WebSocket disconnect | Token hết hạn | Reconnect với token mới sau refresh |
| `data is undefined` | Quên unwrap response | API trả `{ success, data }` – phải lấy `result.data` |
| VITE env không đọc được | Thiếu prefix `VITE_` | Đổi thành `VITE_API_URL=...` |

### CORS – Whitelist thêm origin FE

Nếu FE chạy ở port khác (VD: 5173 – Vite mặc định), thêm vào `SecurityConfig.java`:
```java
config.setAllowedOrigins(List.of(
    "http://localhost:3000",
    "http://localhost:5173",
    "https://your-production-domain.com"
));
```

---

**Happy Coding! 🚀**
