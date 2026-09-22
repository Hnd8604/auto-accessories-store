# Auto Accessories Store

Ứng dụng thương mại điện tử full-stack dành cho cửa hàng phụ kiện ô tô. Hệ thống hỗ trợ quản lý sản phẩm, giỏ hàng, đơn hàng, thanh toán trực tuyến, nội dung blog, dịch vụ chuyên nghiệp và tương tác thời gian thực giữa khách hàng với quản trị viên.

## Tính năng chính

### Khách hàng

- Xem, tìm kiếm và lọc sản phẩm theo danh mục, thương hiệu.
- Quản lý giỏ hàng cho cả khách vãng lai và người dùng đã đăng nhập.
- Đăng ký, đăng nhập bằng tài khoản hoặc Google OAuth2; đổi và khôi phục mật khẩu.
- Đặt hàng, theo dõi lịch sử và trạng thái đơn hàng.
- Thanh toán trực tuyến qua payOS.
- Xem bài viết, dịch vụ và chi tiết sản phẩm.
- Nhận thông báo đơn hàng qua email và SSE.
- Trò chuyện trực tiếp với cửa hàng qua WebSocket/STOMP.

### Quản trị viên

- Quản lý sản phẩm, hình ảnh, danh mục và thương hiệu.
- Quản lý đơn hàng, người dùng, vai trò và quyền hạn.
- Quản lý banner, bài viết, danh mục bài viết và dịch vụ.
- Theo dõi thông báo và trả lời hội thoại của khách hàng.
- Tải ảnh lên Cloudinary thông qua backend.

## Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Frontend | React 18, TypeScript, Vite 5, Tailwind CSS, shadcn/ui, TanStack Query |
| Backend | Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA, WebSocket, SSE |
| Dữ liệu | PostgreSQL 16, Redis 7.4, Flyway |
| Tích hợp | payOS, Google OAuth2, Cloudinary, Brevo SMTP |
| Vận hành | Docker Compose, Nginx, GitHub Actions |

## Yêu cầu môi trường

- Docker Desktop và Docker Compose.
- Java Development Kit 21.
- Node.js 22 và npm (Node.js 18+ vẫn có thể dùng để phát triển cục bộ).
- PowerShell 5.1+ nếu dùng script `run.ps1` trên Windows.

Maven không cần cài riêng vì backend đã có Maven Wrapper.

## Chạy nhanh trên Windows

### 1. Tạo cấu hình môi trường

Tại thư mục gốc của dự án:

```powershell
Copy-Item .env.dev.example .env.dev
```

Mở `.env.dev` và thay các giá trị mẫu. Trước lần chạy đầu tiên, tối thiểu cần cấu hình:

```dotenv
POSTGRES_PASSWORD=your_strong_database_password
REDIS_PASSWORD=your_strong_redis_password
JWT_SIGNER_KEY=your_random_secret_key
ADMIN_PASSWORD=your_initial_admin_password
```

Có thể tạo khóa JWT bằng lệnh sau nếu máy đã cài OpenSSL:

```bash
openssl rand -base64 48
```

Các thông tin Brevo SMTP, Google OAuth2, Cloudinary và payOS cần được điền để sử dụng những tính năng tích hợp tương ứng. Với Brevo, dùng SMTP login và SMTP key (không dùng API key), đồng thời đặt `MAIL_FROM_EMAIL` thành sender/domain đã xác minh. Không commit `.env.dev` hoặc bất kỳ khóa bí mật nào lên Git.

### 2. Khởi động dự án

```powershell
.\run.ps1
```

Script sẽ:

1. Khởi động PostgreSQL và Redis bằng Docker Compose.
2. Chạy Spring Boot backend trong một cửa sổ PowerShell mới.
3. Cài dependency và chạy Vite frontend trong một cửa sổ PowerShell mới.

Nếu `node_modules` đã sẵn sàng, có thể bỏ qua bước cài dependency:

```powershell
.\run.ps1 -SkipInstall
```

### 3. Truy cập ứng dụng

| Dịch vụ | Địa chỉ |
| --- | --- |
| Giao diện web | <http://localhost:3000> |
| REST API | <http://localhost:8080/api/v1> |
| Swagger UI | <http://localhost:8080/api/v1/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/api/v1/v3/api-docs> |
| Health check | <http://localhost:8080/api/v1/actuator/health> |
| RedisInsight (tùy chọn) | <http://localhost:5540> |

