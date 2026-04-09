# Taskoryx – Hướng dẫn tích hợp Frontend

> Base URL: `http://localhost:8080/api`
> Swagger UI: `http://localhost:8080/api/swagger-ui.html`
> Tất cả request cần header: `Authorization: Bearer <accessToken>` (trừ các endpoint công khai)

---

## Mục lục

1. [Cấu hình Axios / Fetch](#1-cấu-hình-axios--fetch)
2. [Xác thực (Authentication)](#2-xác-thực-authentication)
3. [Quản lý Project & Board](#3-quản-lý-project--board)
4. [Quản lý Task & Subtask](#4-quản-lý-task--subtask)
5. [Kanban – Kéo thả](#5-kanban--kéo-thả)
6. [Sprint](#6-sprint)
7. [Checklist](#7-checklist)
8. [Time Tracking – Ghi giờ](#8-time-tracking--ghi-giờ)
9. [Time Tracking – Thống kê](#9-time-tracking--thống-kê)
10. [Bình luận & @Mention](#10-bình-luận--mention)
11. [Thông báo & WebSocket](#11-thông-báo--websocket)
12. [Dashboard & Gantt](#12-dashboard--gantt)
13. [Xác thực 2 yếu tố (2FA)](#13-xác-thực-2-yếu-tố-2fa)
14. [Cấu trúc Response lỗi](#14-cấu-trúc-response-lỗi)
15. [File đính kèm – Hiển thị & Upload](#15-file-đính-kèm--hiển-thị--upload)
16. [File đính kèm trong Comment](#16-file-đính-kèm-trong-comment)
17. [Admin – Quản lý người dùng](#17-admin--quản-lý-người-dùng)
18. [Admin – Quản lý chức vụ (Role)](#18-admin--quản-lý-chức-vụ-role)

---

## 1. Cấu hình Axios / Fetch

```js
// api/axios.js
import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
})

// Gắn accessToken tự động vào mỗi request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Tự động refresh token khi hết hạn (401)
api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config
    if (err.response?.status === 401 && !original._retry) {
      original._retry = true
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        const { data } = await axios.post('/api/auth/refresh', { refreshToken })
        localStorage.setItem('accessToken', data.accessToken)
        original.headers.Authorization = `Bearer ${data.accessToken}`
        return api(original)
      } catch {
        localStorage.clear()
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export default api
```

---

## 2. Xác thực (Authentication)

> **Lưu ý:** Không có endpoint tự đăng ký. Tài khoản **chỉ được tạo bởi Admin** — xem [Section 17](#17-admin--quản-lý-người-dùng).

### Đăng nhập
```http
POST /auth/login
{ "email": "a@example.com", "password": "password123" }
```

**Response:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "userId": "uuid",
  "email": "a@example.com",
  "fullName": "Nguyễn Văn A",
  "mustChangePassword": false
}
```

```js
// Lưu token sau khi login
const { data } = await api.post('/auth/login', { email, password })
localStorage.setItem('accessToken', data.data.accessToken)
localStorage.setItem('refreshToken', data.data.refreshToken)

// Kiểm tra bắt buộc đổi mật khẩu
if (data.data.mustChangePassword) {
  navigate('/change-password')   // redirect bắt buộc, chặn vào app chính
}
```

### Xử lý `mustChangePassword`

Khi Admin tạo tài khoản mới hoặc reset mật khẩu, `mustChangePassword = true`.
Sau khi đăng nhập, FE **bắt buộc** redirect sang trang đổi mật khẩu:

```js
// src/router/ProtectedRoute.jsx
export function ProtectedRoute({ children }) {
  const token = localStorage.getItem('accessToken')
  const user  = useAuthStore(s => s.user)          // lấy từ store sau khi login

  if (!token) return <Navigate to="/login" replace />
  if (user?.mustChangePassword) return <Navigate to="/change-password" replace />
  return children
}
```

```js
// Đổi mật khẩu (sau khi admin tạo tài khoản / reset)
PUT /users/me/password
{
  "currentPassword": "<mật_khẩu_tạm_từ_email>",
  "newPassword": "MatKhauMoi@123",
  "confirmPassword": "MatKhauMoi@123"
}
// Sau khi thành công: mustChangePassword → false, redirect vào app
```

### Đăng xuất
```http
POST /auth/logout   // JWT stateless — client tự xóa token
```
```js
localStorage.removeItem('accessToken')
localStorage.removeItem('refreshToken')
```

---

## 3. Quản lý Project & Board

### Tạo & lấy danh sách project
```http
POST /projects        { "name": "Dự án A", "key": "DA", "description": "..." }
GET  /projects        // danh sách project của user
GET  /projects/{id}
PUT  /projects/{id}
DELETE /projects/{id}
```

### Thành viên

```http
GET    /projects/{id}/members
POST   /projects/{id}/members
PUT    /projects/{id}/members/{userId}/role
DELETE /projects/{id}/members/{userId}
```

**Role project** là chuỗi tự do, có 2 giá trị đặc biệt do hệ thống quản lý:
- `"OWNER"` — tự động gán khi tạo project, không thể đổi/xóa
- `"ADMIN"` — có quyền quản lý sprint, version, category trong dự án

#### Auto-assign role từ tài khoản

> Nếu user được thêm vào dự án **có role hệ thống** (VD: `PM`, `DEV`, `TESTER`...) thì backend **tự động gán role đó làm project role**, không cần truyền `role` trong request.
>
> Chỉ khi user **không có role hệ thống** nào thì mới cần truyền `role`.

**Ví dụ:**
```http
// User có system role "PM" → không cần truyền role
POST /projects/{id}/members
{ "email": "pm@example.com" }
// → project role tự động = "PM"

// User không có system role → bắt buộc truyền role
POST /projects/{id}/members
{ "email": "dev@example.com", "role": "DEVELOPER" }
```

#### Luồng UI khi thêm thành viên

```
1. Nhập email → gọi GET /users/search?keyword=... để tìm user
2. Hiển thị kết quả kèm system role của từng user
3. Nếu user chọn CÓ system role:
     → Hiển thị badge role (VD: "PM"), ẩn ô nhập role, gọi API không kèm `role`
4. Nếu user chọn KHÔNG CÓ system role:
     → Hiển thị ô nhập / dropdown chọn project role, bắt buộc điền
```

```jsx
// Ví dụ component thêm thành viên
function AddMemberForm({ projectId }) {
  const [selectedUser, setSelectedUser] = useState(null)
  const [role, setRole] = useState('')

  // user.systemRole được lấy từ API search user (xem mục 17)
  const hasSystemRole = !!selectedUser?.systemRole

  const handleSubmit = () => {
    const body = { email: selectedUser.email }
    if (!hasSystemRole) body.role = role
    api.post(`/projects/${projectId}/members`, body)
  }

  return (
    <form>
      <UserSearchInput onSelect={setSelectedUser} />
      {selectedUser && (
        hasSystemRole
          ? <Badge>Role tự động: {selectedUser.systemRole}</Badge>
          : <RoleInput value={role} onChange={setRole} required />
      )}
      <button onClick={handleSubmit}>Thêm</button>
    </form>
  )
}
```

**Response thành viên:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "username": "nguyenvana",
  "email": "a@example.com",
  "fullName": "Nguyễn Văn A",
  "avatarUrl": null,
  "role": "PM",
  "joinedAt": "2026-03-30T01:00:00"
}
```

**`currentUserRole` trong ProjectResponse** cũng là `String` (không phải enum):
```json
{
  "id": "uuid",
  "name": "Dự án A",
  "currentUserRole": "OWNER"
}
```

```js
// api/projectService.js
export const getMembers = (projectId) =>
  api.get(`/projects/${projectId}/members`).then(r => r.data.data)

// role là optional — nếu user có system role thì bỏ qua
export const addMember = (projectId, email, role) =>
  api.post(`/projects/${projectId}/members`, { email, ...(role && { role }) })
     .then(r => r.data.data)

export const updateMemberRole = (projectId, userId, role) =>
  api.put(`/projects/${projectId}/members/${userId}/role`, { role }).then(r => r.data.data)

export const removeMember = (projectId, userId) =>
  api.delete(`/projects/${projectId}/members/${userId}`)
```

### Board & Column
```http
POST /projects/{id}/boards   { "name": "Sprint Board", "boardType": "SCRUM" }
GET  /boards/{id}/kanban     // trả về board + columns + tasks đã sắp xếp
POST /boards/{id}/columns    { "name": "To Do", "position": 1000 }
PUT  /columns/{id}           { "name": "In Progress", "taskLimit": 5 }
DELETE /columns/{id}
```

> `boardType`: `KANBAN` (mặc định) | `SCRUM`
> - **KANBAN**: tasks đi thẳng vào column, không cần sprint
> - **SCRUM**: board gắn với sprint, tasks đi qua Backlog → Sprint → Board

---

## 4. Quản lý Task & Subtask

### Tạo task
```http
POST /projects/{projectId}/tasks
```
```json
{
  "title": "Thiết kế UI trang chủ",
  "description": "...",
  "priority": "HIGH",
  "assigneeId": "uuid",
  "startDate": "2026-03-20",
  "dueDate": "2026-03-30",
  "estimatedHours": 8,
  "boardId": "uuid",
  "columnId": "uuid",
  "labelIds": ["uuid1", "uuid2"],
  "versionId": "uuid",
  "categoryId": "uuid",
  "parentTaskId": null
}
```

> `priority`: `LOW` | `MEDIUM` | `HIGH` | `URGENT`
> `boardId` + `columnId` để gắn vào Kanban; để `null` thì task vào Backlog

### Tạo subtask (task con)
```json
{
  "title": "Vẽ wireframe",
  "parentTaskId": "uuid-cua-task-cha"
}
```
- Task con phải cùng project với task cha
- Không giới hạn số lượng task con
- Task con **có thể** gắn vào board/column riêng

### Lấy chi tiết task theo ID hoặc taskKey

```http
GET /tasks/{id}               // theo UUID
GET /tasks/key/{taskKey}      // theo taskKey (VD: PROJ-123) — dùng cho URL thân thiện
```

> Cả hai endpoint trả về cùng `TaskResponse`. Dùng `GET /tasks/key/{taskKey}` khi URL là `/tasks/PROJ-123`.

```js
// taskService.ts
export const getTaskById  = (id)      => api.get(`/tasks/${id}`).then(r => r.data.data)
export const getTaskByKey = (taskKey) => api.get(`/tasks/key/${taskKey}`).then(r => r.data.data)

// Trong TaskDetailPage.tsx — lấy taskKey từ URL params
const { taskKey } = useParams()   // VD: "PROJ-123"
const task = await getTaskByKey(taskKey)
// Nếu 404 → hiển thị trang lỗi "Không tìm thấy task"
```
```json
{
  "id": "uuid",
  "taskKey": "PROJ-1",
  "title": "Thiết kế UI trang chủ",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "parentTaskId": null,
  "parentTaskKey": null,
  "parentTaskTitle": null,
  "subTasks": [
    {
      "id": "uuid",
      "taskKey": "PROJ-2",
      "title": "Vẽ wireframe",
      "status": "TODO",
      "priority": "MEDIUM",
      "assigneeId": "uuid",
      "assigneeName": "Nguyễn Văn A"
    }
  ],
  "watcherCount": 2,
  "commentCount": 3,
  ...
}
```

### Gợi ý render subtask trên UI
```jsx
// TaskDetail.jsx
function TaskDetail({ task }) {
  return (
    <div>
      {task.parentTaskId && (
        <div className="parent-task">
          ↑ Task cha: <a href={`/tasks/${task.parentTaskId}`}>{task.parentTaskKey} {task.parentTaskTitle}</a>
        </div>
      )}

      {task.subTasks?.length > 0 && (
        <div className="subtasks">
          <h4>Subtasks ({task.subTasks.length})</h4>
          {task.subTasks.map(sub => (
            <SubTaskRow key={sub.id} subtask={sub} />
          ))}
        </div>
      )}
    </div>
  )
}
```

### Cập nhật task
```http
PUT /tasks/{id}
```
```json
{
  "title": "...",
  "priority": "URGENT",
  "parentTaskId": "uuid-moi",
  "clearParent": false
}
```
> Để gỡ task cha: `{ "clearParent": true }`

### Xóa / thay đổi trạng thái
```http
DELETE /tasks/{id}
PATCH  /tasks/{id}/status   { "status": "DONE" }
```
> `status`: `TODO` | `IN_PROGRESS` | `IN_REVIEW` | `RESOLVED` | `DONE` | `CANCELLED`

### Backlog & task của tôi
```http
GET /projects/{id}/backlog   // Product Backlog: tasks chưa có column VÀ chưa thuộc sprint nào
GET /sprints/{id}/backlog    // Sprint Backlog: tasks trong sprint chưa được kéo lên board
GET /tasks/my                // tasks được assign cho user hiện tại
```

**Phân biệt 3 trạng thái task:**

| Trạng thái | Điều kiện | API |
|---|---|---|
| Product Backlog | `column = null` + không trong sprint nào PLANNED/ACTIVE | `GET /projects/{id}/backlog` |
| Sprint Backlog | Trong sprint + `column = null` | `GET /sprints/{id}/backlog` |
| On Board | `column != null` | `GET /boards/{id}/kanban` |

**Luồng Scrum:**
```
Tạo task → Product Backlog
    ↓ POST /sprints/{id}/tasks
Sprint Backlog (PLANNED)
    ↓ POST /sprints/{id}/start
Sprint ACTIVE → kéo task lên board (PATCH /tasks/{id}/move)
    ↓ POST /sprints/{id}/complete
Tasks DONE giữ trong sprint | Tasks chưa xong → FE tự xử lý (gán sprint mới hoặc về backlog)
```

**Luồng Kanban:**
```
Tạo task với boardId + columnId → thẳng lên board → kéo qua các cột
```

---

## 5. Kanban – Kéo thả

```http
PATCH /tasks/{id}/move
```
```json
{
  "targetColumnId": "uuid-column",
  "newPosition": 2000.0
}
```

**Thuật toán tính `newPosition`:**
```js
// Kéo thả giữa 2 card: lấy trung bình vị trí trên + dưới
function calcPosition(above, below) {
  if (!above) return below.position / 2
  if (!below) return above.position + 1000
  return (above.position + below.position) / 2
}
```

> Chuyển về Backlog: `targetColumnId: null`

---

## 6. Sprint

```http
POST /projects/{id}/sprints
{
  "name": "Sprint 1",
  "goal": "Hoàn thành MVP",
  "startDate": "2026-03-20",
  "endDate": "2026-04-03",
  "boardId": "uuid"           // optional – board SCRUM gắn với sprint này
}

GET  /projects/{id}/sprints
GET  /sprints/{id}
PUT  /sprints/{id}
DELETE /sprints/{id}

POST /sprints/{id}/start      // bắt đầu sprint (chỉ 1 sprint ACTIVE/project)
POST /sprints/{id}/complete   // kết thúc sprint (tasks chưa xong vẫn giữ nguyên, FE tự xử lý)

POST   /sprints/{id}/tasks    { "taskId": "uuid" }   // thêm task vào sprint (từ Product Backlog)
DELETE /sprints/{id}/tasks/{taskId}                  // gỡ task khỏi sprint → về Product Backlog
GET    /sprints/{id}/backlog                         // Sprint Backlog: tasks trong sprint chưa lên board
```

> Sprint status: `PLANNED` → `ACTIVE` → `COMPLETED`

**SprintResponse** bao gồm:
```json
{
  "id": "uuid",
  "projectId": "uuid",
  "boardId": "uuid",
  "boardName": "Sprint Board",
  "name": "Sprint 1",
  "goal": "...",
  "status": "ACTIVE",
  "startDate": "2026-03-20",
  "endDate": "2026-04-03",
  "taskCount": 10,
  "completedTaskCount": 4,
  "inProgressTaskCount": 3
}
```

**Xử lý khi complete sprint (gợi ý FE):**
```js
// Sau khi POST /sprints/{id}/complete, lấy tasks chưa xong để hỏi user
const incompleteTasks = sprint.tasks.filter(t =>
  !['DONE', 'RESOLVED', 'CANCELLED'].includes(t.status)
)
// Hiển thị modal: "X tasks chưa hoàn thành – chuyển sang sprint tiếp theo hay về backlog?"
// → Gọi POST /sprints/{nextSprintId}/tasks hoặc DELETE /sprints/{id}/tasks/{taskId}
```

---

## 7. Checklist

```http
GET  /tasks/{id}/checklist
POST /tasks/{id}/checklist      { "content": "Viết unit test", "position": 1 }
POST /tasks/{id}/checklist/bulk { "items": [{"content": "Item 1"}, {"content": "Item 2"}] }

PUT    /checklist/{id}    { "content": "...", "checked": true }
DELETE /checklist/{id}
DELETE /tasks/{id}/checklist   // xóa tất cả
```

**Tính % hoàn thành:**
```js
const percent = items.length
  ? Math.round(items.filter(i => i.checked).length / items.length * 100)
  : 0
```

---

## 8. Time Tracking – Ghi giờ

### Log giờ làm việc
```http
POST /time-entries
{
  "taskId": "uuid",
  "hours": 2.5,
  "description": "Fix bug login",
  "workDate": "2026-03-16"
}
```
> `workDate` optional — mặc định ngày hôm nay. `hours`: 0.1 – 24.0

### Lấy danh sách
```http
GET  /tasks/{taskId}/time-entries              // log của task
GET  /tasks/{taskId}/time-entries/total        // tổng giờ của task (BigDecimal)
GET  /time-entries/my?page=0&size=20           // log của tôi (phân trang)
GET  /time-entries/range?start=2026-03-01&end=2026-03-31  // theo khoảng ngày
```

### Cập nhật / Xóa (chỉ người tạo)
```http
PUT    /time-entries/{id}   { "hours": 3.0, "description": "...", "workDate": "..." }
DELETE /time-entries/{id}
```

### TimeTrackingResponse
```json
{
  "id": "uuid",
  "taskId": "uuid",
  "taskKey": "PROJ-5",
  "taskTitle": "Implement Auth API",
  "userId": "uuid",
  "userName": "Nguyễn Văn A",
  "userAvatar": "https://...",
  "hours": 2.5,
  "formattedHours": "2h 30m",
  "description": "Fix bug login",
  "workDate": "2026-03-16",
  "createdAt": "2026-03-16T10:00:00"
}
```

---

## 9. Time Tracking – Thống kê

> Tất cả endpoints stats trả về dữ liệu của **user hiện tại** (từ JWT).
> Tham số `start`/`end` dạng `YYYY-MM-DD`. Nếu không truyền → mặc định 30 ngày gần nhất.

### 9.1 Thống kê theo ngày
```http
GET /time-entries/stats/daily?start=2026-03-01&end=2026-03-31
```

**Response** – mảng `DailyTimeStatsResponse`:
```json
[
  {
    "date": "2026-03-24",
    "dayOfWeek": "thứ hai",
    "totalHours": 6.5,
    "formattedHours": "6h 30m",
    "entryCount": 3,
    "entries": [ { /* TimeTrackingResponse */ } ]
  },
  {
    "date": "2026-03-25",
    "dayOfWeek": "thứ ba",
    "totalHours": 0,
    "formattedHours": "0h",
    "entryCount": 0,
    "entries": []
  }
]
```
> Mảng **bao gồm cả ngày không có entry** (totalHours = 0) → tiện vẽ biểu đồ liên tục.

### 9.2 Thống kê theo tuần
```http
GET /time-entries/stats/weekly?start=2026-03-01&end=2026-03-31
```

**Response** – mảng `WeeklyTimeStatsResponse`:
```json
[
  {
    "year": 2026,
    "weekOfYear": 13,
    "weekStart": "2026-03-23",
    "weekEnd": "2026-03-29",
    "weekLabel": "Tuần 13 (2026-03-23 - 2026-03-29)",
    "totalHours": 32.5,
    "formattedHours": "32h 30m",
    "entryCount": 15,
    "days": [ { /* DailyTimeStatsResponse */ } ]
  }
]
```

### 9.3 Thống kê theo tháng (trong năm)
```http
GET /time-entries/stats/monthly?year=2026
```

**Response** – mảng 12 phần tử `MonthlyTimeStatsResponse`:
```json
[
  {
    "year": 2026,
    "month": 3,
    "monthName": "tháng 3",
    "totalHours": 120.5,
    "formattedHours": "120h 30m",
    "entryCount": 55,
    "activeDays": 22,
    "days": [ { /* DailyTimeStatsResponse, null nếu tháng trống */ } ]
  }
]
```
> `days` là `null` nếu tháng đó không có entry nào (tránh load dư).

### 9.4 Tổng hợp (Summary)
```http
GET /time-entries/stats/summary?start=2026-03-01&end=2026-03-31
```

**Response** – `TimeStatsSummaryResponse`:
```json
{
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "totalHours": 168.5,
  "formattedTotalHours": "168h 30m",
  "totalEntries": 72,
  "activeDays": 22,
  "avgHoursPerActiveDay": 7.66,
  "avgHoursPerDay": 5.44,
  "byProject": [
    {
      "projectId": "uuid",
      "projectName": "Taskoryx",
      "projectKey": "TX",
      "totalHours": 120.0,
      "formattedHours": "120h",
      "entryCount": 50
    }
  ],
  "byDay": [ { /* DailyTimeStatsResponse */ } ]
}
```

### 9.5 Thống kê chi tiết project
```http
GET /projects/{projectId}/time-entries/stats?start=2026-03-01&end=2026-03-31
```

**Response** – `ProjectDetailTimeStatsResponse`:
```json
{
  "projectId": "uuid",
  "projectName": "Taskoryx",
  "projectKey": "TX",
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "totalHours": 320.5,
  "formattedTotalHours": "320h 30m",
  "totalEntries": 140,
  "byMember": [
    {
      "userId": "uuid",
      "userName": "Nguyễn Văn A",
      "userAvatar": "https://...",
      "totalHours": 80.0,
      "formattedHours": "80h",
      "entryCount": 32
    }
  ],
  "byTask": [
    {
      "taskId": "uuid",
      "taskKey": "TX-1",
      "taskTitle": "Thiết kế database",
      "taskStatus": "DONE",
      "estimatedHours": 16.0,
      "loggedHours": 18.5,
      "formattedLoggedHours": "18h 30m",
      "entryCount": 6,
      "progressPercent": 115.6
    }
  ],
  "byDay": [ { /* DailyTimeStatsResponse */ } ]
}
```
> `progressPercent`: % giờ đã log so với giờ ước tính. `null` nếu task không có `estimatedHours`.

### Gợi ý render biểu đồ (React + Recharts)

```jsx
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'

function DailyHoursChart({ data }) {
  const chartData = data.map(d => ({
    name: d.date.slice(5),          // "03-24"
    hours: Number(d.totalHours),
  }))

  return (
    <ResponsiveContainer width="100%" height={240}>
      <BarChart data={chartData}>
        <XAxis dataKey="name" />
        <YAxis unit="h" />
        <Tooltip formatter={(v) => [`${v}h`, 'Giờ làm']} />
        <Bar dataKey="hours" fill="#3b82f6" radius={[4,4,0,0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}

// Dùng với API daily stats
const { data } = await api.get('/time-entries/stats/daily', {
  params: { start: '2026-03-01', end: '2026-03-31' }
})
// data.data → mảng DailyTimeStatsResponse
```

### Gợi ý trang "Báo cáo thời gian"

```
┌────────────────────────────────────────────────────────┐
│  Tháng 3/2026          Tổng: 168h 30m  Ngày active: 22  │
├────────────────────────────────────────────────────────┤
│  [Biểu đồ cột theo ngày]                               │
├─────────────────┬──────────────────────────────────────┤
│  Theo Project   │  Project A  120h  ████████████░░░░    │
│                 │  Project B   48h  ████░░░░░░░░░░░░    │
├─────────────────┴──────────────────────────────────────┤
│  [Bảng chi tiết task: taskKey | title | logged | est%] │
└────────────────────────────────────────────────────────┘
```

---

## 10. Bình luận & @Mention

### Endpoints

```http
GET    /tasks/{id}/comments                              → Lấy danh sách (kèm replies lồng nhau)
POST   /tasks/{id}/comments                              → Đăng comment
PUT    /comments/{id}                                    → Sửa (mention tự re-process)
DELETE /comments/{id}                                    → Xóa
GET    /projects/{id}/members/search?keyword=<text>      → Search thành viên cho @mention
```

### CommentResponse

```json
{
  "id": "uuid",
  "userId": "uuid", "username": "tuan",
  "userFullName": "Nguyễn Tuấn", "userAvatar": "https://...",
  "content": "Nhờ @dung review nhé",
  "parentId": null, "isEdited": false,
  "replies": [],
  "mentionedUsernames": ["dung"],
  "mentionedUsers": [
    { "userId": "uuid", "username": "dung", "fullName": "Trần Dũng", "avatarUrl": "https://..." }
  ],
  "createdAt": "2026-03-24T10:00:00"
}
```

> `mentionedUsers` là danh sách đầy đủ — dùng để render mention chip/highlight.
> `mentionedUsernames` là `List<String>` — backward compat.

### Luồng @mention

```
User gõ "@" trong textarea
   → FE detect "@" + debounce 250ms
   → GET /projects/{id}/members/search?keyword=<text sau @>
   → Hiện dropdown (chỉ thành viên trong project)
   → User chọn → FE chèn "@username " vào textarea
   → Submit → content chứa "@username" → backend tự parse
```

### Hook `useMentionInput`

```js
// src/hooks/useMentionInput.js
import { useState, useRef, useCallback } from 'react'
import api from '../api/axios'

export function useMentionInput(projectId) {
  const [content, setContent]       = useState('')
  const [suggestions, setSuggestions] = useState([])
  const [showDropdown, setShowDropdown] = useState(false)
  const [mentionStart, setMentionStart] = useState(-1)
  const textareaRef = useRef(null)
  const debounceRef = useRef(null)

  const handleChange = useCallback((e) => {
    const value  = e.target.value
    const cursor = e.target.selectionStart
    setContent(value)

    const match = value.slice(0, cursor).match(/@(\w*)$/)
    if (match) {
      setMentionStart(cursor - match[0].length)
      setShowDropdown(true)
      clearTimeout(debounceRef.current)
      debounceRef.current = setTimeout(async () => {
        try {
          const res = await api.get(`/projects/${projectId}/members/search`,
            { params: { keyword: match[1] } })
          setSuggestions(res.data ?? [])
        } catch { setSuggestions([]) }
      }, 250)
    } else {
      setShowDropdown(false)
      setSuggestions([])
    }
  }, [projectId])

  const selectMention = useCallback((user) => {
    const ta     = textareaRef.current
    const before = content.slice(0, mentionStart)
    const after  = content.slice(ta.selectionStart)
    const ins    = `@${user.username} `
    setContent(before + ins + after)
    setShowDropdown(false)
    setSuggestions([])
    setTimeout(() => {
      const pos = mentionStart + ins.length
      ta.focus(); ta.setSelectionRange(pos, pos)
    }, 0)
  }, [content, mentionStart])

  return {
    content, setContent,
    suggestions, showDropdown, textareaRef,
    handleChange, selectMention,
    reset: () => { setContent(''); setShowDropdown(false); setSuggestions([]) },
  }
}
```

### Component `CommentInput`

```jsx
// src/components/Comment/CommentInput.jsx
import { useMentionInput } from '../../hooks/useMentionInput'
import api from '../../api/axios'

export function CommentInput({ taskId, projectId, parentId = null, onSuccess }) {
  const { content, suggestions, showDropdown,
          textareaRef, handleChange, selectMention, reset } = useMentionInput(projectId)

  const submit = async (e) => {
    e.preventDefault()
    if (!content.trim()) return
    await api.post(`/tasks/${taskId}/comments`, { content, parentId })
    reset(); onSuccess?.()
  }

  return (
    <form onSubmit={submit} style={{ position: 'relative' }}>
      <textarea ref={textareaRef} value={content} onChange={handleChange}
        placeholder="Bình luận… dùng @username để mention"
        rows={3} style={{ width: '100%', padding: 8, resize: 'vertical' }} />

      {/* Dropdown gợi ý */}
      {showDropdown && suggestions.length > 0 && (
        <ul style={{
          position: 'absolute', bottom: '100%', left: 0,
          background: '#fff', border: '1px solid #e5e7eb',
          borderRadius: 8, boxShadow: '0 4px 12px rgba(0,0,0,.12)',
          margin: 0, padding: 4, listStyle: 'none',
          minWidth: 240, maxHeight: 200, overflowY: 'auto', zIndex: 50,
        }}>
          {suggestions.map(u => (
            <li key={u.userId}
              onMouseDown={e => { e.preventDefault(); selectMention(u) }}
              style={{ display: 'flex', alignItems: 'center', gap: 8,
                       padding: '6px 10px', cursor: 'pointer', borderRadius: 6 }}
              onMouseEnter={e => e.currentTarget.style.background = '#f3f4f6'}
              onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
            >
              {u.avatarUrl
                ? <img src={u.avatarUrl} width={28} height={28}
                       style={{ borderRadius: '50%', flexShrink: 0 }} alt="" />
                : <div style={{
                    width: 28, height: 28, borderRadius: '50%',
                    background: '#3b82f6', color: '#fff', flexShrink: 0,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 12, fontWeight: 700,
                  }}>{u.fullName?.[0]?.toUpperCase()}</div>
              }
              <div>
                <div style={{ fontWeight: 600, fontSize: 14 }}>{u.fullName}</div>
                <div style={{ color: '#6b7280', fontSize: 12 }}>@{u.username}</div>
              </div>
            </li>
          ))}
        </ul>
      )}

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 6 }}>
        <button type="submit" disabled={!content.trim()}
          style={{ background: '#3b82f6', color: '#fff', border: 'none',
                   borderRadius: 6, padding: '6px 16px', cursor: 'pointer' }}>
          Gửi
        </button>
      </div>
    </form>
  )
}
```

### Component `CommentContent` – render mention highlight

```jsx
// src/components/Comment/CommentContent.jsx
export function CommentContent({ content, mentionedUsers = [] }) {
  const parts = []
  let remaining = content

  // Sắp xếp theo thứ tự xuất hiện trong string
  const sorted = [...mentionedUsers].sort((a, b) =>
    content.indexOf(`@${a.username}`) - content.indexOf(`@${b.username}`)
  )

  sorted.forEach(user => {
    const tag = `@${user.username}`
    const idx = remaining.indexOf(tag)
    if (idx === -1) return
    if (idx > 0) parts.push({ type: 'text', value: remaining.slice(0, idx) })
    parts.push({ type: 'mention', user })
    remaining = remaining.slice(idx + tag.length)
  })
  if (remaining) parts.push({ type: 'text', value: remaining })

  return (
    <span>
      {parts.map((part, i) =>
        part.type === 'text' ? <span key={i}>{part.value}</span> : (
          <span key={i} style={{
            display: 'inline-flex', alignItems: 'center', gap: 3,
            background: '#eff6ff', color: '#2563eb',
            borderRadius: 4, padding: '1px 6px',
            fontWeight: 600, fontSize: '0.9em',
          }}>
            {part.user.avatarUrl && (
              <img src={part.user.avatarUrl} width={14} height={14}
                   style={{ borderRadius: '50%' }} alt="" />
            )}
            @{part.user.username}
          </span>
        )
      )}
    </span>
  )
}
```

### Dùng trong TaskDetail

```jsx
import { CommentSection } from '../components/Comment/CommentSection'

<CommentSection
  taskId={task.id}
  projectId={task.projectId}
  currentUserId={currentUser.id}
/>
```

> `CommentSection` = gọi `GET /tasks/{id}/comments` + render list `CommentItem` + `CommentInput` ở dưới.
> `CommentItem` = 1 comment + action (Trả lời / Sửa / Xóa) + inline `CommentInput` để reply.

---

## 10b. Đính kèm (Attachment)

```http
POST /tasks/{id}/attachments   // multipart/form-data, field: "file"
GET  /tasks/{id}/attachments
DELETE /attachments/{id}
```
```js
const formData = new FormData()
formData.append('file', file)
await api.post(`/tasks/${taskId}/attachments`, formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
```
> Giới hạn: 10MB/file

---

## 11. Thông báo & WebSocket

### REST notifications
```http
GET  /notifications           // danh sách thông báo
PATCH /notifications/{id}/read
PATCH /notifications/read-all
```

### WebSocket (real-time)
Cài package: `npm install @stomp/stompjs sockjs-client`

```js
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const token = localStorage.getItem('accessToken')

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/api/ws'),
  connectHeaders: { Authorization: `Bearer ${token}` },
  onConnect: () => {
    // Nhận thông báo cá nhân
    client.subscribe('/user/queue/notifications', (msg) => {
      const notification = JSON.parse(msg.body)
      // Hiển thị toast/badge
    })

    // Nhận cập nhật real-time của project
    client.subscribe(`/topic/project/${projectId}`, (msg) => {
      const event = JSON.parse(msg.body)
      // Refresh board / task list
    })
  },
})

client.activate()
```

---

## 12. Dashboard & Gantt

```http
GET /dashboard/me              // tổng quan cá nhân
GET /dashboard/projects/{id}   // tổng quan project
GET /projects/{id}/gantt       // tasks có startDate hoặc dueDate
GET /projects/{id}/roadmap     // versions + tasks theo version
GET /projects/{id}/activity    // activity feed
```

### Gantt response
```json
[
  {
    "id": "uuid",
    "taskKey": "PROJ-1",
    "title": "...",
    "startDate": "2026-03-20",
    "dueDate": "2026-04-01",
    "status": "IN_PROGRESS",
    "assigneeName": "..."
  }
]
```

---

## 13. Xác thực 2 yếu tố (2FA)

```http
POST /auth/2fa/setup    // trả về { qrCodeUrl, secret }
POST /auth/2fa/enable   { "code": "123456" }   // xác minh TOTP rồi bật
POST /auth/2fa/disable  { "code": "123456" }
GET  /auth/2fa/status   // { enabled: true/false }
```

**Flow:**
1. Gọi `/setup` → nhận QR code URL → hiển thị cho user scan bằng Google Authenticator
2. User nhập 6 chữ số → gọi `/enable`
3. Từ lần đăng nhập tiếp theo: sau khi login thành công, nếu `twoFactorEnabled: true` thì yêu cầu nhập TOTP

---

## 14. Cấu trúc Response lỗi

```json
{
  "timestamp": "2026-03-16T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Task cha phải thuộc cùng project",
  "path": "/api/tasks/uuid"
}
```

| Status | Ý nghĩa |
|--------|---------|
| 400 | Dữ liệu không hợp lệ |
| 401 | Chưa xác thực / token hết hạn |
| 403 | Không có quyền truy cập |
| 404 | Không tìm thấy resource |
| 409 | Conflict (trùng lặp, vi phạm ràng buộc) |

```js
// Xử lý lỗi tập trung
api.interceptors.response.use(null, (err) => {
  const msg = err.response?.data?.message || 'Đã có lỗi xảy ra'
  toast.error(msg)
  return Promise.reject(err)
})
```

---

## Tóm tắt các endpoint hay dùng nhất

| Mục đích | Method | Endpoint |
|---------|--------|---------|
| Đăng nhập | POST | `/auth/login` |
| Đổi mật khẩu bắt buộc | PUT | `/users/me/password` |
| Tạo task | POST | `/projects/{id}/tasks` |
| Tạo subtask | POST | `/projects/{id}/tasks` (kèm `parentTaskId`) |
| Chi tiết task (UUID) | GET | `/tasks/{id}` |
| **Chi tiết task (taskKey)** | **GET** | **`/tasks/key/{taskKey}`** |
| Cập nhật task | PUT | `/tasks/{id}` |
| Kéo thả Kanban | PATCH | `/tasks/{id}/move` |
| Kanban board | GET | `/boards/{id}/kanban` |
| Product Backlog | GET | `/projects/{id}/backlog` |
| Sprint Backlog | GET | `/sprints/{id}/backlog` |
| Task của tôi | GET | `/tasks/my` |
| Gantt | GET | `/projects/{id}/gantt` |
| Dashboard | GET | `/dashboard/me` |
| **Log giờ làm việc** | **POST** | **`/time-entries`** |
| **Stats theo ngày** | **GET** | **`/time-entries/stats/daily`** |
| **Stats theo tuần** | **GET** | **`/time-entries/stats/weekly`** |
| **Stats theo tháng** | **GET** | **`/time-entries/stats/monthly?year=`** |
| **Tổng hợp stats** | **GET** | **`/time-entries/stats/summary`** |
| **Stats project** | **GET** | **`/projects/{id}/time-entries/stats`** |
| **File của task** | **GET** | **`/tasks/{id}/attachments?category=`** |
| **Upload file** | **POST** | **`/tasks/{id}/attachments?commentId=`** |
| **File của comment** | **GET** | **`/comments/{id}/attachments`** |
| **Xem ảnh inline** | **GET** | **`/attachments/{id}/inline`** |
| **Download file** | **GET** | **`/attachments/{id}/download`** |
| **Stats file task** | **GET** | **`/tasks/{id}/attachments/stats`** |
| **Stats file project** | **GET** | **`/projects/{id}/attachments/stats`** |
| **[Admin] Danh sách users** | **GET** | **`/admin/users`** |
| **[Admin] Tạo tài khoản** | **POST** | **`/admin/users`** |
| **[Admin] Reset mật khẩu** | **POST** | **`/admin/users/{id}/reset-password`** |

---

## 15. File đính kèm – Hiển thị & Upload

### 15.1 Endpoints

```
GET    /tasks/{taskId}/attachments              – Danh sách file của task
GET    /tasks/{taskId}/attachments?category=IMAGE  – Lọc theo loại
POST   /tasks/{taskId}/attachments             – Upload file (multipart/form-data, field "file")
POST   /tasks/{taskId}/attachments?commentId=  – Upload và gắn vào comment
DELETE /attachments/{id}                       – Xóa file (chỉ người upload)
GET    /attachments/{id}/inline                – Xem trực tiếp (dùng cho <img src>)
GET    /attachments/{id}/download              – Tải xuống
GET    /tasks/{taskId}/attachments/stats       – Thống kê theo loại cho task
GET    /projects/{projectId}/attachments/stats – Thống kê theo loại cho project
GET    /comments/{commentId}/attachments       – File đính kèm của 1 comment
```

### 15.2 Response mẫu – AttachmentResponse

```json
{
  "id": "3f2a1b...",
  "taskId": "aa11...",
  "commentId": "bb22...",
  "uploadedById": "cc33...",
  "uploadedByName": "Nguyễn Văn A",
  "fileName": "screenshot.png",
  "fileSize": 204800,
  "formattedFileSize": "200.00 KB",
  "fileType": "image/png",
  "fileCategory": "IMAGE",
  "fileUrl": "/api/attachments/files/tasks/.../...",
  "isImage": true,
  "createdAt": "2026-03-26T10:00:00"
}
```

`fileCategory` nhận một trong 9 giá trị: `IMAGE` · `DOCUMENT` · `SPREADSHEET` · `PRESENTATION` · `VIDEO` · `AUDIO` · `ARCHIVE` · `CODE` · `OTHER`

### 15.3 Upload file

```js
// api/attachment.js
import api from './axios'

export const uploadAttachment = async (taskId, file, commentId = null) => {
  const form = new FormData()
  form.append('file', file)
  const url = commentId
    ? `/tasks/${taskId}/attachments?commentId=${commentId}`
    : `/tasks/${taskId}/attachments`
  const res = await api.post(url, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data.data
}

export const deleteAttachment = (id) => api.delete(`/attachments/${id}`)

export const getTaskAttachments = (taskId, category = null) =>
  api.get(`/tasks/${taskId}/attachments`, { params: category ? { category } : {} })
    .then(r => r.data.data)

export const getCommentAttachments = (commentId) =>
  api.get(`/comments/${commentId}/attachments`).then(r => r.data.data)

export const getAttachmentStats = (taskId) =>
  api.get(`/tasks/${taskId}/attachments/stats`).then(r => r.data.data)
```

### 15.4 Fetch ảnh cần auth (quan trọng)

Trình duyệt **không tự gắn** `Authorization` header khi load `<img src="...">`.
Phải fetch blob rồi tạo object URL tạm:

```js
// utils/attachment.js
const cache = new Map() // cache để không fetch lại

export const fetchAuthImage = async (attachmentId) => {
  if (cache.has(attachmentId)) return cache.get(attachmentId)

  const token = localStorage.getItem('accessToken')
  const res = await fetch(`/api/attachments/${attachmentId}/inline`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error('Không thể tải ảnh')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  cache.set(attachmentId, url)
  return url
}

// Gọi khi component unmount để giải phóng bộ nhớ
export const revokeAuthImage = (attachmentId) => {
  const url = cache.get(attachmentId)
  if (url) {
    URL.revokeObjectURL(url)
    cache.delete(attachmentId)
  }
}

// Icon theo category
export const FILE_ICONS = {
  IMAGE:        '🖼️',
  DOCUMENT:     '📄',
  SPREADSHEET:  '📊',
  PRESENTATION: '📑',
  VIDEO:        '🎬',
  AUDIO:        '🎵',
  ARCHIVE:      '📦',
  CODE:         '💻',
  OTHER:        '📎',
}

export const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
```

### 15.5 Component AuthImage (React)

```jsx
// components/AuthImage.jsx
import { useState, useEffect } from 'react'
import { fetchAuthImage, revokeAuthImage } from '@/utils/attachment'

export default function AuthImage({ attachmentId, fileName, className, style }) {
  const [src, setSrc] = useState(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    let cancelled = false
    fetchAuthImage(attachmentId)
      .then(url => { if (!cancelled) setSrc(url) })
      .catch(() => { if (!cancelled) setError(true) })
    return () => { cancelled = true }
    // Không revoke ở đây vì cache dùng chung — revoke khi unmount trang
  }, [attachmentId])

  if (error) return <span title={fileName}>🖼️ {fileName}</span>
  if (!src) return <span className="skeleton" style={{ width: 200, height: 150 }} />

  return (
    <img
      src={src}
      alt={fileName}
      className={className}
      style={style}
      loading="lazy"
    />
  )
}
```

### 15.6 Component AttachmentItem

```jsx
// components/AttachmentItem.jsx
import { FILE_ICONS, formatFileSize } from '@/utils/attachment'
import AuthImage from './AuthImage'

export default function AttachmentItem({ attachment, onDelete, compact = false }) {
  const { id, fileName, fileSize, fileCategory, uploadedByName, isImage } = attachment
  const downloadUrl = `/api/attachments/${id}/download`

  if (isImage) {
    return (
      <div className="att-image">
        <a href={`/api/attachments/${id}/inline`} target="_blank" rel="noreferrer">
          <AuthImage
            attachmentId={id}
            fileName={fileName}
            style={{
              maxWidth: compact ? 180 : 360,
              maxHeight: compact ? 140 : 280,
              borderRadius: 8,
              objectFit: 'cover',
              cursor: 'zoom-in',
              display: 'block',
            }}
          />
        </a>
        {!compact && (
          <div className="att-image__footer">
            <span className="att-name">{fileName}</span>
            <span className="att-meta">{formatFileSize(fileSize)}</span>
            <a href={downloadUrl} download={fileName} className="att-action">↓ Tải</a>
            {onDelete && (
              <button className="att-delete" onClick={() => onDelete(id)}>✕</button>
            )}
          </div>
        )}
      </div>
    )
  }

  // File thường
  return (
    <div className="att-file">
      <span className="att-file__icon">{FILE_ICONS[fileCategory] ?? '📎'}</span>
      <div className="att-file__body">
        <a href={downloadUrl} download={fileName} className="att-file__name">
          {fileName}
        </a>
        {!compact && (
          <span className="att-file__meta">
            {formatFileSize(fileSize)} · {uploadedByName}
          </span>
        )}
      </div>
      {onDelete && (
        <button className="att-file__delete" onClick={() => onDelete(id)} title="Xóa">
          ✕
        </button>
      )}
    </div>
  )
}
```

### 15.7 CSS

```css
/* Ảnh */
.att-image { display: inline-block; margin: 4px; }
.att-image__footer {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; color: #555; margin-top: 4px;
}
.att-name { font-weight: 500; }
.att-action { color: #1a73e8; text-decoration: none; }
.att-delete { background: none; border: none; cursor: pointer; color: #999; }
.att-delete:hover { color: #e53e3e; }

/* File thường */
.att-file {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; background: #f5f7fa;
  border-radius: 6px; margin-bottom: 4px;
}
.att-file__icon { font-size: 18px; }
.att-file__body { flex: 1; min-width: 0; }
.att-file__name {
  display: block; font-size: 13px; color: #1a73e8;
  text-decoration: none; white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis;
}
.att-file__name:hover { text-decoration: underline; }
.att-file__meta { font-size: 11px; color: #999; }
.att-file__delete { background: none; border: none; cursor: pointer; color: #bbb; }
.att-file__delete:hover { color: #e53e3e; }

/* Skeleton loader */
.skeleton {
  display: inline-block; background: linear-gradient(90deg,#eee 25%,#f5f5f5 50%,#eee 75%);
  background-size: 200% 100%; animation: shimmer 1.2s infinite;
  border-radius: 8px;
}
@keyframes shimmer { 0%{background-position:200%} 100%{background-position:-200%} }
```

---

## 16. File đính kèm trong Comment

### 16.1 Tổng quan luồng

```
User mở comment → load comments + attachments song song
                → nhóm attachments theo commentId
                → hiển thị ảnh ngay trong body comment
                → hiển thị file khác dưới dạng danh sách
User đính kèm file vào comment → POST /tasks/{id}/attachments?commentId={cid}
```

### 16.2 Hook – useCommentAttachments

```js
// hooks/useCommentAttachments.js
import { useState } from 'react'
import { uploadAttachment, deleteAttachment } from '@/api/attachment'

export function useCommentAttachments(taskId, initialMap = {}) {
  // { [commentId]: AttachmentResponse[] }
  const [map, setMap] = useState(initialMap)
  const [uploading, setUploading] = useState({}) // { [commentId]: bool }

  const upload = async (commentId, file) => {
    setUploading(p => ({ ...p, [commentId]: true }))
    try {
      const att = await uploadAttachment(taskId, file, commentId)
      setMap(p => ({ ...p, [commentId]: [...(p[commentId] || []), att] }))
      return att
    } finally {
      setUploading(p => ({ ...p, [commentId]: false }))
    }
  }

  const remove = async (commentId, attachmentId) => {
    await deleteAttachment(attachmentId)
    setMap(p => ({
      ...p,
      [commentId]: (p[commentId] || []).filter(a => a.id !== attachmentId),
    }))
  }

  return { map, uploading, upload, remove }
}
```

### 16.3 Load comments + attachments cùng lúc

```js
// Gọi 2 API song song, ghép dữ liệu ở FE — chỉ 2 request thay vì N+1
const loadCommentsWithAttachments = async (taskId) => {
  const [comments, attachments] = await Promise.all([
    api.get(`/tasks/${taskId}/comments`).then(r => r.data.data),
    api.get(`/tasks/${taskId}/attachments`).then(r => r.data.data),
  ])

  // Nhóm attachment theo commentId (bỏ qua attachment không có commentId)
  const byComment = {}
  for (const a of attachments) {
    if (a.commentId) {
      ;(byComment[a.commentId] ??= []).push(a)
    }
  }

  return { comments, initialAttachmentMap: byComment }
}
```

### 16.4 Component CommentItem

```jsx
// components/CommentItem.jsx
import { useRef, useState } from 'react'
import AttachmentItem from './AttachmentItem'

export default function CommentItem({
  comment,
  taskId,
  attachments = [],    // AttachmentResponse[] cho comment này
  isUploading = false,
  onUpload,            // (commentId, file) => void
  onDeleteAtt,         // (commentId, attachmentId) => void
}) {
  const fileRef = useRef()
  const [dragOver, setDragOver] = useState(false)

  const images = attachments.filter(a => a.isImage)
  const files  = attachments.filter(a => !a.isImage)

  const handleFiles = (fileList) => {
    ;[...fileList].forEach(f => onUpload(comment.id, f))
  }

  return (
    <div
      className={`comment-item ${dragOver ? 'comment-item--dragover' : ''}`}
      onDragOver={e => { e.preventDefault(); setDragOver(true) }}
      onDragLeave={() => setDragOver(false)}
      onDrop={e => { e.preventDefault(); setDragOver(false); handleFiles(e.dataTransfer.files) }}
    >
      {/* ── Header ── */}
      <div className="comment-item__header">
        <img src={comment.authorAvatar} alt="" className="comment-item__avatar" />
        <div className="comment-item__meta">
          <strong>{comment.authorName}</strong>
          <span className="comment-item__time">{formatDate(comment.createdAt)}</span>
          {comment.isEdited && <span className="comment-item__edited">(đã chỉnh sửa)</span>}
        </div>
      </div>

      {/* ── Nội dung text ── */}
      <div
        className="comment-item__body"
        dangerouslySetInnerHTML={{ __html: renderMentions(comment.content) }}
      />

      {/* ── Ảnh đính kèm: grid tự động ── */}
      {images.length > 0 && (
        <div className="comment-item__images">
          {images.map(a => (
            <AttachmentItem
              key={a.id}
              attachment={a}
              compact
              onDelete={id => onDeleteAtt(comment.id, id)}
            />
          ))}
        </div>
      )}

      {/* ── File thường: danh sách ── */}
      {files.length > 0 && (
        <div className="comment-item__files">
          {files.map(a => (
            <AttachmentItem
              key={a.id}
              attachment={a}
              onDelete={id => onDeleteAtt(comment.id, id)}
            />
          ))}
        </div>
      )}

      {/* ── Footer: nút đính kèm ── */}
      <div className="comment-item__footer">
        <button
          className="btn-ghost btn-xs"
          onClick={() => fileRef.current.click()}
          disabled={isUploading}
          title="Đính kèm file vào comment này (kéo thả cũng được)"
        >
          {isUploading ? '⏳ Đang upload…' : '📎 Đính kèm'}
        </button>
        <input
          ref={fileRef}
          type="file"
          multiple
          hidden
          onChange={e => handleFiles(e.target.files)}
        />
      </div>

      {/* ── Drag-over overlay ── */}
      {dragOver && (
        <div className="comment-item__drop-hint">Thả file vào đây để đính kèm</div>
      )}
    </div>
  )
}

// helper: format ngày giờ
function formatDate(iso) {
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

// helper: highlight @mention
function renderMentions(content) {
  return content.replace(/@(\w+)/g, '<span class="mention">@$1</span>')
}
```

### 16.5 Component CommentSection (lắp ghép hoàn chỉnh)

```jsx
// components/CommentSection.jsx
import { useEffect, useState } from 'react'
import { useCommentAttachments } from '@/hooks/useCommentAttachments'
import CommentItem from './CommentItem'
import api from '@/api/axios'

export default function CommentSection({ taskId }) {
  const [comments, setComments]   = useState([])
  const [newText, setNewText]     = useState('')
  const [loading, setLoading]     = useState(true)

  const { map, uploading, upload, remove } = useCommentAttachments(taskId)

  // Load comments + attachments song song
  useEffect(() => {
    const fetchAll = async () => {
      setLoading(true)
      const [cRes, aRes] = await Promise.all([
        api.get(`/tasks/${taskId}/comments`),
        api.get(`/tasks/${taskId}/attachments`),
      ])
      setComments(cRes.data.data)

      // Seed initialMap vào hook (cách đơn giản: set trực tiếp qua setter)
      const byComment = {}
      for (const a of aRes.data.data) {
        if (a.commentId) (byComment[a.commentId] ??= []).push(a)
      }
      // Dùng qua initialMap param hoặc reset map nếu hook hỗ trợ
      // Nếu không, mount lại hook với initialAttachmentMap (xem ghi chú bên dưới)
      setLoading(false)
    }
    fetchAll()
  }, [taskId])

  const postComment = async () => {
    if (!newText.trim()) return
    const res = await api.post(`/tasks/${taskId}/comments`, { content: newText })
    setComments(c => [...c, res.data.data])
    setNewText('')
  }

  if (loading) return <p>Đang tải bình luận…</p>

  return (
    <div className="comment-section">
      {comments.map(c => (
        <CommentItem
          key={c.id}
          comment={c}
          taskId={taskId}
          attachments={map[c.id] || []}
          isUploading={uploading[c.id] || false}
          onUpload={upload}
          onDeleteAtt={remove}
        />
      ))}

      {/* Ô nhập comment mới */}
      <div className="comment-new">
        <textarea
          value={newText}
          onChange={e => setNewText(e.target.value)}
          placeholder="Viết bình luận… (hỗ trợ @mention)"
          rows={3}
        />
        <button onClick={postComment} disabled={!newText.trim()}>
          Gửi
        </button>
      </div>
    </div>
  )
}
```

> **Ghi chú — seed initialMap:**
> Hook `useCommentAttachments` nhận `initialMap` qua param. Để seed data từ API vào, khởi tạo map trước rồi truyền vào hook:
> ```js
> const [initMap, setInitMap] = useState({})
> const { map, ... } = useCommentAttachments(taskId, initMap)
> // sau khi fetch xong: setInitMap(byComment)
> ```

### 16.6 CSS bổ sung cho Comment

```css
.comment-item {
  position: relative;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fff;
  margin-bottom: 12px;
  border: 1px solid #eee;
  transition: border-color 0.15s;
}
.comment-item--dragover {
  border: 2px dashed #4a90e2;
  background: #f0f6ff;
}

.comment-item__header { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.comment-item__avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
.comment-item__meta { display: flex; align-items: baseline; gap: 6px; }
.comment-item__time { font-size: 11px; color: #999; }
.comment-item__edited { font-size: 11px; color: #aaa; font-style: italic; }

.comment-item__body { font-size: 14px; line-height: 1.6; margin-bottom: 8px; }
.mention { color: #1a73e8; font-weight: 500; }

/* Ảnh: flex-wrap tự động nhiều cột */
.comment-item__images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

/* File: stack dọc */
.comment-item__files { margin-bottom: 8px; }

.comment-item__footer { display: flex; gap: 8px; align-items: center; }
.comment-item__drop-hint {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(74,144,226,.08);
  border-radius: 8px;
  font-size: 14px; color: #4a90e2; pointer-events: none;
}

/* Button styles */
.btn-ghost {
  background: none; border: 1px solid #ddd; border-radius: 4px;
  cursor: pointer; font-size: 13px; padding: 3px 10px; color: #555;
}
.btn-ghost:hover { background: #f5f5f5; }
.btn-ghost:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-xs { padding: 2px 8px; font-size: 12px; }
```

---

## 17. Admin – Quản lý người dùng

> Tất cả endpoints `/admin/users` yêu cầu user đang đăng nhập phải có quyền Admin (permission `ADMIN_ACCESS`).
> Nếu không đủ quyền → server trả về `403 Forbidden`.

### 17.1 Endpoints

```
GET    /admin/users                      – Danh sách tất cả users (có tìm kiếm + phân trang)
POST   /admin/users                      – Tạo tài khoản mới (mật khẩu random, gửi qua email)
GET    /admin/users/{id}                 – Chi tiết user
PATCH  /admin/users/{id}/status          – Toggle kích hoạt / vô hiệu hóa
POST   /admin/users/{id}/reset-password  – Đặt lại mật khẩu (random, gửi qua email)
POST   /admin/users/{id}/roles           – Gán role hệ thống cho user
DELETE /admin/users/{id}/roles/{roleId}  – Bỏ role hệ thống khỏi user
```

### 17.2 Tạo tài khoản

```http
POST /admin/users
```
```json
{
  "fullName": "Nguyễn Văn A",
  "email": "a@example.com",
  "username": "nguyenvana",
  "phone": "0901234567"
}
```

> Admin **chỉ điền thông tin cơ bản** — **không có trường password**.
> Hệ thống tự tạo mật khẩu ngẫu nhiên 12 ký tự và gửi về email người dùng.
> Tài khoản mới tự động có `mustChangePassword = true`.

**Response (201):**
```json
{
  "success": true,
  "message": "Tài khoản đã được tạo. Mật khẩu tạm thời đã gửi về email người dùng.",
  "data": {
    "id": "uuid",
    "username": "nguyenvana",
    "email": "a@example.com",
    "fullName": "Nguyễn Văn A",
    "phone": "0901234567",
    "isActive": true,
    "mustChangePassword": true,
    "createdAt": "2026-03-29T10:00:00"
  }
}
```

**Lỗi thường gặp:**
```json
// Email đã tồn tại
{ "status": 400, "message": "Email 'a@example.com' đã được sử dụng" }

// Username đã tồn tại
{ "status": 400, "message": "Username 'nguyenvana' đã được sử dụng" }
```

```js
// api/adminUserService.ts
export const adminCreateUser = (data) =>
  api.post('/admin/users', data).then(r => r.data.data)
```

### 17.3 Danh sách users

```http
GET /admin/users?keyword=&page=0&size=20
```

> Khác với `GET /users/search` (chỉ active users), endpoint này trả về **tất cả users** kể cả đã vô hiệu hóa.

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "username": "nguyenvana",
      "email": "a@example.com",
      "fullName": "Nguyễn Văn A",
      "isActive": true,
      "mustChangePassword": false,
      "roles": [{ "id": "uuid", "name": "PM", "isSystemRole": true }],
      "lastLoginAt": "2026-03-28T09:15:00",
      "createdAt": "2026-03-01T10:00:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "page": 0,
  "size": 20
}
```

```js
export const adminGetUsers = (keyword = '', page = 0, size = 20) =>
  api.get('/admin/users', { params: { keyword, page, size } }).then(r => r.data.data)
```

### 17.4 Cập nhật thông tin

```http
PUT /admin/users/{id}
```
```json
{
  "fullName": "Nguyễn Văn B",
  "phone": "0909999999",
  "timezone": "Asia/Ho_Chi_Minh",
  "language": "vi"
}
```
> Chỉ cập nhật các trường được truyền vào (null = giữ nguyên).

```js
export const adminUpdateUser = (id, data) =>
  api.put(`/admin/users/${id}`, data).then(r => r.data.data)
```

### 17.5 Vô hiệu hóa / Kích hoạt lại

```http
PATCH /admin/users/{id}/status   // Toggle: active ↔ inactive (không cần body)
```

> User bị vô hiệu hóa sẽ **không thể đăng nhập**. Không thể tự vô hiệu hóa chính mình.

**Response:** trả về `AdminUserResponse` với `isActive` đã được cập nhật.

```js
export const adminToggleUserStatus = (id) =>
  api.patch(`/admin/users/${id}/status`).then(r => r.data.data)
```

### 17.6 Đặt lại mật khẩu

```http
POST /admin/users/{id}/reset-password
```
> Không cần body.
> Server tạo mật khẩu ngẫu nhiên mới → lưu vào DB (đã hash) → gửi email cho người dùng → `mustChangePassword = true`.

**Response (200):**
```json
{
  "success": true,
  "message": "Mật khẩu mới đã được gửi về email người dùng. Người dùng sẽ phải đổi mật khẩu sau khi đăng nhập."
}
```

```js
export const adminResetPassword = (id) =>
  api.post(`/admin/users/${id}/reset-password`)
```

### 17.7 Luồng hoàn chỉnh – mustChangePassword

```
Admin tạo tài khoản / reset mật khẩu
   → Backend: tạo mật khẩu random → hash → lưu DB → gửi email → mustChangePassword = true
   ↓
User nhận email → đọc mật khẩu tạm
   ↓
User đăng nhập (POST /auth/login)
   → Response: { mustChangePassword: true, accessToken: "..." }
   ↓
FE kiểm tra mustChangePassword
   → true  → redirect /change-password (chặn mọi route khác)
   → false → vào app bình thường
   ↓
User đổi mật khẩu (PUT /users/me/password)
   { currentPassword: "<mật_khẩu_tạm>", newPassword: "...", confirmPassword: "..." }
   → mustChangePassword = false
   ↓
FE redirect vào app chính
```

### 17.8 Gợi ý UI trang Admin Users

```
┌──────────────────────────────────────────────────────────────────┐
│  Quản lý người dùng                           [+ Tạo tài khoản] │
├────────────────────────────────┬─────────────────────────────────┤
│  🔍 Tìm kiếm...                │                                 │
├────────────────────────────────┴─────────────────────────────────┤
│  Họ tên          Email              Trạng thái   Lần đăng nhập   │
│  ──────────────────────────────────────────────────────────────  │
│  Nguyễn Văn A    a@example.com      ● Active     28/03/2026      │
│  [Sửa] [Reset MK] [Vô hiệu hóa]                                 │
│  ──────────────────────────────────────────────────────────────  │
│  Trần Thị B      b@example.com      ○ Inactive   —              │
│  [Sửa] [Kích hoạt]                                              │
└──────────────────────────────────────────────────────────────────┘
```

**Lưu ý badge `mustChangePassword`:** Nếu `mustChangePassword = true`, hiển thị badge cảnh báo "Chưa đổi MK" bên cạnh tên user để admin biết user chưa hoàn tất đăng nhập lần đầu.

```jsx
{user.mustChangePassword && (
  <span style={{
    background: '#fef3c7', color: '#92400e',
    fontSize: 11, fontWeight: 600,
    padding: '2px 6px', borderRadius: 4, marginLeft: 6,
  }}>
    Chưa đổi MK
  </span>
)}
```

---

## 18. Admin – Quản lý chức vụ (Role)

> Tất cả endpoints `/admin/roles` và `/admin/permissions` yêu cầu permission `ADMIN_ACCESS`.

### Phân biệt 2 loại role

| | Role hệ thống | Role project |
|---|---|---|
| Quản lý tại | `/admin/roles` | `/projects/{id}/members` |
| Lưu ở | Bảng `roles` (DB) | Field `role` trong `project_members` |
| Ý nghĩa | Quyền truy cập toàn hệ thống | Chức vụ trong từng dự án |
| Gán cho user | `POST /admin/users/{id}/roles` | Khi add member vào project |
| **Đặc biệt** | **Nếu user có role hệ thống → tự động thành project role khi thêm vào dự án** | Chuỗi tự do, admin tự đặt |

### System roles mặc định (do hệ thống tạo sẵn)

| Role | `isSystemRole` | Mô tả | Tự động gán project role |
|---|---|---|---|
| `ADMIN` | ✓ | Quản trị viên hệ thống, toàn quyền | Có (`"ADMIN"`) |
| `PM` | ✓ | Project Manager | Có (`"PM"`) |

> Role `ADMIN` và `PM` là system role — **không thể xóa, không thể sửa `isSystemRole`**.
> Admin có thể tạo thêm role tùy ý (VD: `DEV`, `TESTER`) — các role này cũng sẽ tự động ánh xạ vào project khi thêm thành viên.

---

### 18.1 Endpoints

```
GET    /admin/permissions                    – Danh sách tất cả permissions có trong hệ thống
GET    /admin/roles                          – Danh sách tất cả roles
GET    /admin/roles/{id}                     – Chi tiết role
POST   /admin/roles                          – Tạo role mới
PUT    /admin/roles/{id}                     – Sửa tên / mô tả role
DELETE /admin/roles/{id}                     – Xóa role (không xóa được system role)
POST   /admin/roles/{id}/permissions         – Gán thêm permissions vào role
DELETE /admin/roles/{id}/permissions         – Bỏ permissions khỏi role
POST   /admin/users/{id}/roles               – Gán role hệ thống cho user
DELETE /admin/users/{id}/roles/{roleId}      – Bỏ role hệ thống khỏi user
```

---

### 18.2 Data models

```typescript
interface Permission {
  id: string
  name: string        // VD: "ADMIN_ACCESS", "PROJECT_CREATE"
  description: string
  resource: string    // VD: "PROJECT", "TASK", "ADMIN"
}

interface Role {
  id: string
  name: string        // VD: "PM", "DEV" — BE tự uppercase + replace space → "_"
  description: string
  isSystemRole: boolean   // true = không sửa/xóa được (ADMIN, PM là system role)
  permissions: Permission[]
  createdAt: string
}

// Trong AdminUserResponse — có trường roles
interface AdminUserResponse {
  id: string
  username: string
  email: string
  fullName: string
  isActive: boolean
  mustChangePassword: boolean
  roles: Role[]       // ← danh sách system roles của user
  lastLoginAt: string
  createdAt: string
}
```

> `systemRole` để hiển thị trên UI khi tìm kiếm user (xem Section 3 – Thêm thành viên):
> ```js
> // Lấy system role đầu tiên (không phải ADMIN) để hiển thị
> const systemRole = user.roles?.find(r => r.name !== 'ADMIN')?.name
>                 ?? user.roles?.[0]?.name
>                 ?? null
> ```

---

### 18.3 Lấy danh sách

```http
GET /admin/roles
GET /admin/permissions
```

```js
// api/adminRoleService.js
export const getRoles = () =>
  api.get('/admin/roles').then(r => r.data.data)

export const getPermissions = () =>
  api.get('/admin/permissions').then(r => r.data.data)
```

---

### 18.4 Tạo role mới

```http
POST /admin/roles
```
```json
{
  "name": "Quản lý dự án",
  "description": "Có thể tạo và quản lý dự án",
  "permissionIds": ["uuid1", "uuid2"]
}
```

> - `name` bắt buộc, max 100 ký tự. BE tự chuyển thành `QUẢN_LÝ_DỰ_ÁN` (uppercase + replace space)
> - `permissionIds` tùy chọn — gán permissions ngay khi tạo

**Response (201):**
```json
{
  "success": true,
  "message": "Tạo role thành công",
  "data": {
    "id": "uuid",
    "name": "QUẢN_LÝ_DỰ_ÁN",
    "description": "Có thể tạo và quản lý dự án",
    "isSystemRole": false,
    "permissions": [],
    "createdAt": "2026-03-30T01:00:00"
  }
}
```

**Lỗi trùng tên:**
```json
{ "success": false, "message": "Role 'QUẢN_LÝ_DỰ_ÁN' đã tồn tại" }
```

```js
export const createRole = (data) =>
  api.post('/admin/roles', data).then(r => r.data.data)
```

---

### 18.5 Sửa role

```http
PUT /admin/roles/{id}
```
```json
{
  "name": "Team Lead",
  "description": "Mô tả mới"
}
```

> Chỉ gửi field cần thay đổi. Không sửa được `isSystemRole = true`.

```js
export const updateRole = (id, data) =>
  api.put(`/admin/roles/${id}`, data).then(r => r.data.data)
```

---

### 18.6 Xóa role

```http
DELETE /admin/roles/{id}
```

> Không xóa được system role — server trả `403`.

```js
export const deleteRole = (id) =>
  api.delete(`/admin/roles/${id}`)
```

---

### 18.7 Quản lý permissions của role

**Gán thêm permissions:**
```http
POST /admin/roles/{id}/permissions
{ "permissionIds": ["uuid1", "uuid2"] }
```

**Bỏ permissions:**
```http
DELETE /admin/roles/{id}/permissions
{ "permissionIds": ["uuid1"] }
```

> Cả hai đều trả về `Role` đã cập nhật. Gán là **additive** — POST chỉ thêm, không ghi đè toàn bộ.

```js
export const addPermissionsToRole = (roleId, permissionIds) =>
  api.post(`/admin/roles/${roleId}/permissions`, { permissionIds }).then(r => r.data.data)

export const removePermissionsFromRole = (roleId, permissionIds) =>
  api.delete(`/admin/roles/${roleId}/permissions`, { data: { permissionIds } }).then(r => r.data.data)
```

---

### 18.8 Gán / bỏ role cho user

```http
POST   /admin/users/{userId}/roles           { "roleId": "uuid" }
DELETE /admin/users/{userId}/roles/{roleId}
```

> Trả về `AdminUserResponse` với danh sách `roles` đã cập nhật.
>
> **Lưu ý:** Gán role hệ thống cho user ảnh hưởng đến project role tự động. Ví dụ gán `PM` cho user → từ đó khi thêm user vào bất kỳ dự án nào, role trong dự án sẽ tự động là `"PM"`.

```js
export const assignRoleToUser = (userId, roleId) =>
  api.post(`/admin/users/${userId}/roles`, { roleId }).then(r => r.data.data)

export const removeRoleFromUser = (userId, roleId) =>
  api.delete(`/admin/users/${userId}/roles/${roleId}`).then(r => r.data.data)
```

---

### 18.9 Gợi ý UI

**Trang `/admin/roles`:**
```
┌─────────────────────────────────────────────────────────────────┐
│  Quản lý chức vụ hệ thống                        [+ Tạo Role]  │
├─────────────────────────────────────────────────────────────────┤
│  ADMIN   [System] Quản trị viên hệ thống  15 quyền  [Xem]      │
│  PM      [System] Project Manager         15 quyền  [Xem]      │
│  DEV              Lập trình viên           5 quyền  [Sửa][Xóa] │
│  TESTER           Kiểm thử                3 quyền  [Sửa][Xóa]  │
└─────────────────────────────────────────────────────────────────┘
```
- `isSystemRole = true` → ẩn nút Sửa/Xóa, hiển thị badge `[System]`
- Hiển thị chú thích: *"Role hệ thống sẽ tự động trở thành chức vụ trong dự án khi thêm thành viên"*

**Trang `/admin/users` — cột Role hệ thống:**
```
┌──────────────────────────────────────────────────────────────────────┐
│  Họ tên          Email              Role hệ thống   Trạng thái       │
│  ──────────────────────────────────────────────────────────────────  │
│  Nguyễn Văn A    a@example.com      [PM]             ● Active        │
│  [Sửa] [Reset MK] [Gán role] [Vô hiệu hóa]                         │
│  ──────────────────────────────────────────────────────────────────  │
│  Trần Thị B      b@example.com      —                ● Active        │
│  [Sửa] [Reset MK] [Gán role] [Vô hiệu hóa]                         │
└──────────────────────────────────────────────────────────────────────┘
```

**Modal Tạo / Sửa Role:**
```
Tên role:   [________________]
Mô tả:      [________________]

Permissions:
  ADMIN:    ☑ ADMIN_ACCESS
  PROJECT:  ☑ PROJECT_VIEW  ☑ PROJECT_CREATE  ☐ PROJECT_DELETE
  TASK:     ☑ TASK_VIEW     ☐ TASK_DELETE

                              [Hủy]  [Lưu]
```

**Tab Roles trong User Detail:**
```
Roles hệ thống: [TEAM_LEAD ×]  [USER ×]
Thêm role:      [Chọn role... ▼]  [+ Gán]
```
- Dropdown chỉ hiển thị roles chưa được gán cho user đó
- `×` gọi `DELETE /admin/users/{id}/roles/{roleId}`

---

### 18.10 Lưu ý quan trọng

| Điều kiện | Hành vi |
|-----------|---------|
| `isSystemRole = true` | Không hiển thị nút Sửa / Xóa |
| Gán permission | Additive — POST chỉ thêm, không ghi đè |
| Xóa permission | Gửi đúng `permissionIds` muốn bỏ |
| Tên role | BE tự uppercase + replace space thành `_`, FE hiển thị nguyên như nhập |
| Role project vs Role hệ thống | Khác nhau hoàn toàn — role project là chuỗi tự do trong từng dự án, role hệ thống kiểm soát quyền admin panel |
