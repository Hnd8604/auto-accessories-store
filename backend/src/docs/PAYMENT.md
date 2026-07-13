# Thanh Toán Trực Tuyến — Tài liệu Kỹ thuật

Tài liệu này mô tả tính năng **Thanh toán trực tuyến** của hệ thống AutoLux Store: khách hàng thanh toán đơn hàng bằng chuyển khoản ngân hàng qua mã **QR VietQR**, hệ thống **tự động xác nhận** đơn đã thanh toán nhờ webhook (IPN) từ cổng [SePay](https://sepay.vn). Ngoài ra hệ thống vẫn hỗ trợ phương thức **COD** (thanh toán khi nhận hàng).

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc](#2-kiến-trúc)
3. [Backend](#3-backend)
   - [Cấu hình](#31-cấu-hình)
   - [Entities & Enums](#32-entities--enums)
   - [DTO](#33-dto)
   - [API Endpoints](#34-api-endpoints)
   - [Service](#35-service)
   - [Bảo mật webhook](#36-bảo-mật-webhook)
4. [Frontend](#4-frontend)
5. [Mobile](#5-mobile)
6. [Luồng dữ liệu](#6-luồng-dữ-liệu)
7. [Cấu hình SePay](#7-cấu-hình-sepay)
8. [Hướng dẫn kiểm tra](#8-hướng-dẫn-kiểm-tra)
9. [Giới hạn & hướng mở rộng](#9-giới-hạn--hướng-mở-rộng)

---

## 1. Tổng quan

### Vấn đề
Khách đặt hàng cần thanh toán trước qua chuyển khoản, nhưng admin không thể ngồi canh sao kê ngân hàng để đối chiếu từng giao dịch rồi xác nhận đơn thủ công — chậm và dễ sai sót.

### Giải pháp
Tích hợp cổng **SePay** đứng giữa ngân hàng và backend:

| Thành phần | Vai trò |
|------------|---------|
| VietQR | Sinh mã QR chứa sẵn số tài khoản, số tiền, nội dung CK |
| SePay | Lắng nghe biến động số dư tài khoản ngân hàng, đẩy **webhook** về backend |
| Backend | Nhận webhook → khớp đơn theo nội dung CK → tự cập nhật trạng thái `PAID` |
| Frontend | Hiển thị QR + **poll** trạng thái để biết khi nào đã thanh toán |

### Kết quả
- Khách quét QR bằng app ngân hàng, chuyển khoản trong vài giây.
- Đơn hàng **tự động** chuyển sang `PAID` mà không cần admin thao tác.
- FE tự phát hiện và hiện màn hình "Thanh toán thành công".

**Điểm cốt lõi:** Backend không chủ động hỏi ngân hàng. Đơn được khớp với giao dịch nhờ **nội dung chuyển khoản chính là `orderCode`**.

---

## 2. Kiến trúc

```
Khách hàng (FE)                 Backend (Spring Boot)              SePay + Ngân hàng
──────────────                  ─────────────────────             ─────────────────
PaymentDialog.tsx
  │
  ├── POST /payments/{orderId}/create ──► PaymentController
  │                                        └── PaymentService.createPayment()
  │                                            └── sinh qrCodeUrl (qr.sepay.vn)
  │   ◄──── PaymentResponse (QR + thông tin CK) ───────────────────
  │
  ├── (khách quét QR / chuyển khoản) ───────────────────────────────► App ngân hàng
  │                                                                       │
  │                                                                       ▼
  │                                                                Tiền vào TK
  │                                                                       │
  │                                                       SePay phát hiện ▼
  │                                        PaymentController ◄── POST /payments/sepay/webhook
  │                                        └── PaymentService.handleSepayWebhook()
  │                                            ├── verifyApiKey()
  │                                            ├── extractOrderCode(content)
  │                                            ├── đối chiếu số tiền
  │                                            ├── Order.paymentStatus = PAID
  │                                            └── lưu Payment
  │
  └── GET /payments/{orderId}/status  (poll mỗi 5s) ──► PaymentController
        ◄──── paymentStatus = PAID ───────────────────  PaymentService.checkPaymentStatus()
```

**Công nghệ:** Spring Boot REST + SePay Webhook (IPN) + VietQR. Không dùng WebSocket — FE phát hiện thay đổi bằng cơ chế **polling**.

---

## 3. Backend

### 3.1 Cấu hình

File `src/main/resources/application.yaml` (mẫu tại `application-example.yaml`):

```yaml
sepay:
  api-key: "YOUR_SEPAY_API_KEY"          # Key bí mật để xác thực webhook
  bank-code: "MBBank"                     # Mã ngân hàng theo chuẩn VietQR
  bank-account-number: "0123456789"       # Số tài khoản nhận tiền
  bank-account-name: "NGUYEN VAN A"       # Tên chủ tài khoản
  bank-name: "MB Bank"                    # Tên hiển thị cho khách
```

### 3.2 Entities & Enums

#### Bảng `payments`

File: `src/main/java/app/store/entity/Payment.java`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT PK | Kế thừa `BaseEntityLong`, tự tăng |
| `order_id` | FK NOT NULL | `@ManyToOne` đến `orders` |
| `amount` | DECIMAL NOT NULL | Số tiền giao dịch nhận được |
| `gateway` | VARCHAR | Tên ngân hàng (MBBank, VCB, ...) |
| `transaction_code` | VARCHAR | Mã giao dịch từ SePay |
| `reference_code` | VARCHAR | Mã tham chiếu |
| `transfer_content` | VARCHAR | Nội dung chuyển khoản |
| `account_number` | VARCHAR | Số tài khoản |
| `transaction_date` | VARCHAR | Ngày giao dịch (chuỗi từ SePay) |
| `status` | VARCHAR | `UNPAID` / `PAID` / `REFUNDED` |

#### Enums

File `enums/PaymentMethod.java`:

| Giá trị | Mô tả |
|---------|-------|
| `COD` | Thanh toán khi nhận hàng |
| `BANK_TRANSFER` | Chuyển khoản qua VietQR / SePay |

File `enums/PaymentStatus.java`: `UNPAID`, `PAID`, `REFUNDED`.

> Trạng thái thanh toán thực tế được lưu ở `Order.paymentStatus`; bảng `payments` lưu lại từng giao dịch nhận được (audit log).

### 3.3 DTO

#### `SepayWebhookRequest` — payload SePay gửi về

File: `src/main/java/app/store/dto/request/SepayWebhookRequest.java`

| Trường | Mô tả |
|--------|-------|
| `gateway` | Tên ngân hàng |
| `transactionDate` | Thời điểm giao dịch |
| `accountNumber` | Số tài khoản nhận |
| `transferType` | `in` = tiền vào, `out` = tiền ra |
| `transferAmount` | Số tiền giao dịch |
| `code` | Mã giao dịch |
| `content` | **Nội dung chuyển khoản — chứa `orderCode`** |
| `referenceCode` | Mã tham chiếu |
| `description` | Mô tả đầy đủ |

```json
{
  "id": 93,
  "gateway": "MBBank",
  "transactionDate": "2024-01-15 10:30:00",
  "accountNumber": "0123456789",
  "transferType": "in",
  "transferAmount": 500000,
  "code": "TF123456",
  "content": "DH20240115A1B2C3D4",
  "referenceCode": "FT24015ABCDE",
  "description": "MBBank-0123456789-DH20240115A1B2C3D4"
}
```

#### `PaymentResponse` — trả về FE

File: `src/main/java/app/store/dto/response/PaymentResponse.java`

| Trường | Mô tả |
|--------|-------|
| `orderId` | ID đơn hàng |
| `orderCode` | Mã đơn (= nội dung CK) |
| `amount` | Số tiền cần thanh toán |
| `qrCodeUrl` | URL ảnh QR VietQR |
| `bankName` / `bankAccountNumber` / `bankAccountName` | Thông tin tài khoản nhận |
| `paymentContent` | Nội dung CK bắt buộc nhập |
| `paymentStatus` | `UNPAID` / `PAID` / `REFUNDED` |

### 3.4 API Endpoints

File: `src/main/java/app/store/controller/PaymentController.java` — base path `/payments`

| Method | URL | Auth | Mô tả |
|--------|-----|------|-------|
| `POST` | `/payments/{orderId}/create` | User | Sinh QR thanh toán. Yêu cầu đơn chưa thanh toán & dùng `BANK_TRANSFER` |
| `GET` | `/payments/{orderId}/status` | User | Lấy trạng thái thanh toán hiện tại (FE poll endpoint này) |
| `POST` | `/payments/sepay/webhook` | **Public** (verify API key) | SePay gọi khi có giao dịch ngân hàng |

**Response mẫu `POST /payments/{orderId}/create`:**
```json
{
  "code": 1000,
  "result": {
    "orderId": "uuid-...",
    "orderCode": "DH20240115A1B2C3D4",
    "amount": 500000,
    "qrCodeUrl": "https://qr.sepay.vn/img?bank=MBBank&acc=0123456789&template=compact&amount=500000&des=DH20240115A1B2C3D4",
    "bankName": "MB Bank",
    "bankAccountNumber": "0123456789",
    "bankAccountName": "NGUYEN VAN A",
    "paymentContent": "DH20240115A1B2C3D4",
    "paymentStatus": "UNPAID"
  }
}
```

**Webhook nhận tại `POST /payments/sepay/webhook`:**
- Header `Authorization` chứa API key (dạng `Apikey <key>` hoặc `Bearer <key>`).
- Body là `SepayWebhookRequest` (xem mục 3.3).
- API key sai → trả `401`. Lỗi nghiệp vụ khác → trả `200 OK` để SePay **không retry spam**.

### 3.5 Service

File: `src/main/java/app/store/service/PaymentService.java`

| Method | Mô tả |
|--------|-------|
| `generateOrderCode()` | Sinh mã đơn `DH` + `yyyyMMdd` + 8 ký tự UUID viết hoa. VD: `DH20240115A1B2C3D4` |
| `createPayment(orderId)` | Kiểm tra đơn chưa `PAID` & là `BANK_TRANSFER` → sinh `qrCodeUrl` → trả `PaymentResponse` |
| `checkPaymentStatus(orderId)` | Trả trạng thái hiện tại; nếu còn `UNPAID` kèm lại `qrCodeUrl` |
| `handleSepayWebhook(request)` | Xử lý giao dịch (xem dưới) |
| `extractOrderCode(content)` | Trích `orderCode` bằng regex `DH\d{8}[A-Z0-9]{8}` |
| `buildQrCodeUrl(amount, content)` | Dựng URL ảnh `https://qr.sepay.vn/img?...` |
| `verifyApiKey(apiKey)` | Xác thực webhook, so sánh **constant-time** |

**Logic `handleSepayWebhook`:**

```
1. verifyApiKey(Authorization)             → sai key thì ném lỗi (401)
2. Bỏ qua nếu transferType != "in"         → chỉ xử lý tiền vào
3. Chuẩn hoá content (upper, bỏ space)
4. extractOrderCode(content)               → regex DH + 8 số + 8 ký tự
5. Tìm Order theo orderCode                → không thấy thì bỏ qua
6. Nếu Order đã PAID                        → bỏ qua (idempotent)
7. Nếu transferAmount < totalPrice         → lưu Payment(UNPAID), KHÔNG đánh dấu đơn
8. Đủ tiền                                  → Order.paymentStatus = PAID + lưu Payment(PAID)
```

**Sinh mã đơn:** `orderCode` vừa là định danh đơn, vừa là **nội dung chuyển khoản** để webhook khớp giao dịch. Đây là mắt xích kết nối toàn bộ luồng.

### 3.6 Bảo mật webhook

Webhook là endpoint **public** (SePay gọi từ ngoài, không có JWT của user), nên phải tự bảo vệ:

- **Verify API key:** so sánh header `Authorization` với `sepay.api-key`. Nếu thiếu xác thực, bất kỳ ai cũng có thể giả webhook để đánh dấu đơn `PAID` mà không trả tiền thật.
- **So sánh constant-time** (`constantTimeEquals`) chống dò key qua thời gian phản hồi (timing attack).
- **Đối chiếu số tiền:** chuyển thiếu tiền không làm đơn chuyển `PAID`.
- **Idempotent:** đơn đã `PAID` thì webhook lặp lại bị bỏ qua, tránh ghi trùng giao dịch.

---

## 4. Frontend

| File | Mô tả |
|------|-------|
| `features/orders/api/payments.ts` | Client gọi `createPayment`, `checkPaymentStatus` |
| `features/orders/components/PaymentDialog.tsx` | Dialog hiển thị QR + thông tin CK, **poll** trạng thái |
| `features/cart/components/Checkout.tsx` | Màn đặt hàng, chọn phương thức thanh toán |

**Hành vi `PaymentDialog`:**

```
Mở dialog
    ↓
createPayment(orderId) → hiển thị QR + thông tin CK (nút copy số TK / số tiền / nội dung CK)
    ↓
startPolling: checkPaymentStatus mỗi 5 giây (setInterval)
    ↓
paymentStatus === "PAID"  → dừng poll → màn hình "Thanh toán thành công" → onPaymentSuccess()
    ↓
Đóng dialog → clearInterval (tránh rò rỉ / poll thừa)
```

> FE poll vì không có kênh realtime từ BE; webhook chỉ cập nhật DB, FE phát hiện thay đổi qua poll (độ trễ tối đa ~5s).

---

## 5. Mobile

Android (Kotlin + Jetpack Compose) dùng chung API với web.

| File | Mô tả |
|------|-------|
| `feature/order/data/remote/OrderApi.kt` | Retrofit: `createPayment`, `getPaymentStatus` |
| `feature/order/data/model/OrderDto.kt` | `PaymentQrResponse` (qrCodeUrl, ...) |
| `feature/order/presentation/CheckoutScreen.kt` | Chọn phương thức `CASH` hoặc `BANK_TRANSFER` (VietQR) |

```kotlin
@POST("payments/{orderId}/create")
suspend fun createPayment(@Path("orderId") orderId: String): ApiResponse<PaymentQrResponse>

@GET("payments/{orderId}/status")
suspend fun getPaymentStatus(@Path("orderId") orderId: String): ApiResponse<String>
```

---

## 6. Luồng dữ liệu

### Luồng thanh toán thành công

```
1. Khách đặt hàng với paymentMethod = BANK_TRANSFER → BE sinh orderCode (DH...)
2. FE mở PaymentDialog → POST /payments/{orderId}/create
3. PaymentService.createPayment() trả qrCodeUrl + thông tin CK
4. FE hiển thị QR, bắt đầu poll GET /payments/{orderId}/status mỗi 5s
5. Khách quét QR → app ngân hàng chuyển tiền (nội dung CK = orderCode)
6. Ngân hàng ghi nhận tiền vào → SePay phát hiện
7. SePay → POST /payments/sepay/webhook (kèm API key)
8. PaymentService.handleSepayWebhook():
   a. verifyApiKey()
   b. extractOrderCode(content) → tìm Order
   c. đối chiếu số tiền đủ
   d. Order.paymentStatus = PAID + lưu Payment(PAID)
9. Lần poll kế tiếp của FE thấy status = PAID → màn "Thanh toán thành công"
```

### Luồng chuyển thiếu tiền

```
... bước 1–7 như trên ...
8. handleSepayWebhook: transferAmount < totalPrice
   → lưu Payment(UNPAID) làm bằng chứng giao dịch
   → KHÔNG đổi Order.paymentStatus
9. FE vẫn thấy status = UNPAID → tiếp tục chờ
```

### Luồng webhook giả mạo

```
1. Kẻ tấn công gọi POST /payments/sepay/webhook không có / sai API key
2. verifyApiKey() ném AppException
3. Controller trả 401 Unauthorized → đơn KHÔNG bị đánh dấu PAID
```

---

## 7. Cấu hình SePay

1. Đăng ký tài khoản [SePay](https://sepay.vn), liên kết tài khoản ngân hàng cần nhận tiền.
2. Tạo **API Key** → điền vào `sepay.api-key`.
3. Cấu hình **Webhook URL** trỏ tới `https://<domain-backend>/api/v1/payments/sepay/webhook`, kiểu xác thực **API Key** (header `Authorization`).
4. Đảm bảo `bank-code` và `bank-account-number` trùng tài khoản đã liên kết để QR sinh đúng.

---

## 8. Hướng dẫn kiểm tra

### Khởi động

```bash
# Backend
cd auto_accessories_store-be
mvn spring-boot:run        # http://localhost:8080/api/v1

# Frontend
cd store-fe
npm run dev                # http://localhost:3000
```

### Kiểm tra end-to-end (môi trường thật)

1. Đặt một đơn hàng với phương thức **Chuyển khoản ngân hàng (VietQR)**.
2. Dialog thanh toán hiển thị mã QR + thông tin tài khoản.
3. Dùng app ngân hàng quét QR và chuyển đúng số tiền, giữ nguyên nội dung CK.
4. Sau khi SePay đẩy webhook (vài giây), dialog tự chuyển sang "Thanh toán thành công".

### Giả lập webhook (môi trường dev)

```bash
curl -X POST http://localhost:8080/api/v1/payments/sepay/webhook \
  -H "Authorization: Apikey YOUR_SEPAY_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
        "gateway": "MBBank",
        "transactionDate": "2024-01-15 10:30:00",
        "accountNumber": "0123456789",
        "transferType": "in",
        "transferAmount": 500000,
        "code": "TF123456",
        "content": "DH20240115A1B2C3D4",
        "referenceCode": "FT24015ABCDE"
      }'
```

Thay `content` bằng `orderCode` thật của đơn cần test và `transferAmount` ≥ tổng tiền đơn.

### Kiểm tra DB

```sql
SELECT * FROM payments ORDER BY created_at DESC LIMIT 10;
SELECT id, order_code, payment_method, payment_status FROM orders ORDER BY created_at DESC LIMIT 10;
```

---

## 9. Giới hạn & hướng mở rộng

- Chưa có WebSocket/SSE → FE phải **poll** (độ trễ tối đa ~5s). Có thể nâng cấp đẩy realtime khi webhook xử lý xong.
- `REFUNDED` đã có trong enum nhưng **chưa có luồng hoàn tiền** triển khai.
- Chưa có cơ chế **hết hạn QR** / tự huỷ đơn chưa thanh toán sau thời gian nhất định.
- Giao dịch **chuyển thiếu tiền** được lưu nhưng chưa có quy trình thông báo / xử lý chênh lệch.
- `transactionDate` đang lưu dạng chuỗi từ SePay — nên chuẩn hoá về kiểu thời gian nếu cần truy vấn theo ngày.

---

*Tài liệu cập nhật lần cuối: 2026-06-05*
