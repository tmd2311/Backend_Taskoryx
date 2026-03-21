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
8. [Time Tracking](#8-time-tracking)
9. [Bình luận & Đính kèm](#9-bình-luận--đính-kèm)
10. [Thông báo & WebSocket](#10-thông-báo--websocket)
11. [Dashboard & Gantt](#11-dashboard--gantt)
12. [Xác thực 2 yếu tố (2FA)](#12-xác-thực-2-yếu-tố-2fa)
13. [Cấu trúc Response lỗi](#13-cấu-trúc-response-lỗi)

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

### Đăng ký
```http
POST /auth/register
{
  "fullName": "Nguyễn Văn A",
  "email": "a@example.com",
  "password": "password123"
}
```

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
  "user": { "id": "uuid", "email": "...", "fullName": "...", "twoFactorEnabled": false }
}
```

```js
// Lưu token sau khi login
const { data } = await api.post('/auth/login', { email, password })
localStorage.setItem('accessToken', data.accessToken)
localStorage.setItem('refreshToken', data.refreshToken)
```

### Đăng xuất
```http
POST /auth/logout   // token bị xóa phía client
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
POST   /projects/{id}/members   { "userId": "uuid", "role": "MEMBER" }
DELETE /projects/{id}/members/{userId}
```

### Board & Column
```http
POST /projects/{id}/boards   { "name": "Sprint Board" }
GET  /boards/{id}/kanban     // trả về board + columns + tasks đã sắp xếp
POST /boards/{id}/columns    { "name": "To Do", "position": 1000 }
PUT  /columns/{id}           { "name": "In Progress", "taskLimit": 5 }
DELETE /columns/{id}
```

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

### Lấy chi tiết task (bao gồm subtask)
```http
GET /tasks/{id}
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
GET /projects/{id}/backlog   // tasks không gắn column
GET /tasks/my                // tasks được assign cho user hiện tại
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
{ "name": "Sprint 1", "goal": "Hoàn thành MVP", "startDate": "2026-03-20", "endDate": "2026-04-03" }

GET  /projects/{id}/sprints
GET  /sprints/{id}
PUT  /sprints/{id}
DELETE /sprints/{id}

POST /sprints/{id}/start      // bắt đầu sprint (chỉ 1 sprint ACTIVE/project)
POST /sprints/{id}/complete   // kết thúc sprint

POST   /sprints/{id}/tasks    { "taskId": "uuid" }   // thêm task vào sprint
DELETE /sprints/{id}/tasks    { "taskId": "uuid" }   // gỡ task khỏi sprint
```

> Sprint status: `PLANNING` → `ACTIVE` → `COMPLETED`

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

## 8. Time Tracking

```http
POST /time-entries
{
  "taskId": "uuid",
  "description": "Fix bug login",
  "hoursSpent": 2.5,
  "date": "2026-03-16"
}

GET  /tasks/{id}/time-entries   // log của task
GET  /time-entries/my           // log của user hiện tại
GET  /time-entries/range?startDate=2026-03-01&endDate=2026-03-31

PUT    /time-entries/{id}
DELETE /time-entries/{id}
```

---

## 9. Bình luận & Đính kèm

### Comment
```http
GET  /tasks/{id}/comments
POST /tasks/{id}/comments   { "content": "LGTM!", "mentionedUserIds": ["uuid1"] }
PUT  /comments/{id}         { "content": "..." }
DELETE /comments/{id}
```

### Attachment (upload file)
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

## 10. Thông báo & WebSocket

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

## 11. Dashboard & Gantt

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

## 12. Xác thực 2 yếu tố (2FA)

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

## 13. Cấu trúc Response lỗi

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
| Tạo task | POST | `/projects/{id}/tasks` |
| Tạo subtask | POST | `/projects/{id}/tasks` (kèm `parentTaskId`) |
| Chi tiết task | GET | `/tasks/{id}` |
| Cập nhật task | PUT | `/tasks/{id}` |
| Kéo thả Kanban | PATCH | `/tasks/{id}/move` |
| Kanban board | GET | `/boards/{id}/kanban` |
| Backlog | GET | `/projects/{id}/backlog` |
| Task của tôi | GET | `/tasks/my` |
| Gantt | GET | `/projects/{id}/gantt` |
| Dashboard | GET | `/dashboard/me` |
