# Thông Báo (Kafka) — Tài liệu Kỹ thuật

Tài liệu này mô tả tính năng **Thông báo** của hệ thống AutoLux Store. Khi có sự kiện đơn hàng (đặt hàng mới, đổi trạng thái, huỷ), hệ thống phát **event qua Apache Kafka**; một consumer lắng nghe và xử lý **bất đồng bộ**: gửi email + lưu thông báo vào DB + đẩy real-time tới trình duyệt qua **SSE**.

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc](#2-kiến-trúc)
3. [Vì sao dùng Kafka](#3-vì-sao-dùng-kafka)
4. [Backend](#4-backend)
   - [Cấu hình Kafka](#41-cấu-hình-kafka)
   - [Topics & Events](#42-topics--events)
   - [Producer](#43-producer)
   - [Consumer](#44-consumer)
   - [Notification & DB](#45-notification--db)
   - [SSE real-time](#46-sse-real-time)
   - [API Endpoints](#47-api-endpoints)
5. [Luồng dữ liệu](#5-luồng-dữ-liệu)
6. [Hướng dẫn kiểm tra](#6-hướng-dẫn-kiểm-tra)
7. [Giới hạn & hướng mở rộng](#7-giới-hạn--hướng-mở-rộng)

---

## 1. Tổng quan

### Vấn đề
Khi khách đặt hàng hoặc đơn đổi trạng thái, cần đồng thời: gửi email, lưu thông báo trong app, và hiện chuông thông báo real-time. Nếu làm **đồng bộ ngay trong luồng đặt hàng**, API đặt hàng sẽ chậm (chờ gửi mail SMTP) và dễ lỗi dây chuyền (mail lỗi → đặt hàng lỗi).

### Giải pháp
Tách phần thông báo ra **xử lý bất đồng bộ qua Kafka**:
- `OrderService` chỉ **phát event** rồi trả response ngay → API nhanh.
- `OrderNotificationConsumer` lắng nghe event, xử lý gửi mail + lưu DB + đẩy SSE ở luồng riêng.

### Kết quả
- API đặt hàng phản hồi nhanh, không chờ gửi mail.
- Lỗi gửi mail không làm hỏng việc đặt hàng.
- Khách nhận thông báo real-time (chuông) + email + lịch sử thông báo trong app.

---

## 2. Kiến trúc

```
   OrderService (đặt hàng / đổi trạng thái)
        │  build event
        ▼
   OrderEventProducer
        │  kafkaTemplate.send(topic, orderId, event)
        ▼
┌──────────────────────── Apache Kafka ─────────────────────────┐
│  topic: order.created            (3 partitions)               │
│  topic: order.status.changed     (3 partitions)               │
└───────────────────────────────────────────────────────────────┘
        │  @KafkaListener (group: store-notification-group)
        ▼
   OrderNotificationConsumer
        ├── MailService.sendOrder...Email()        → Gmail SMTP
        └── NotificationService.createNotification()
                ├── lưu Notification vào MySQL
                └── SseEmitterService.sendToUser()  ──► FE (EventSource /notifications/stream)
```

**Công nghệ:** Spring Kafka (producer + `@KafkaListener` consumer) + JSON serialize event + Spring MVC SSE (`SseEmitter`) đẩy real-time tới FE.

---

## 3. Vì sao dùng Kafka

| Tiêu chí | Lợi ích |
|----------|---------|
| **Bất đồng bộ** | Đặt hàng không phải chờ gửi mail/SSE — phản hồi nhanh |
| **Tách rời (decoupling)** | `OrderService` không biết gì về mail/SSE; chỉ phát event |
| **Bền bỉ (durability)** | Event nằm trong Kafka log; consumer chết rồi bật lại vẫn đọc tiếp (`auto-offset-reset: earliest`) |
| **Mở rộng** | Thêm consumer mới (SMS, Zalo, thống kê...) chỉ cần subscribe topic, không sửa producer |
| **Phân phối theo key** | Dùng `orderId` làm key → các event cùng đơn vào cùng partition, giữ đúng thứ tự |

---

## 4. Backend

### 4.1 Cấu hình Kafka

File `application.yaml` (mẫu tại `application-example.yaml`):

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9094
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
    consumer:
      group-id: store-notification-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    listener:
      missing-topics-fatal: false

app:
  kafka:
    topics:
      order-created: order.created
      order-status-changed: order.status.changed
```

> Producer serialize event thành **JSON**; consumer nhận **String** rồi tự `objectMapper.readValue(...)` về object (tránh phụ thuộc type-header giữa 2 service).

File khai báo topic: `src/main/java/app/store/config/KafkaConfig.java` — tạo sẵn 2 topic, mỗi topic **3 partitions, 1 replica**.

### 4.2 Topics & Events

| Topic | Phát khi | Event DTO |
|-------|----------|-----------|
| `order.created` | Đặt hàng thành công | `OrderCreatedEvent` |
| `order.status.changed` | Admin đổi trạng thái / user huỷ đơn | `OrderStatusChangedEvent` |

#### `OrderCreatedEvent` (`dto/event/OrderCreatedEvent.java`)
| Trường | Mô tả |
|--------|-------|
| `orderId`, `orderCode` | Định danh đơn |
| `userId`, `userEmail`, `recipientName` | Người nhận thông báo |
| `totalPrice`, `paymentMethod`, `createdAt` | Thông tin đơn |

#### `OrderStatusChangedEvent` (`dto/event/OrderStatusChangedEvent.java`)
| Trường | Mô tả |
|--------|-------|
| `orderId`, `orderCode` | Định danh đơn |
| `userId`, `userEmail`, `recipientName` | Người nhận |
| `oldStatus`, `newStatus`, `changedAt` | Thay đổi trạng thái |

### 4.3 Producer

File: `src/main/java/app/store/service/OrderEventProducer.java`

```java
kafkaTemplate.send(orderCreatedTopic, event.getOrderId(), event);
//                  ↑ topic           ↑ key (orderId)     ↑ value (event)
```

| Method | Mô tả |
|--------|-------|
| `publishOrderCreated(event)` | Gửi lên topic `order.created` |
| `publishOrderStatusChanged(event)` | Gửi lên topic `order.status.changed` |

**Nơi gọi** (`OrderService.java`):
- `createOrder()` → `publishOrderCreated(...)` sau khi lưu đơn.
- `updateOrderByAdmin()` → phát status-changed **chỉ khi** trạng thái thực sự đổi.
- `cancelOrder()` → phát status-changed với `newStatus = CANCELED`.

> Việc phát event được bọc `try/catch` và **chỉ log lỗi** — Kafka lỗi cũng không làm hỏng giao dịch đặt hàng.

### 4.4 Consumer

File: `src/main/java/app/store/service/OrderNotificationConsumer.java`

```java
@KafkaListener(topics = "${app.kafka.topics.order-created}",
               groupId = "${spring.kafka.consumer.group-id}")
public void handleOrderCreated(String payload) { ... }
```

| Listener | Xử lý |
|----------|-------|
| `handleOrderCreated` | (1) `MailService.sendOrderCreatedEmail` (2) `NotificationService.createNotification(type=ORDER_CREATED)` |
| `handleOrderStatusChanged` | (1) `MailService.sendOrderStatusChangedEmail` (2) tạo notification: `ORDER_CANCELED` nếu `newStatus=CANCELED`, ngược lại `ORDER_STATUS_CHANGED` |

Consumer parse JSON → object bằng `ObjectMapper`. Nếu xử lý lỗi → ném `RuntimeException` để Kafka có thể retry theo cơ chế mặc định.

### 4.5 Notification & DB

File entity: `src/main/java/app/store/entity/Notification.java` — bảng `notifications`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | VARCHAR(36) PK | UUID (`BaseEntityUUID`) |
| `user_id` | FK NOT NULL | Người nhận |
| `title` | VARCHAR NOT NULL | Tiêu đề |
| `message` | VARCHAR(500) NOT NULL | Nội dung |
| `type` | VARCHAR NOT NULL | `NotificationType` |
| `reference_id` | VARCHAR | ID đối tượng liên quan (orderId...) |
| `is_read` | BOOLEAN | Mặc định `false` |

Index: `idx_notification_user_read(user_id, is_read)`, `idx_notification_user_created(user_id, createdAt)`.

**`NotificationType`** (`enums/NotificationType.java`): `ORDER_CREATED`, `ORDER_STATUS_CHANGED`, `ORDER_CANCELED`, `PAYMENT_RECEIVED`, `SYSTEM`.

File service: `src/main/java/app/store/service/NotificationService.java`

| Method | Mô tả |
|--------|-------|
| `createNotification(userId, title, message, type, referenceId)` | Lưu DB + đẩy SSE tới user |
| `getMyNotifications(pageable)` | Danh sách thông báo của user, mới nhất trước |
| `countUnread()` | Đếm thông báo chưa đọc |
| `markAsRead(id)` / `markAllAsRead()` | Đánh dấu đã đọc |

### 4.6 SSE real-time

File: `src/main/java/app/store/service/SseEmitterService.java`

- Quản lý emitter theo `Map<userId, List<SseEmitter>>` — **một user mở nhiều tab/thiết bị** → nhiều emitter.
- `createEmitter(userId)`: tạo `SseEmitter` (timeout **30 phút**), gửi ngay event `connected`.
- `sendToUser(userId, data)`: đẩy event tên `notification` tới **tất cả** kết nối của user.
- Tự dọn emitter khi `onCompletion` / `onTimeout` / `onError`.

> SSE chạy **trong cùng process** (lưu emitter ở RAM). Consumer Kafka và endpoint SSE phải cùng instance thì mới push được — xem [mục 7](#7-giới-hạn--hướng-mở-rộng).

### 4.7 API Endpoints

File: `src/main/java/app/store/controller/NotificationController.java` — base path `/notifications`

| Method | URL | Mô tả |
|--------|-----|-------|
| `GET` | `/notifications/stream` | Mở kết nối **SSE** (`text/event-stream`) nhận thông báo real-time |
| `GET` | `/notifications?page=&size=` | Danh sách thông báo (phân trang, mới nhất trước) |
| `GET` | `/notifications/unread-count` | Số thông báo chưa đọc |
| `PUT` | `/notifications/{id}/read` | Đánh dấu 1 thông báo đã đọc |
| `PUT` | `/notifications/read-all` | Đánh dấu tất cả đã đọc |

**Kết nối SSE từ FE:**
```javascript
const es = new EventSource('/api/v1/notifications/stream', { withCredentials: true });
es.addEventListener('connected', e => console.log('SSE ready'));
es.addEventListener('notification', e => {
  const noti = JSON.parse(e.data);   // NotificationResponse
  // hiện toast + tăng badge chuông
});
```

---

## 5. Luồng dữ liệu

### Luồng đặt hàng → thông báo

```
1. User đặt hàng → OrderService.createOrder() lưu Order vào MySQL
2. OrderEventProducer.publishOrderCreated(event) → Kafka topic order.created
3. API đặt hàng trả response NGAY (không chờ mail)
        ── (bất đồng bộ) ──
4. OrderNotificationConsumer.handleOrderCreated(payload) nhận event
5. MailService gửi email "Đặt hàng thành công"
6. NotificationService.createNotification():
   a. lưu Notification(type=ORDER_CREATED) vào MySQL
   b. SseEmitterService.sendToUser(userId, response)
7. FE đang mở /notifications/stream → nhận event "notification" → hiện chuông
```

### Luồng đổi trạng thái đơn

```
1. Admin đổi trạng thái → OrderService.updateOrderByAdmin()
2. Nếu oldStatus != newStatus → publishOrderStatusChanged(event) → topic order.status.changed
3. Consumer.handleOrderStatusChanged():
   - gửi email cập nhật trạng thái
   - tạo Notification: ORDER_CANCELED nếu CANCELED, ngược lại ORDER_STATUS_CHANGED
   - đẩy SSE tới user
```

---

## 6. Hướng dẫn kiểm tra

### Khởi động hạ tầng

```bash
# Kafka (ví dụ chạy bằng Docker, broker ở localhost:9094)
docker compose up -d kafka

# Backend
cd auto_accessories_store-be
mvn spring-boot:run        # http://localhost:8080/api/v1
```

Khi khởi động, `KafkaConfig` tự tạo 2 topic `order.created`, `order.status.changed` (nếu broker cho phép auto-create / qua `NewTopic` bean).

### Kiểm tra end-to-end

1. Đăng nhập, mở kết nối SSE: gọi `GET /notifications/stream` (hoặc để FE mở `EventSource`).
2. Đặt một đơn hàng.
3. Quan sát:
   - Log backend: `Published order-created event...` rồi `Processed order-created notification...`
   - Email "Đặt hàng thành công" trong hộp thư.
   - SSE nhận event `notification` → FE hiện chuông.
4. Gọi `GET /notifications` → thấy thông báo vừa tạo; `GET /notifications/unread-count` > 0.

### Kiểm tra topic Kafka

```bash
# Liệt kê topic
kafka-topics.sh --bootstrap-server localhost:9094 --list

# Xem message trong topic
kafka-console-consumer.sh --bootstrap-server localhost:9094 \
  --topic order.created --from-beginning
```

### Kiểm tra DB
```sql
SELECT * FROM notifications WHERE user_id = 'your-user-id' ORDER BY created_at DESC;
```

---

## 7. Giới hạn & hướng mở rộng

- **SSE chỉ hoạt động trên 1 instance:** emitter lưu trong RAM. Khi scale nhiều instance, consumer xử lý ở instance A nhưng user kết nối SSE ở instance B sẽ không nhận được push. Giải pháp: dùng Redis Pub/Sub (project đã có Redis) hoặc topic Kafka "fan-out" để mọi instance đẩy SSE.
- **Chưa có Dead Letter Topic (DLT):** consumer lỗi sẽ retry theo mặc định; nên cấu hình DLT + retry/back-off để không kẹt message lỗi.
- **`PAYMENT_RECEIVED` đã khai báo nhưng chưa dùng:** có thể nối webhook SePay (xem [PAYMENT.md](PAYMENT.md)) phát thông báo "Đã nhận thanh toán" qua cùng cơ chế này.
- **Email đồng bộ trong consumer:** nếu SMTP chậm, consumer xử lý chậm theo. Có thể tách email thành consumer/topic riêng.
- **Thiếu thông báo cho admin:** hiện chỉ thông báo cho user đặt hàng; có thể thêm topic/consumer thông báo cho admin khi có đơn mới.

---

*Tài liệu cập nhật lần cuối: 2026-06-05*