Tài khoản quản trị ban đầu sử dụng `ADMIN_USERNAME` (mặc định là `admin`) và `ADMIN_PASSWORD` trong `.env.dev`. Tài khoản chỉ được tạo khi database chưa có người dùng quản trị tương ứng.

### 4. Dừng dự án

Đóng hai cửa sổ backend/frontend, sau đó dừng hạ tầng:

```powershell
.\run.ps1 -Stop
```

## Chạy từng thành phần

Script hỗ trợ khởi động riêng từng phần:

```powershell
# Chỉ PostgreSQL và Redis
.\run.ps1 -Only infra

# Chỉ backend (hạ tầng phải đang chạy)
.\run.ps1 -Only backend

# Chỉ frontend
.\run.ps1 -Only frontend -SkipInstall
```

Có thể bật thêm RedisInsight bằng Docker Compose:

```powershell
docker compose --env-file .env.dev --profile infra --profile tools up -d
```

## Kiểm tra chất lượng mã nguồn

### Backend

```powershell
Set-Location backend
.\mvnw.cmd test
```

Trên macOS/Linux, dùng `./mvnw test`.

### Frontend

```powershell
Set-Location frontend
npm ci
npm run typecheck
npm run lint
npm run build
```

## Biến môi trường

Toàn bộ cấu hình phát triển nằm trong `.env.dev` ở thư mục gốc. Danh sách đầy đủ và chú thích có trong [`.env.dev.example`](.env.dev.example).

| Nhóm biến | Mục đích |
| --- | --- |
| `POSTGRES_*`, `DB_*` | Kết nối PostgreSQL |
| `REDIS_*` | Kết nối và xác thực Redis |
| `JWT_*` | Ký access token và refresh token |
| `ADMIN_*` | Khởi tạo tài khoản quản trị đầu tiên |
| `MAIL_*` | Gửi email qua SMTP |
| `GOOGLE_*` | Xác thực Google OAuth2 ở backend |
| `CLOUDINARY_*` | Lưu trữ ảnh sản phẩm và nội dung |
| `PAYOS_*` | Tạo link thanh toán và xác thực webhook |
| `VITE_*` | Cấu hình công khai được nhúng vào frontend |

> Mọi biến có tiền tố `VITE_` đều có thể xuất hiện trong bundle JavaScript gửi tới trình duyệt. Tuyệt đối không đặt mật khẩu, API secret hoặc private key trong các biến này.

## Cấu trúc dự án

```text
auto_accessories_store/
├── backend/                 # Spring Boot API, migration và unit test
│   └── src/
│       ├── main/
│       ├── test/
│       └── docs/            # Tài liệu kỹ thuật theo tính năng
├── frontend/                # React + TypeScript + Vite
│   └── src/
│       ├── components/
│       ├── features/
│       ├── pages/
│       └── routes/
├── docs/                    # Hướng dẫn test, deploy và CI/CD
├── docker-compose.yml       # Hạ tầng phát triển cục bộ
├── docker-compose.prod.yml  # Stack production
├── .env.dev.example         # Mẫu cấu hình development
├── .env.prod.example        # Mẫu cấu hình production
└── run.ps1                  # Script khởi động development trên Windows
```

Flyway tự tạo và kiểm tra schema khi backend khởi động. Các migration nằm tại `backend/src/main/resources/db/migration`.

## Tài liệu liên quan

- [Hướng dẫn kiểm thử](docs/TESTING.md)
- [Triển khai lên VPS](docs/DEPLOY.md)
- [Quy trình CI/CD](docs/CI-CD.md)
- [Xử lý lỗi toàn cục](docs/GLOBAL_EXCEPTION_HANDLER.md)
- [Giỏ hàng](backend/src/docs/CART.md)
- [Thanh toán payOS](backend/src/docs/PAYMENT.md)
- [Đăng nhập Google](backend/src/docs/GOOGLE_LOGIN.md)
- [Live chat](backend/src/docs/LIVECHAT.md)
- [Dịch vụ chuyên nghiệp](backend/src/docs/SERVICES.md)
- [Quy ước mã lỗi](backend/src/docs/ERROR_CODES.md)

Để triển khai production, không dùng `.env.dev`; hãy tạo `.env.prod` từ `.env.prod.example` và làm theo tài liệu deploy.
