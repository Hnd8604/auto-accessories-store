# Thanh Toán Trực Tuyến — Tài liệu Kỹ thuật

Tài liệu này mô tả tính năng **Thanh toán trực tuyến** của hệ thống AutoLux Store: khách thanh toán đơn bằng chuyển khoản ngân hàng qua cổng [payOS](https://payos.vn). Backend tạo **link thanh toán** trên payOS, frontend **nhúng trang checkout của payOS** vào dialog, và đơn được **tự động xác nhận** nhờ webhook của payOS hoặc đối soát qua API. Hệ thống vẫn hỗ trợ **COD** (thanh toán khi nhận hàng).

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc](#2-kiến-trúc)
3. [Backend](#3-backend)
   - [Cấu hình](#31-cấu-hình)
   - [Bảng dữ liệu & Enums](#32-bảng-dữ-liệu--enums)
   - [API Endpoints](#33-api-endpoints)
   - [Service](#34-service)
   - [Xác thực webhook](#35-xác-thực-webhook)
   - [Chống xử lý trùng](#36-chống-xử-lý-trùng)
   - [Mã phản hồi webhook](#37-mã-phản-hồi-webhook)
4. [Frontend](#4-frontend)
5. [Luồng dữ liệu](#5-luồng-dữ-liệu)
6. [Cấu hình payOS](#6-cấu-hình-payos)
7. [Hướng dẫn kiểm tra](#7-hướng-dẫn-kiểm-tra)
8. [Rủi ro & hướng mở rộng](#8-rủi-ro--hướng-mở-rộng)

---

## 1. Tổng quan

| Thành phần | Vai trò |
|------------|---------|
| payOS | Tạo link thanh toán (trang checkout có QR VietQR), theo dõi tiền vào, gửi **webhook** và cho tra cứu trạng thái link qua API |
| Backend | Tạo/dùng lại link, xác thực webhook, đối soát với payOS, cập nhật `PAID` |
| Frontend | Nhúng checkout payOS vào dialog, **poll** backend để biết khi nào đã thanh toán |

**Điểm cốt lõi:** payOS bắt buộc `orderCode` là **số nguyên** và không nhận trùng, nên không dùng lại được `orders.order_code` (dạng `DH20240115A1B2C3D4`). Mỗi link lấy một số từ sequence `payos_order_code_seq` và được lưu vào bảng `payos_payment_links`; webhook khớp đơn qua số này chứ không qua nội dung chuyển khoản.

Trạng thái đơn có **hai nguồn** cập nhật, cùng đi qua một hàm ghi nhận giao dịch:

1. **Webhook** payOS gọi `POST /payments/payos/webhook`.
2. **Đối soát**: mỗi lần FE poll `GET /payments/{orderId}/status` khi đơn còn `UNPAID`, backend gọi API tra cứu link của payOS. Nhờ vậy đơn vẫn được xác nhận khi webhook chậm, bị lỡ, hoặc chưa cấu hình (dev local không có URL public).

---

## 2. Kiến trúc

```
Khách hàng (FE)                    Backend (Spring Boot)                     payOS
──────────────                     ─────────────────────                     ─────
PaymentDialog.tsx
  │
  ├── POST /payments/{orderId}/create ──► PaymentService.createPayment()
  │                                        ├── có link gần nhất? ── GET /v2/payment-requests/{id} ──►
  │                                        │     PAID → ghi nhận, trả PAID
  │                                        │     PENDING/PROCESSING & còn hạn → dùng lại
  │                                        └── ngược lại ── POST /v2/payment-requests ──►
  │   ◄──── PaymentResponse (checkoutUrl) ─────────────────────────────
  │
  ├── nhúng iframe checkoutUrl (@payos/payos-checkout) ──────────────────► trang checkout
  │                                                                          │ khách quét QR
  │                                                                          ▼
  │                                   [permitAll] ◄──────── POST /payments/payos/webhook
  │                                    └── PayosGateway.verifyWebhook()  (chữ ký trong body)
  │                                        └── PaymentService.handlePayosWebhook()
  │
  └── GET /payments/{orderId}/status (poll 5s) ──► PaymentService.checkPaymentStatus()
        ◄──── paymentStatus ───────────────────     └── UNPAID → đối soát qua GET link ──►
```

**Công nghệ:** Spring Boot REST + SDK `vn.payos:payos-java:2.0.1` + `@payos/payos-checkout@1.0.8`. FE phát hiện thay đổi bằng **polling**.

---

## 3. Backend

### 3.1 Cấu hình

`src/main/resources/application.yaml` (mẫu tại `application.yaml.example`):

```yaml
payos:
  client-id: ${PAYOS_CLIENT_ID}
  api-key: ${PAYOS_API_KEY}
  checksum-key: ${PAYOS_CHECKSUM_KEY}          # ký request và xác thực chữ ký webhook
  return-url: ${PAYOS_RETURN_URL:http://localhost:3000/}
  cancel-url: ${PAYOS_CANCEL_URL:http://localhost:3000/}
  link-expiry-minutes: ${PAYOS_LINK_EXPIRY_MINUTES:15}
```

- Ba key lấy ở my.payos.vn → kênh thanh toán → thông tin tích hợp.
- `return-url`/`cancel-url` là tham số bắt buộc khi tạo link. Ở profile `prod` chúng suy ra từ `APP_DOMAIN` (`application-prod.yaml`).
- Bean `PayOS` dựng trong `config/PayosConfig.java`, timeout 15s cho mỗi lần thử (mặc định của SDK là 60s). SDK tự thử lại tối đa 2 lần khi lỗi kết nối hoặc HTTP 408/429/5xx.

### 3.2 Bảng dữ liệu & Enums

#### Bảng `payos_payment_links` — `V1__init_schema.sql`

File: `entity/PayosPaymentLink.java`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT PK | `BaseEntityLong` |
| `order_id` | FK NOT NULL | Đơn hàng, `ON DELETE CASCADE` |
| `payos_order_code` | BIGINT UNIQUE | `orderCode` gửi sang payOS, lấy từ `payos_order_code_seq` |
| `payment_link_id` | VARCHAR UNIQUE | ID link do payOS trả về |
| `checkout_url` | VARCHAR(1024) | Trang checkout |
| `expires_at` | TIMESTAMP | Hạn link (`expiredAt` gửi cho payOS) |

Một đơn có thể có **nhiều link** theo thời gian (link cũ hết hạn/bị huỷ → tạo link mới). Giữ lại mọi link để tiền trả vào link cũ vẫn tra ngược ra được đơn.

#### Bảng `payments`

Giữ nguyên schema, mỗi dòng là một giao dịch tiền vào (audit log). Unique index `ux_payments_reference_code` (V3) là khoá chống ghi trùng.

| Cột | Giá trị từ payOS |
|-----|------------------|
| `gateway` | `PAYOS` |
| `transaction_code` | `paymentLinkId` |
| `reference_code` | `reference` (mã giao dịch ngân hàng) |
| `transfer_content` | `description` |
| `account_number` | `accountNumber` |
| `transaction_date` | `transactionDateTime` (chuỗi) |
| `amount`, `status` | Số tiền; `PAID`/`UNPAID` |

#### `WebhookOutcome`

Kết quả ghi nhận một giao dịch (dùng cho cả webhook và đối soát):

| Giá trị | Ý nghĩa | Ghi Payment? | Đổi trạng thái đơn? |
|---------|---------|--------------|---------------------|
| `PAID` | Đủ tiền | ✅ `PAID` | ✅ → `PAID` |
| `ORDER_ALREADY_PAID` | Đơn đã `PAID` nhưng có thêm tiền vào | ✅ `PAID` | ❌ |
| `UNDERPAID` | Thiếu tiền so với tổng đơn | ✅ `UNPAID` | ❌ |
| `DUPLICATE_TRANSACTION` | `reference` đã ghi nhận | ❌ | ❌ |
| `NOT_SUCCESS` | `data.code` khác `"00"` | ❌ | ❌ |
| `ORDER_NOT_FOUND` | Không link nào có `orderCode` này | ❌ | ❌ |
| `INVALID_PAYLOAD` | Thiếu `orderCode`/`amount`/`reference` | ❌ | ❌ |

> Mọi outcome là **quyết định cuối cùng** → trả `200`. Lỗi tạm thời (mất kết nối DB…) không biểu diễn bằng outcome mà ném exception thành `5xx`.

### 3.3 API Endpoints

File: `controller/PaymentController.java` — base path `/payments` (context path `/api/v1`)

| Method | URL | Auth | Mô tả |
|--------|-----|------|-------|
| `POST` | `/payments/{orderId}/create` | JWT | Trả link thanh toán (dùng lại hoặc tạo mới). Đơn phải `BANK_TRANSFER`, chưa `PAID`, chưa huỷ |
| `GET` | `/payments/{orderId}/status` | JWT | Trạng thái thanh toán, có đối soát với payOS khi còn `UNPAID` |
| `POST` | `/payments/payos/webhook` | **Chữ ký payOS** (kiểm trong controller) | payOS gọi khi có thanh toán |

**Quyền truy cập đơn:** người có `ORDER_GET_BY_ID` xem mọi đơn; người khác chỉ đơn của mình (`findByIdAndUserUsername`). Đơn của người khác trả `ORDER_NOT_EXISTED` (404) như đơn không tồn tại.

**Response mẫu `POST /payments/{orderId}/create`:**
```json
{
  "code": 1000,
  "result": {
    "orderId": "uuid-...",
    "orderCode": "DH20240115A1B2C3D4",
    "amount": 500000,
    "paymentStatus": "UNPAID",
    "checkoutUrl": "https://pay.payos.vn/web/124c33293c43417ab7879e14c8d9eb18",
    "paymentLinkId": "124c33293c43417ab7879e14c8d9eb18",
    "expiredAt": "2026-09-13T10:15:00"
  }
}
```

Khi đơn đã `PAID`, `checkoutUrl`/`paymentLinkId`/`expiredAt` là `null`.

**Response mẫu webhook:** `{"success": true, "outcome": "PAID", "message": "Order has been marked as paid"}`

**Lỗi gọi payOS** (mạng, payOS từ chối…): `PayosGateway` log `status/code/desc` của payOS rồi ném `ErrorCode.PAYMENT_GATEWAY_ERROR` → `502`.

### 3.4 Service

File: `service/PaymentService.java`

| Method | Mô tả |
|--------|-------|
| `generateOrderCode()` | Mã đơn hiển thị `DH` + `yyyyMMdd` + 8 ký tự |
| `createPayment(orderId)` | Xem bảng dưới |
| `checkPaymentStatus(orderId)` | Trả trạng thái DB; nếu `UNPAID` và có link → `getLink` rồi ghi nhận các `transactions`. Lỗi payOS/DB khi đối soát chỉ log, vẫn trả trạng thái DB |
| `handlePayosWebhook(data)` | `@Transactional`: `code != "00"` → `NOT_SUCCESS`; thiếu field → `INVALID_PAYLOAD`; không thấy link → `ORDER_NOT_FOUND`; còn lại ghi nhận giao dịch |
| `cancelOpenPaymentLink(order)` | `OrderService.cancelOrder` gọi: huỷ link còn hạn của đơn chuyển khoản chưa trả. Lỗi chỉ log |

**`createPayment` với link gần nhất của đơn:**

| Trạng thái link trên payOS | Hành động |
|----------------------------|-----------|
| Có giao dịch làm đơn đủ tiền | Ghi nhận → trả `PAID`, **không** mở link mới (tránh trả hai lần khi webhook chưa tới) |
| `PENDING`/`PROCESSING`, còn hạn quá 2 phút | Dùng lại link |
| `PENDING`/`PROCESSING`, sắp/đã hết hạn | Huỷ link cũ trên payOS, tạo link mới |
| `CANCELLED`, `EXPIRED`, `UNDERPAID`, `FAILED` | Tạo link mới |
| Chưa có link | Tạo link mới |

**Tham số tạo link:**
- `orderCode` = `nextval('payos_order_code_seq')`
- `amount` = `totalPrice` dạng số nguyên (`numeric(38,2)` có phần lẻ khác 0 → `IllegalArgumentException`)
- `description` = `DH` + tối đa 7 chữ số cuối của `orderCode` — tài liệu payOS giới hạn **9 ký tự** với tài khoản ngân hàng không liên kết qua payOS
- `expiredAt` = bây giờ + `link-expiry-minutes`

### 3.5 Xác thực webhook

Webhook không có JWT, nên `POST /payments/payos/webhook` nằm trong `permitAll` của `SecurityConfig`, và `PaymentController` gọi `PayosGateway.verifyWebhook(body)` **ngay dòng đầu**. Đây là chốt chặn duy nhất.

Theo [tài liệu payOS](https://payos.vn/docs/tich-hop-webhook/kiem-tra-du-lieu-voi-signature/), `signature` nằm **trong body**, là HMAC-SHA256 bằng **Checksum Key** trên các field của `data` sắp xếp theo key, dạng `key1=value1&key2=value2`, giá trị null thành chuỗi rỗng. Payload:

```json
{
  "code": "00", "desc": "success", "success": true,
  "data": {
    "orderCode": 123, "amount": 3000, "description": "VQRIO123",
    "accountNumber": "12345678", "reference": "TF230204212323",
    "transactionDateTime": "2023-02-04 18:25:00", "currency": "VND",
    "paymentLinkId": "124c33293c43417ab7879e14c8d9eb18", "code": "00", "desc": "Thành công",
    "counterAccountBankId": "", "counterAccountBankName": "", "counterAccountName": "",
    "counterAccountNumber": "", "virtualAccountName": "", "virtualAccountNumber": ""
  },
  "signature": "8d8640d802576397a1ce45ebda7f835055768ac7ad2e0bfb77f9b8f12cca4c7f"
}
```

Việc kiểm chữ ký giao cho SDK (`payOS.webhooks().verify(...)`). Đặc điểm, đọc từ source SDK 2.0.1:

- SDK parse body vào lớp `WebhookData` với **danh sách field cố định** ở trên rồi ký lại từ object đó. Nếu một ngày payOS thêm field mới vào `data`, chữ ký sẽ lệch và **mọi webhook bị từ chối** (fail-closed) cho tới khi nâng SDK — trong lúc đó đơn vẫn được xác nhận nhờ đối soát qua API.
- Mọi lỗi (sai/thiếu chữ ký, body không phải JSON) → `AppException(WEBHOOK_INVALID_SIGNATURE)` → `401`. Log chỉ ghi tên lớp exception, không ghi chữ ký hay key.
- Chữ ký **không có timestamp** nên không có cửa sổ chống replay như SePay trước đây. Một webhook hợp lệ bị phát lại sẽ rơi vào `DUPLICATE_TRANSACTION` (mục 3.6) và không đổi được gì.
- SDK so sánh chữ ký bằng `String.equals` (không constant-time).

### 3.6 Chống xử lý trùng

Cùng một giao dịch đến từ **cả** webhook lẫn đối soát, và webhook có thể bị phát lại.

| Tầng | Cơ chế | Bắt được gì |
|------|--------|-------------|
| Service | `existsByReferenceCode(reference)` → `DUPLICATE_TRANSACTION` | Lần đến sau khi lần trước đã commit |
| Database | Unique index `ux_payments_reference_code` | Webhook và đối soát chạy **song song** |

Khi vi phạm unique index:
- Trong webhook (`@Transactional`): rollback cả việc set `PAID`, unique violation thành `409 DATA_CONFLICT`. Lần xử lý sau thấy bản ghi của luồng thắng → `DUPLICATE_TRANSACTION`.
- Trong đối soát (không transaction bao ngoài, mỗi `save` tự commit): `DataAccessException` bị bắt và log, response vẫn trả trạng thái DB; lần poll sau sẽ thấy `PAID`.

### 3.7 Mã phản hồi webhook

Tài liệu payOS yêu cầu phản hồi **mã 2XX** để xác nhận đã nhận webhook. Tài liệu hiện không mô tả chính sách gửi lại khi endpoint lỗi, cũng không mô tả cách payOS kiểm tra URL lúc lưu webhook — đừng dựa vào việc payOS sẽ gửi lại; đối soát qua API là lưới an toàn.

| Tình huống | HTTP |
|------------|------|
| Xử lý xong (mọi `WebhookOutcome`, kể cả `ORDER_NOT_FOUND`) | 200 + `success: true` |
| Sai/thiếu chữ ký, body không phải JSON | 401 |
| Xung đột unique/FK trong DB | 409 (`DATA_CONFLICT`) |
| Lỗi hạ tầng hoặc integrity không dự kiến | 500 (`UNCATEGORIZED_EXCEPTION`) |

Controller **không** bọc try/catch: nuốt lỗi DB thành `200` từng làm mất giao dịch có thật ở bản cũ.

---

## 4. Frontend

| File | Mô tả |
|------|-------|
| `features/orders/api/payments.ts` | `createPayment`, `checkPaymentStatus` |
| `features/orders/components/PaymentDialog.tsx` | Nhúng checkout payOS, poll trạng thái |
| `features/cart/components/Checkout.tsx` | Chọn phương thức thanh toán, mở `PaymentDialog` |

**Thư viện `@payos/payos-checkout@1.0.8`** (đọc từ source):
- `usePayOS(config)` **không phải React hook**: mỗi lần gọi tạo một đối tượng checkout mới với `open()`/`exit()`. Component import dưới tên `createPayOSCheckout`, tạo đúng một đối tượng cho mỗi `checkoutUrl` trong `useEffect` và `exit()` khi cleanup.
- `embedded: true` → iframe `src` = `checkoutUrl` đổi `/web/` thành `/embedded/`, cao `100%` khung chứa → khung `#payos-embedded-checkout` phải có chiều cao cố định.
- Chỉ nhận `postMessage` từ `https://pay.payos.vn`, `https://dev.pay.payos.vn`, `https://next.dev.pay.payos.vn`. Thư viện tự gỡ iframe trước khi gọi `onSuccess` (status `PAID`), `onCancel` (status `CANCELLED`), `onExit` (khách đóng hoặc lỗi).

**Hành vi `PaymentDialog`:**

```
Mở dialog → createPayment(orderId)
   ├── paymentStatus = PAID → màn "Thanh toán thành công"
   └── có checkoutUrl → nhúng iframe payOS + poll /status mỗi 5s
          ├── onSuccess → "Đang xác nhận..." + gọi /status ngay
          │                (KHÔNG tin sự kiện trình duyệt; chỉ backend báo PAID mới là thành công)
          ├── onCancel / onExit → "Bạn đã huỷ / Đã đóng" + nút "Thanh toán lại" (gọi lại createPayment)
          └── /status = PAID → dừng poll → màn thành công → onPaymentSuccess()
Đóng dialog → dừng poll, gỡ iframe
```

`onPaymentSuccess` của parent được giữ qua `ref`: nếu đưa thẳng vào dependency, mỗi lần `Checkout` render lại sẽ làm effect nhúng chạy lại và tải lại iframe giữa chừng.

---

## 5. Luồng dữ liệu

### Thanh toán thành công

```
1. Khách đặt đơn BANK_TRANSFER → BE sinh orderCode (DH...)
2. FE mở PaymentDialog → POST /payments/{orderId}/create
3. BE lấy payos_order_code từ sequence, gọi payOS tạo link, lưu payos_payment_links
4. FE nhúng checkoutUrl, poll GET /payments/{orderId}/status mỗi 5s
5. Khách quét QR → tiền vào
6a. payOS → POST /payments/payos/webhook → verify chữ ký → handlePayosWebhook
    → tìm link theo orderCode → đủ tiền → Order PAID + Payment(PAID)
6b. (hoặc) lần poll kế tiếp → getLink thấy transaction → ghi nhận như trên
7. Lần poll kế tiếp thấy PAID → màn "Thanh toán thành công"
```

### Khách huỷ rồi thanh toán lại

```
1. Khách bấm huỷ trên checkout → payOS huỷ link → onCancel → FE hiện "Thanh toán lại"
2. POST /create → getLink trả CANCELLED → tạo link mới với payos_order_code mới
3. Link cũ vẫn nằm trong payos_payment_links, nên nếu có tiền vào link cũ vẫn khớp được đơn
```

### Webhook giả mạo

```
1. POST /payments/payos/webhook với chữ ký sai/thiếu
2. PayosGateway.verifyWebhook ném AppException ngay dòng đầu controller
3. 401 (code 3201) — PaymentService không được gọi, đơn không đổi
```

### Huỷ đơn chưa thanh toán

```
OrderService.cancelOrder → hoàn kho → paymentService.cancelOpenPaymentLink(order)
→ huỷ link còn hạn trên payOS (lỗi chỉ log, không chặn việc huỷ đơn)
```

---

## 6. Cấu hình payOS

1. Tạo kênh thanh toán trên my.payos.vn, liên kết tài khoản ngân hàng nhận tiền.
2. Copy **Client ID**, **API Key**, **Checksum Key** vào `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY` (`.env.dev` / `.env.prod`).
3. Khai **Webhook URL** `https://<APP_DOMAIN>/api/v1/payments/payos/webhook`. SDK cũng có `payOS.webhooks().confirm(url)` gọi API `POST /confirm-webhook` nếu muốn khai bằng code.
4. Dev local không có URL public: không cần webhook, trạng thái vẫn cập nhật qua đối soát khi dialog đang poll. Muốn thử webhook thật thì expose backend bằng tunnel (cloudflared, ngrok).

---

## 7. Hướng dẫn kiểm tra

### Unit test

```bash
cd backend
./mvnw test -Dtest='PaymentServiceTest,PayosGatewayTest,PaymentControllerTest,OrderServiceTest'
```

- `PaymentServiceTest` — 29 test Mockito: tạo/dùng lại/thay link, giới hạn `description`, số tiền lẻ, đơn đã trả trên payOS nhưng chưa có webhook, đối soát khi poll, mọi nhánh `WebhookOutcome`, huỷ link khi huỷ đơn.
- `PayosGatewayTest` — 9 test chạy **SDK thật** (không mock) để bắt lỗi tương thích với Jackson do Spring Boot quản lý (SDK khai 2.20.0, dự án dùng 2.18.3). Chữ ký webhook neo vào vector tính bằng `openssl`; API payOS giả lập bằng `HttpServer` của JDK để kiểm chữ ký request tạo link, parse response, và đổi lỗi payOS thành `PAYMENT_GATEWAY_ERROR`.
- `PaymentControllerTest` — 2 test: verify trước rồi mới gọi service; sai chữ ký thì service không được gọi.

### Giả lập webhook (dev)

Tính chữ ký theo mục 3.5 rồi gửi (bash + `jq` + `openssl`):

```bash
KEY="$PAYOS_CHECKSUM_KEY"
DATA='{"orderCode":1,"amount":500000,"description":"DH1","accountNumber":"12345678","reference":"FT-TEST-001","transactionDateTime":"2026-09-13 10:00:00","currency":"VND","paymentLinkId":"<paymentLinkId>","code":"00","desc":"success","counterAccountBankId":"","counterAccountBankName":"","counterAccountName":"","counterAccountNumber":"","virtualAccountName":"","virtualAccountNumber":""}'
CANON=$(printf '%s' "$DATA" | jq -r 'to_entries | sort_by(.key) | map("\(.key)=\(.value // "")") | join("&")')
SIG=$(printf '%s' "$CANON" | openssl dgst -sha256 -hmac "$KEY" | awk '{print $NF}')

curl -i -X POST http://localhost:8080/api/v1/payments/payos/webhook \
  -H "Content-Type: application/json" \
  -d "{\"code\":\"00\",\"desc\":\"success\",\"success\":true,\"data\":$DATA,\"signature\":\"$SIG\"}"
# → 200 {"outcome":"PAID"} nếu orderCode khớp một link và amount >= tổng đơn
# Gửi lại y hệt → {"outcome":"DUPLICATE_TRANSACTION"}
# Đổi amount mà không ký lại → 401
```

Thay `orderCode` bằng `payos_order_code` thật: `SELECT payos_order_code, payment_link_id FROM payos_payment_links ORDER BY id DESC LIMIT 5;`

### End-to-end (payOS thật)

1. Điền `PAYOS_*`, đặt một đơn **Chuyển khoản ngân hàng** giá trị nhỏ.
2. Dialog hiện checkout payOS nhúng.
3. Quét QR, chuyển tiền → dialog "Đang xác nhận" rồi "Thanh toán thành công".
4. Kiểm DB: `orders.payment_status = 'PAID'`, một dòng `payments` với `gateway = 'PAYOS'`.
5. Đặt đơn khác, bấm huỷ trên checkout → "Thanh toán lại" mở link mới.
6. Huỷ một đơn chưa trả → link trên my.payos.vn chuyển trạng thái huỷ.

---

## 8. Rủi ro & hướng mở rộng

- **Đối soát gọi payOS mỗi 5s** trong lúc dialog mở và đơn `UNPAID` (một request `GET` link mỗi lần poll). Tài liệu payOS không nêu hạn mức; nếu gặp HTTP 429 nên giãn chu kỳ hoặc chỉ đối soát sau `onSuccess`.
- **Đối soát chỉ xem link gần nhất** của đơn. Tiền vào link cũ hơn chỉ được ghi nhận qua webhook.
- **Tạo link đồng thời** cho cùng một đơn (hai tab, hoặc `StrictMode` ở dev gọi effect hai lần) có thể sinh hai link còn hiệu lực. Cả hai đều khớp được đơn nên không mất tiền, nhưng khách có thể trả hai lần → thấy qua `ORDER_ALREADY_PAID`.
- **`ORDER_NOT_FOUND`/`UNDERPAID`/`ORDER_ALREADY_PAID` chỉ log** — nên nối vào `NotificationService` (`PAYMENT_RECEIVED` đã khai báo) để báo admin.
- **Chưa có luồng hoàn tiền** (`REFUNDED` chưa dùng) và **chưa tự huỷ đơn `UNPAID`** quá hạn.
- **Polling thay vì realtime** — có thể tái dùng `SseEmitterService`.
- `transactionDate` vẫn lưu dạng chuỗi.

---

*Tài liệu cập nhật lần cuối: 2026-09-13*
