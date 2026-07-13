# Live Chat — Tài liệu Kỹ thuật

Tài liệu này mô tả tính năng **Live Chat** được tích hợp vào hệ thống AutoLux Store: khách hàng chat trực tiếp từ trang web, admin nhận và trả lời tập trung tại trang `/admin/inbox`.

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc](#2-kiến-trúc)
3. [Backend](#3-backend)
   - [Dependency](#31-dependency)
   - [Entities & Database](#32-entities--database)
   - [WebSocket Config](#33-websocket-config)
   - [API Endpoints](#34-api-endpoints)
   - [Services](#35-services)
   - [Security](#36-security)
4. [Frontend](#4-frontend)
   - [Packages](#41-packages)
   - [Cấu trúc thư mục](#42-cấu-trúc-thư-mục)
   - [STOMP Hook](#43-stomp-hook)
   - [ChatWidget (khách hàng)](#44-chatwidget-khách-hàng)
   - [Inbox Admin](#45-inbox-admin)
5. [Luồng dữ liệu](#5-luồng-dữ-liệu)
6. [Hướng dẫn chạy & kiểm tra](#6-hướng-dẫn-chạy--kiểm-tra)
7. [Mở rộng Phase 2 — Zalo & Messenger](#7-mở-rộng-phase-2--zalo--messenger)

---

## 1. Tổng quan

### Vấn đề
Khách hàng liên hệ qua 3 kênh (website, Zalo OA, Facebook Messenger), admin phải mở từng ứng dụng riêng để xem và trả lời — mất thời gian, dễ bỏ sót.

### Giải pháp — Phase 1 (đã triển khai)
Xây dựng **Website Live Chat** 2 chiều hoàn chỉnh:

| Kênh | Nhận tin | Gửi tin | Ghi chú |
|------|----------|---------|---------|
| Website | Admin dashboard | Admin dashboard | Real-time, WebSocket |
| Zalo OA | *(Phase 2)* | Dùng app Zalo trực tiếp | API gửi mất phí |
| Messenger | *(Phase 2)* | Dùng app Facebook trực tiếp | API gửi mất phí |

### Kết quả
- Khách hàng chat qua widget nút tròn góc dưới phải trang chủ
- Admin xem và trả lời tại `/admin/inbox` — không cần chuyển tab
- Tin nhắn real-time, không cần refresh trang

---

## 2. Kiến trúc

```
Khách hàng (trang chủ)              Backend (Spring Boot)               Admin (/admin/inbox)
──────────────────────              ─────────────────────               ───────────────────
ChatWidget.tsx
  │
  ├── POST /conversations ─────────► ConversationController ──────────► Tạo conversation mới
  │                                   └── ConversationService
  │                                       └── ConversationRepository (MySQL)
  │
  ├── STOMP /ws ───────────────────► WebSocketConfig (STOMP broker)
  │     └── SEND /app/chat.send ──► ChatController
  │                                   └── ChatMessageService
  │                                       ├── Lưu vào MySQL
  │                                       ├── broadcast /topic/conversation/{id}  ──────────► ChatWindow.tsx
  │                                       └── broadcast /topic/admin/new-message ──────────► InboxPage.tsx (cập nhật list)
  │
  └── SUBSCRIBE /topic/conversation/{id} ◄── broadcast từ ChatMessageService


Luồng admin gửi tin:
Admin (ChatWindow) → STOMP /app/chat.send → ChatMessageService → broadcast → ChatWidget khách nhận
```

**Công nghệ real-time:** Spring WebSocket + STOMP + SockJS (fallback cho môi trường không hỗ trợ WebSocket thuần)

---

## 3. Backend

### 3.1 Dependency

Thêm vào `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 3.2 Entities & Database

JPA tự động tạo bảng qua `spring.jpa.hibernate.ddl-auto`.

#### Bảng `conversations`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | VARCHAR(36) PK | UUID tự sinh |
| `guest_name` | VARCHAR(255) NOT NULL | Tên khách nhập khi bắt đầu chat |
| `channel` | VARCHAR(50) | `WEB` / `ZALO` / `MESSENGER` |
| `status` | VARCHAR(20) | `OPEN` / `CLOSED` |
| `unread_count` | INT | Số tin chưa đọc (phía admin) |
| `last_message_at` | DATETIME | Thời điểm tin nhắn cuối |
| `created_at` | DATETIME | JPA auditing tự điền |
| `updated_at` | DATETIME | JPA auditing tự điền |

File: `src/main/java/app/store/entity/Conversation.java`

#### Bảng `chat_messages`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | VARCHAR(36) PK | UUID tự sinh |
| `conversation_id` | VARCHAR(36) NOT NULL | FK logic đến `conversations.id` |
| `sender_type` | VARCHAR(20) | `CUSTOMER` / `ADMIN` |
| `content` | VARCHAR(2000) NOT NULL | Nội dung tin nhắn |
| `created_at` | DATETIME | JPA auditing tự điền |

Index: `idx_chat_message_conversation(conversation_id, created_at)` — tăng tốc truy vấn lịch sử.

File: `src/main/java/app/store/entity/ChatMessage.java`

### 3.3 WebSocket Config

File: `src/main/java/app/store/config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");      // prefix kênh subscribe
        registry.setApplicationDestinationPrefixes("/app"); // prefix kênh gửi
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS fallback tự động
    }
}
```

**STOMP topics được sử dụng:**

| Topic | Mô tả |
|-------|-------|
| `/topic/conversation/{id}` | Tin nhắn mới trong 1 hội thoại cụ thể |
| `/topic/admin/new-message` | Thông báo cho admin panel khi có tin từ khách |

### 3.4 API Endpoints

#### REST — ConversationController
`src/main/java/app/store/controller/ConversationController.java`

| Method | URL | Auth | Mô tả |
|--------|-----|------|-------|
| `POST` | `/conversations` | Public | Khách tạo hội thoại mới, trả về `conversationId` |
| `GET` | `/conversations` | ADMIN | Lấy danh sách hội thoại (phân trang, sắp xếp theo `lastMessageAt` mới nhất) |
| `GET` | `/conversations/{id}/messages` | Public | Lấy lịch sử tin nhắn (phân trang, sắp xếp cũ → mới) |
| `PUT` | `/conversations/{id}/read` | ADMIN | Đặt `unreadCount = 0` |
| `PUT` | `/conversations/{id}/close` | ADMIN | Đặt `status = CLOSED` |
| `GET` | `/conversations/unread-count` | ADMIN | Tổng `unreadCount` tất cả hội thoại đang OPEN |

**Request body `POST /conversations`:**
```json
{ "guestName": "Nguyễn Văn A" }
```

**Response mẫu:**
```json
{
  "code": 1000,
  "result": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "guestName": "Nguyễn Văn A",
    "channel": "WEB",
    "status": "OPEN",
    "unreadCount": 0,
    "lastMessageAt": null,
    "createdAt": "2026-05-31T10:00:00"
  }
}
```

#### STOMP — ChatController
`src/main/java/app/store/controller/ChatController.java`

| Destination | Mô tả |
|-------------|-------|
| `/app/chat.send` | Gửi tin nhắn mới (cả khách và admin đều dùng endpoint này) |

**Payload gửi đến `/app/chat.send`:**
```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Cho hỏi giá sản phẩm X?",
  "senderType": "CUSTOMER"
}
```

**Broadcast nhận được tại `/topic/conversation/{id}`:**
```json
{
  "id": "msg-uuid",
  "conversationId": "conv-uuid",
  "senderType": "CUSTOMER",
  "content": "Cho hỏi giá sản phẩm X?",
  "createdAt": "2026-05-31T10:05:00"
}
```

### 3.5 Services

#### ConversationService
`src/main/java/app/store/service/ConversationService.java`

| Method | Mô tả |
|--------|-------|
| `create(request)` | Tạo conversation mới channel=WEB |
| `getAll(page, size)` | Phân trang, sắp xếp theo `lastMessageAt` DESC |
| `getById(id)` | Lấy 1 conversation theo id |
| `markAsRead(id)` | Reset `unreadCount = 0` |
| `close(id)` | Đặt `status = CLOSED` |
| `incrementUnread(id, lastMsg)` | Tăng `unreadCount + 1`, cập nhật `lastMessageAt` |
| `updateLastMessage(id, lastMsg)` | Cập nhật `lastMessageAt` khi admin gửi |
| `getTotalUnread()` | Tổng unread của tất cả conversation OPEN |

#### ChatMessageService
`src/main/java/app/store/service/ChatMessageService.java`

| Method | Mô tả |
|--------|-------|
| `send(request)` | Lưu message → broadcast STOMP → nếu CUSTOMER thì tăng unread và notify admin |
| `getMessages(conversationId, page, size)` | Lịch sử tin nhắn phân trang |

### 3.6 Security

Cập nhật `SecurityConfig.java`:

```java
// Public endpoints — thêm /ws/** và POST /conversations
private static final String[] PUBLIC_ENDPOINTS = {
    ...,
    "/ws/**"  // WebSocket handshake
};

// POST public
.requestMatchers(HttpMethod.POST, ..., "/conversations").permitAll()

// GET public — khách cần đọc lại lịch sử khi reload
.requestMatchers(HttpMethod.GET, ..., "/conversations/*/messages").permitAll()
```

Các endpoint `GET /conversations`, `PUT /conversations/**`, `GET /conversations/unread-count` yêu cầu role `ADMIN` (kiểm soát bằng `@PreAuthorize("hasRole('ADMIN')")`).

---

## 4. Frontend

### 4.1 Packages

Thêm vào `store-fe/package.json`:

```json
"@stomp/stompjs": "^7.3.0",
"sockjs-client": "^1.6.1",
"@types/sockjs-client": "^1.5.4"
```

Cài đặt: `npm install @stomp/stompjs sockjs-client @types/sockjs-client`

### 4.2 Cấu trúc thư mục

```
store-fe/src/
├── components/
│   └── ChatWidget.tsx              # Widget chat cho khách hàng (nút + cửa sổ)
│
├── pages/
│   ├── Index.tsx                   # Nhúng <ChatWidget /> vào trang chủ
│   └── AdminPage.tsx               # Thêm tab "Tin nhắn" + route inbox
│
├── routes/
│   └── index.tsx                   # Thêm route /admin/inbox
│
└── features/
    └── inbox/
        ├── api/
        │   └── InboxApi.ts         # Axios calls đến /conversations API
        ├── hooks/
        │   └── useStompChat.ts     # STOMP connection + subscribe/publish
        ├── components/
        │   ├── InboxPage.tsx       # Layout 2 cột: danh sách + chat
        │   ├── ConversationList.tsx # Cột trái: danh sách + search
        │   ├── ConversationItem.tsx # 1 hội thoại trong danh sách
        │   ├── ChatWindow.tsx      # Cột phải: lịch sử + nhập tin
        │   └── MessageBubble.tsx   # 1 tin nhắn (admin/khách)
        └── types/
            └── index.ts            # TypeScript interfaces
```

### 4.3 STOMP Hook

File: `src/features/inbox/hooks/useStompChat.ts`

```typescript
const { send } = useStompChat({
  conversationId: "conv-uuid",  // subscribe topic của conversation này
  onMessage: (msg) => { /* nhận tin nhắn mới */ },
  onAdminNewMessage: (msg) => { /* admin nhận notify có conversation mới */ },
  enabled: true,
});

// Gửi tin nhắn
send({ conversationId, content: "Xin chào!", senderType: "CUSTOMER" });
```

- Tự động kết nối lại sau 5 giây nếu mất kết nối (`reconnectDelay: 5000`)
- Dùng SockJS làm transport layer (fallback cho HTTP long-polling)

### 4.4 ChatWidget (khách hàng)

File: `src/components/ChatWidget.tsx` — nhúng vào `pages/Index.tsx`

**Luồng sử dụng:**

```
Bấm nút chat
    ↓
Lần đầu: nhập tên → POST /conversations → nhận conversationId
    ↓ (những lần sau: đọc từ localStorage)
Kết nối STOMP, subscribe /topic/conversation/{id}
    ↓
Chat bình thường — gửi/nhận real-time
```

**Lưu trữ session:**
- `localStorage["chat_conversation_id"]` — ID hội thoại, giữ khi reload trang
- `localStorage["chat_guest_name"]` — tên khách

### 4.5 Inbox Admin

Truy cập tại: `/admin/inbox` (mục "Tin nhắn" trong sidebar)

**Cột trái — ConversationList:**
- Danh sách hội thoại, sắp xếp theo tin nhắn mới nhất
- Ô tìm kiếm theo tên khách
- Chấm xanh + badge số unread
- Nhãn trạng thái "Đang mở" / "Đã đóng"

**Cột phải — ChatWindow:**
- Tải lịch sử tin nhắn khi chọn hội thoại
- Tự động cuộn xuống tin mới nhất
- Tự động đánh dấu đã đọc khi mở hội thoại
- Ô nhập + nút Gửi (Enter để gửi)
- Nút "Đóng hội thoại" — chuyển status sang CLOSED
- Hội thoại CLOSED ẩn ô nhập tin

**Real-time updates:**
- Admin subscribe `/topic/admin/new-message` → khi có tin từ khách, danh sách tự refresh
- Admin đang mở hội thoại subscribe `/topic/conversation/{id}` → tin xuất hiện ngay không cần reload

---

## 5. Luồng dữ liệu

### Luồng khách gửi tin

```
1. Khách nhập tin nhắn → nhấn Enter
2. ChatWidget.tsx gọi send({ conversationId, content, senderType: "CUSTOMER" })
3. STOMP publish đến /app/chat.send
4. ChatController.sendMessage() nhận
5. ChatMessageService.send():
   a. Lưu ChatMessage vào MySQL
   b. broadcast response đến /topic/conversation/{id}
   c. conversationService.incrementUnread() → unreadCount++, lastMessageAt = now
   d. broadcast đến /topic/admin/new-message
6. ChatWidget.tsx nhận lại message qua subscription → hiển thị
7. Admin InboxPage nhận /topic/admin/new-message → gọi loadConversations() → cập nhật list
8. Admin đang trong hội thoại đó → ChatWindow nhận /topic/conversation/{id} → hiển thị ngay
```

### Luồng admin trả lời

```
1. Admin nhập tin nhắn → nhấn Gửi
2. ChatWindow.tsx gọi send({ conversationId, content, senderType: "ADMIN" })
3. STOMP publish đến /app/chat.send
4. ChatController.sendMessage() nhận
5. ChatMessageService.send():
   a. Lưu ChatMessage vào MySQL
   b. broadcast response đến /topic/conversation/{id}
   c. conversationService.updateLastMessage() → lastMessageAt = now
6. ChatWindow.tsx nhận lại → hiển thị (với style bong bóng phải)
7. ChatWidget khách subscribe → nhận tin → hiển thị ngay
```

### Luồng khách mở lại trang (reload)

```
1. ChatWidget đọc conversationId từ localStorage
2. Gọi GET /conversations/{id}/messages → lấy lịch sử
3. Kết nối STOMP mới, subscribe /topic/conversation/{id}
4. Chat tiếp tục bình thường
```

---

## 6. Hướng dẫn chạy & kiểm tra

### Khởi động

```bash
# Terminal 1 — Backend
cd auto_accessories_store-be
mvn spring-boot:run
# Chạy tại http://localhost:8080/api/v1

# Terminal 2 — Frontend
cd store-fe
npm run dev
# Chạy tại http://localhost:3000
```

Khi backend khởi động lần đầu, JPA tự tạo 2 bảng mới: `conversations` và `chat_messages`.

### Kiểm tra end-to-end

1. Mở tab khách: `http://localhost:3000`
2. Nhấn nút chat góc dưới phải → nhập tên → bấm "Bắt đầu chat"
3. Gửi một tin nhắn thử
4. Mở tab admin: `http://localhost:3000/admin/inbox` (đăng nhập tài khoản admin)
5. Thấy hội thoại mới xuất hiện trong danh sách bên trái
6. Nhấn vào hội thoại → thấy tin nhắn của khách
7. Admin gõ và gửi → xuất hiện ngay ở tab khách
8. Kiểm tra badge unread trong sidebar Admin tại mục "Tin nhắn"

### Kiểm tra WebSocket

Dùng browser DevTools → Network → tab WS:
- Kết nối `ws://localhost:8080/api/v1/ws/...` (SockJS)
- Thấy các frame STOMP CONNECT, SUBSCRIBE, SEND, MESSAGE

### Kiểm tra DB

```sql
SELECT * FROM conversations ORDER BY created_at DESC LIMIT 10;
SELECT * FROM chat_messages WHERE conversation_id = 'your-id' ORDER BY created_at;
```

---

## 7. Mở rộng Phase 2 — Zalo & Messenger

### Chiến lược
- **Chỉ nhận** tin nhắn từ Zalo OA và Messenger về admin dashboard (webhook miễn phí)
- **Gửi đi** admin tự dùng app Zalo/Facebook trên điện thoại (tránh phí API)

### Zalo OA

**Yêu cầu:**
- Tài khoản Zalo Official Account
- Vào [Zalo for Developers](https://developers.zalo.me) → tạo app → lấy OA Access Token
- Cấu hình webhook URL: `https://yourdomain.com/api/v1/webhook/zalo`

**Cần thêm vào backend:**
```java
// controller/ZaloWebhookController.java
@PostMapping("/webhook/zalo")
public ResponseEntity<String> receiveZalo(@RequestBody ZaloWebhookPayload payload) {
    // parse payload → tạo/lấy Conversation (channel=ZALO, externalId=sender.id)
    // lưu ChatMessage, broadcast STOMP
    return ResponseEntity.ok("OK");
}
```

**application.properties:**
```properties
zalo.oa.secret-key=YOUR_SECRET_KEY
```

### Facebook Messenger

**Yêu cầu:**
- Facebook App với permission `pages_messaging`
- Page Access Token
- Webhook URL: `https://yourdomain.com/api/v1/webhook/messenger`
- Webhook Verify Token (chuỗi tự đặt)

**Cần thêm vào backend:**
```java
// controller/MessengerWebhookController.java

@GetMapping("/webhook/messenger")  // Facebook gọi để verify
public String verify(@RequestParam("hub.verify_token") String token,
                     @RequestParam("hub.challenge") String challenge) {
    if (verifyToken.equals(token)) return challenge;
    return "Invalid token";
}

@PostMapping("/webhook/messenger")  // Nhận tin nhắn
public ResponseEntity<String> receive(@RequestBody MessengerPayload payload) {
    // parse → tạo Conversation (channel=MESSENGER) → lưu ChatMessage → broadcast
    return ResponseEntity.ok("EVENT_RECEIVED");
}
```

**application.properties:**
```properties
facebook.verify-token=YOUR_VERIFY_TOKEN
facebook.app.secret=YOUR_APP_SECRET
```

### Thay đổi frontend (Phase 2)

Khi `conversation.channel` là `ZALO` hoặc `MESSENGER`, `ChatWindow.tsx` thay ô nhập tin bằng banner:

```
⚠️ Kênh Zalo — Vui lòng trả lời trực tiếp trên app Zalo OA (điện thoại)
```

Không cần thay đổi `ConversationList.tsx` hay `InboxPage.tsx` — chỉ cần thêm `ChannelBadge` hiển thị màu sắc theo kênh.

---

*Tài liệu cập nhật lần cuối: 2026-05-31*
