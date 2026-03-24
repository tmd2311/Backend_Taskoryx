# Hướng dẫn đóng góp – Taskoryx Backend

Cảm ơn bạn đã quan tâm đến việc đóng góp cho Taskoryx! Tài liệu này hướng dẫn quy trình đóng góp để mọi thứ diễn ra suôn sẻ.

---

## Mục lục

- [Báo cáo lỗi (Bug Report)](#báo-cáo-lỗi)
- [Đề xuất tính năng (Feature Request)](#đề-xuất-tính-năng)
- [Quy trình đóng góp code](#quy-trình-đóng-góp-code)
- [Quy ước code](#quy-ước-code)
- [Cài đặt môi trường phát triển](#cài-đặt-môi-trường-phát-triển)

---

## Báo cáo lỗi

Nếu bạn phát hiện lỗi, vui lòng tạo [GitHub Issue](../../issues/new) với các thông tin:

- **Mô tả lỗi**: Lỗi xảy ra như thế nào?
- **Các bước tái hiện**: Gọi API nào / request body gì để thấy lỗi?
- **Kết quả mong đợi**: Đáng lẽ phải trả về gì?
- **Kết quả thực tế**: Thực tế trả về gì (status code, response body)?
- **Môi trường**: OS, phiên bản Java, phiên bản PostgreSQL

---

## Đề xuất tính năng

Tạo [GitHub Issue](../../issues/new) với nhãn `enhancement` và mô tả:

- Tính năng bạn muốn thêm là gì?
- Tại sao tính năng này hữu ích?
- Bạn hình dung endpoint / luồng xử lý như thế nào?

---

## Quy trình đóng góp code

### 1. Fork & Clone

```bash
git clone https://github.com/<your-username>/taskoryx-be.git
cd taskoryx-be
```

### 2. Tạo branch mới

Đặt tên branch theo quy ước:

```bash
git checkout -b feat/ten-tinh-nang    # tính năng mới
git checkout -b fix/mo-ta-loi         # sửa lỗi
git checkout -b docs/cap-nhat-readme  # tài liệu
```

### 3. Phát triển & commit

```bash
# Viết code...

git add <files>
git commit -m "feat: thêm endpoint X"
```

**Quy ước commit message** (Conventional Commits):

| Tiền tố | Ý nghĩa |
|---------|---------|
| `feat:` | Tính năng mới |
| `fix:` | Sửa lỗi |
| `docs:` | Cập nhật tài liệu |
| `refactor:` | Tái cấu trúc code |
| `test:` | Thêm / sửa unit test |
| `chore:` | Cập nhật config, dependencies |

### 4. Kiểm tra trước khi push

```bash
# Build và chạy toàn bộ test
./mvnw clean verify

# Chỉ chạy test
./mvnw test

# Chỉ build (bỏ qua test)
./mvnw clean package -DskipTests
```

### 5. Tạo Pull Request

- Push branch lên fork của bạn
- Tạo Pull Request vào branch `main`
- Mô tả rõ những thay đổi bạn đã làm
- Liên kết với Issue liên quan (nếu có): `Closes #123`

---

## Quy ước code

### Java / Spring Boot

- Tuân theo **Java Code Conventions** (camelCase cho method/field, PascalCase cho class)
- Dùng **Lombok** để giảm boilerplate (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`)
- Annotation `@Transactional` đặt ở tầng **Service**, không đặt ở Controller
- Không để business logic trong Controller; Controller chỉ nhận request và trả response

### Entity

- Mỗi entity ánh xạ 1 bảng, đặt trong `entity/`
- Dùng `@Column(nullable = false)` rõ ràng cho các trường bắt buộc
- Đặt quan hệ `FetchType.LAZY` theo mặc định để tránh N+1 query

### DTO

- Tách biệt **Request DTO** (nhận từ client) và **Response DTO** (trả về client)
- Không expose entity JPA trực tiếp qua API
- Validate input bằng annotation Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Email`, ...)

### Service

- Mỗi domain có 1 interface + 1 implementation trong `service/`
- Ném exception có nghĩa (`ResourceNotFoundException`, `AccessDeniedException`) thay vì trả `null`

### Exception

- Toàn bộ exception xử lý tập trung qua `@ControllerAdvice` trong `exception/`
- Mỗi lỗi trả về JSON chuẩn: `{ "status": 404, "message": "..." }`

---

## Cài đặt môi trường phát triển

```bash
# Chạy ứng dụng (dev profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Build JAR
./mvnw clean package

# Chạy toàn bộ test
./mvnw test

# Xem Swagger UI sau khi chạy
# http://localhost:8080/api/swagger-ui.html
```

Chi tiết xem thêm tại [README.md](./README.md).
