# Đăng Nhập Google (OAuth2) — Tài liệu Kỹ thuật

Tài liệu này mô tả tính năng **Đăng nhập bằng Google** của hệ thống AutoLux Store, dùng giao thức **OAuth2 Authorization Code Flow**. Người dùng đăng nhập bằng tài khoản Google; backend đổi `code` lấy thông tin user từ Google, tự **tạo hoặc liên kết** tài khoản nội bộ, rồi cấp **JWT** giống đăng nhập thường.

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc & Flow OAuth2](#2-kiến-trúc--flow-oauth2)
3. [Backend](#3-backend)
   - [Cấu hình](#31-cấu-hình)
   - [API Endpoint](#32-api-endpoint)
   - [Service — 4 bước xử lý](#33-service--4-bước-xử-lý)
   - [Tìm hoặc tạo user](#34-tìm-hoặc-tạo-user)
   - [Cấp JWT](#35-cấp-jwt)
   - [Security](#36-security)
4. [Frontend](#4-frontend)
5. [Luồng dữ liệu](#5-luồng-dữ-liệu)
6. [Cấu hình Google Cloud Console](#6-cấu-hình-google-cloud-console)
7. [Hướng dẫn kiểm tra](#7-hướng-dẫn-kiểm-tra)
8. [Giới hạn & hướng mở rộng](#8-giới-hạn--hướng-mở-rộng)

---

## 1. Tổng quan

### Vấn đề
Bắt người dùng nhớ thêm một cặp username/password là rào cản. Nhiều khách muốn đăng nhập nhanh bằng tài khoản Google sẵn có, và không phải nhập lại thông tin cá nhân (tên, email, ảnh đại diện).

### Giải pháp
Tích hợp **Google OAuth2 Authorization Code Flow**:
- FE chuyển hướng người dùng tới trang đồng ý (consent) của Google.
- Google trả về một `authorization code`.
- **Backend** (không phải FE) đổi `code` lấy access token của Google, gọi Google lấy thông tin user.
- Backend tìm/tạo user nội bộ và cấp **JWT** của hệ thống → từ đó hoạt động y như đăng nhập thường.

### Kết quả
- Đăng nhập 1 chạm bằng Google, không cần mật khẩu.
- User mới được **tự tạo** kèm tên, email, avatar từ Google.
- User đã có (đăng ký bằng email) sẽ được **liên kết** với Google, không tạo trùng.

---

## 2. Kiến trúc & Flow OAuth2

```
  Người dùng          Frontend (React)            Google                 Backend (Spring Boot)
  ──────────          ────────────────            ──────                 ─────────────────────
      │                     │                        │                          │
  bấm "Đăng nhập     window.location →               │                          │
   bằng Google"     accounts.google.com/o/oauth2     │                          │
      │ ───────────────────┼──────────────────────► consent                     │
      │  đồng ý ◄───────────┼───────────────────────  │                          │
      │                     │  redirect ?code=xxx     │                          │
      │ ◄──────────────────────────────────────────  │                          │
      │              /auth/google/callback            │                          │
      │              (GoogleCallback.tsx)             │                          │
      │                     │  POST /auth/google { code } ─────────────────────► AuthenticationController
      │                     │                        │                          └─► GoogleAuthService
      │                     │                        │   POST oauth2/token       │     exchangeCodeForToken()
      │                     │                        │ ◄─────────────────────────┤
      │                     │                        │   GET userinfo            │     fetchGoogleUserInfo()
      │                     │                        │ ◄─────────────────────────┤
      │                     │                        │                          │     findOrCreateUser()
      │                     │  { user, accessToken, refreshToken } ◄────────────┤     generateAuthResponse()
      │                     │  lưu JWT, điều hướng     │                          │
```

**Authorization Code Flow** (code đổi token ở **server-side**) an toàn hơn Implicit Flow vì `client_secret` và access token của Google không bao giờ lộ ra trình duyệt.

---

## 3. Backend

### 3.1 Cấu hình

File `application.yaml` (mẫu tại `application-example.yaml`):

```yaml
google:
  client-id: "YOUR_GOOGLE_CLIENT_ID"
  client-secret: "YOUR_GOOGLE_CLIENT_SECRET"
  redirect-uri: "http://localhost:3000/auth/google/callback"
```

> `redirect-uri` **phải trùng tuyệt đối** với giá trị khai báo trong Google Cloud Console và với redirect URI mà FE dùng — Google so khớp chính xác từng ký tự.

### 3.2 API Endpoint

File: `src/main/java/app/store/controller/AuthenticationController.java`

| Method | URL | Auth | Mô tả |
|--------|-----|------|-------|
| `POST` | `/auth/google` | **Public** | Đăng nhập bằng Google. Nhận `code`, trả JWT. Tự tạo tài khoản nếu chưa có |

**Request body** (`GoogleAuthRequest`):
```json
{ "code": "4/0AeanS0b...authorization_code_from_google" }
```
`code` bắt buộc (`@NotBlank`).

**Response** (`AuthenticationResponse`):
```json
{
  "code": 1000,
  "result": {
    "user": { "id": "...", "username": "nguyenvana", "email": "...", "fullName": "...", "avatarUrl": "..." },
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "authenticated": true
  }
}
```

### 3.3 Service — 4 bước xử lý

File: `src/main/java/app/store/service/GoogleAuthService.java` → `authenticateWithGoogle(request)`

```
Step 1. exchangeCodeForToken(code)
        POST https://oauth2.googleapis.com/token
        body: code, client_id, client_secret, redirect_uri, grant_type=authorization_code
        → lấy "access_token" của Google

Step 2. fetchGoogleUserInfo(googleAccessToken)
        GET https://www.googleapis.com/oauth2/v2/userinfo (Bearer token)
        → { id, email, name, picture }

Step 3. findOrCreateUser(googleUserInfo)
        → User nội bộ (xem 3.4)

Step 4. authenticationService.generateAuthResponse(user)
        → cấp JWT access + refresh token (xem 3.5)
```

Mọi lỗi gọi Google (đổi token / lấy user info thất bại) đều ném `AppException(ErrorCode.GOOGLE_AUTH_FAILED)` → HTTP `401` (code nghiệp vụ `5001`). Dùng `RestTemplate` để backend gọi sang API Google.

### 3.4 Tìm hoặc tạo user

`findOrCreateUser()` theo thứ tự ưu tiên:

```
1. Tìm theo googleId
   → có: cập nhật avatar nếu đổi → trả user
   → không:
2.   Tìm theo email (user đã đăng ký bằng email trước đó)
     → có: LIÊN KẾT Google (set googleId + avatar) vào user hiện tại  ← tránh tạo trùng
     → không:
3.     Tạo user mới (createNewGoogleUser)
```

**Khi tạo user mới** (`createNewGoogleUser`):
- `username` sinh duy nhất từ phần trước `@` của email; nếu trùng → thêm hậu tố UUID 6 ký tự (`generateUniqueUsername`).
- `password = null` — user Google **không có mật khẩu**.
- Gán role mặc định `USER`.
- Lưu `email`, `fullName`, `avatarUrl`, `googleId` từ Google.
- **Tạo sẵn `Cart`** gắn vào user (giống đăng ký thường).

**Các field liên quan trong `User`** (`entity/User.java`): `googleId`, `avatarUrl`, `password` (nullable).

### 3.5 Cấp JWT

`AuthenticationService.generateAuthResponse(user)` — **dùng chung** cho login thường và Google login:

```java
var accessToken  = generateAccessToken(user);   // JWT, hết hạn theo jwt.access-duration
var refreshToken = generateRefreshToken(user);  // JWT, hết hạn theo jwt.refresh-duration
return AuthenticationResponse.builder()
        .user(userMapper.toUserResponse(user))
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .authenticated(true)
        .build();
```

Sau bước này, user Google dùng JWT y hệt user thường — không có sự khác biệt ở các API khác.

### 3.6 Security

`/auth/google` nằm trong danh sách public của `SecurityConfig.java`:

```java
"/auth/login", "/auth/google", "/auth/introspect", "/auth/logout", "/auth/refresh", ...
```

`client-secret` chỉ tồn tại ở backend; FE không bao giờ thấy. Việc đổi `code → token` diễn ra server-side nên an toàn.

---

## 4. Frontend

| File | Mô tả |
|------|-------|
| `features/auth/pages/AuthPage.tsx` | Nút "Đăng nhập bằng Google" — chuyển hướng tới Google |
| `features/auth/pages/GoogleCallback.tsx` | Trang nhận `?code=...`, gọi backend hoàn tất login |
| `features/auth/api/auth.ts` | `AuthService.googleLogin(code)` → `POST /auth/google` |
| `constants/config.ts` | `GOOGLE_CLIENT_ID`, `GOOGLE_REDIRECT_URI` (từ env `VITE_*`) |
| `routes/index.tsx` | Route `/auth/google/callback` → `GoogleCallback` |

### Bước 1 — Chuyển hướng tới Google (`AuthPage.tsx`)

```javascript
const scope = "openid email profile";
const url =
  `https://accounts.google.com/o/oauth2/v2/auth?` +
  `client_id=${GOOGLE_CLIENT_ID}&` +
  `redirect_uri=${GOOGLE_REDIRECT_URI}&` +
  `response_type=code&` +
  `scope=${scope}&` +
  `access_type=offline&` +
  `prompt=consent`;
window.location.href = url;
```

### Bước 2 — Callback (`GoogleCallback.tsx`)

```
1. Google redirect về /auth/google/callback?code=xxx
2. Đọc "code" (hoặc "error" nếu user từ chối)
3. AuthService.googleLogin(code) → POST /auth/google
4. Nhận { user, accessToken, refreshToken } → lưu vào AuthContext
5. invalidate cache giỏ hàng theo user
6. Điều hướng: admin → /admin, user thường → /
```

> Dùng `useRef(hasRun)` chặn gọi 2 lần do React StrictMode — quan trọng vì `code` của Google **chỉ dùng được một lần**.

---

## 5. Luồng dữ liệu

```
1. User bấm "Đăng nhập bằng Google" trên AuthPage
2. FE redirect tới accounts.google.com (kèm client_id, redirect_uri, scope)
3. User đồng ý → Google redirect về /auth/google/callback?code=xxx
4. GoogleCallback.tsx → POST /auth/google { code }
5. GoogleAuthService.authenticateWithGoogle():
   a. exchangeCodeForToken(code)         → Google access token
   b. fetchGoogleUserInfo(token)         → { id, email, name, picture }
   c. findOrCreateUser():
        - googleId có?    → dùng lại + cập nhật avatar
        - email khớp?     → liên kết Google vào user cũ
        - đều không?      → tạo user mới (role USER, tạo Cart, password=null)
   d. generateAuthResponse(user)         → JWT access + refresh
6. FE lưu JWT, điều hướng theo role
```

---

## 6. Cấu hình Google Cloud Console

1. Vào [Google Cloud Console](https://console.cloud.google.com) → **APIs & Services → Credentials**.
2. Tạo **OAuth 2.0 Client ID** (loại *Web application*).
3. **Authorized redirect URIs**: thêm `http://localhost:3000/auth/google/callback` (và URL production tương ứng).
4. Lấy **Client ID** + **Client Secret** → điền vào:
   - Backend: `google.client-id`, `google.client-secret`, `google.redirect-uri`.
   - Frontend: `VITE_GOOGLE_CLIENT_ID`, `VITE_GOOGLE_REDIRECT_URI`.
5. (Tuỳ chọn) Cấu hình **OAuth consent screen**: tên app, scope `openid email profile`, thêm test users nếu app còn ở chế độ Testing.

> Cả 3 nơi (Google Console, backend, frontend) phải dùng **cùng một `redirect-uri`**, nếu lệch sẽ bị lỗi `redirect_uri_mismatch`.

---

## 7. Hướng dẫn kiểm tra

### Khởi động
```bash
# Backend
cd auto_accessories_store-be && mvn spring-boot:run     # http://localhost:8080/api/v1

# Frontend
cd store-fe && npm run dev                              # http://localhost:3000
```

### Kiểm tra end-to-end
1. Mở `http://localhost:3000/auth`, bấm **"Đăng nhập bằng Google"**.
2. Chọn tài khoản Google, đồng ý cấp quyền.
3. Được redirect về `/auth/google/callback` → hiện "Đang đăng nhập bằng Google...".
4. Đăng nhập thành công → vào trang chủ (hoặc `/admin` nếu là admin).
5. Kiểm tra log backend: `Google login: email=..., name=...` và `Created new Google user` (lần đầu).

### Kiểm tra liên kết tài khoản
1. Đăng ký bằng email `x@gmail.com` (password thường).
2. Đăng nhập Google bằng chính `x@gmail.com`.
3. Hệ thống **liên kết** (không tạo user mới) — `googleId` được set vào user cũ.

### Kiểm tra DB
```sql
SELECT id, username, email, google_id, avatar_url, password
FROM users WHERE email = 'x@gmail.com';
-- user Google: google_id != null, password = null
```

---

## 8. Giới hạn & hướng mở rộng

- **Không xác minh `email_verified`:** hiện tin tưởng email Google trả về; nên kiểm tra cờ `verified_email` trước khi liên kết theo email để tránh chiếm tài khoản.
- **Tự động liên kết theo email:** nếu email Google trùng user có sẵn, hệ thống tự gộp — tiện nhưng cần email đã verify để an toàn.
- **Chưa lưu Google refresh token:** mỗi lần đăng nhập đều đổi `code` mới; chưa dùng `access_type=offline` để gọi API Google thay người dùng về sau.
- **Mới hỗ trợ web:** mobile (Android) chưa tích hợp Google Sign-In; có thể thêm bằng Credential Manager rồi gửi `id_token`/`code` về cùng endpoint.
- **Phụ thuộc một provider:** có thể mở rộng thêm Facebook/Apple theo cùng kiến trúc (đổi code server-side → tạo/liên kết user → cấp JWT).

---

*Tài liệu cập nhật lần cuối: 2026-06-05*
