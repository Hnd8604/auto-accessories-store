# Giỏ Hàng — Tài liệu Kỹ thuật

Tài liệu này mô tả tính năng **Giỏ hàng** của hệ thống AutoLux Store. Hệ thống dùng **2 lớp giỏ hàng**:

| Lớp | Đối tượng | Nơi lưu | Vòng đời |
|-----|-----------|---------|----------|
| **Session cart** (giỏ tạm / cache) | Khách chưa đăng nhập (guest) | `HttpSession` phía server (RAM) | Tự hết hạn sau ~30 phút không hoạt động |
| **DB cart** (giỏ bền vững) | Người dùng đã đăng nhập | MySQL (`carts`, `cart_items`) | Tồn tại theo tài khoản |

Khi khách đăng nhập, **giỏ tạm được merge** vào giỏ DB rồi xoá đi.

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc](#2-kiến-trúc)
3. [Session Cart (giỏ tạm / cache)](#3-session-cart-giỏ-tạm--cache)
   - [Cơ chế lưu trữ](#31-cơ-chế-lưu-trữ)
   - [Service](#32-service)
   - [API Endpoints](#33-api-endpoints)
4. [DB Cart (giỏ bền vững)](#4-db-cart-giỏ-bền-vững)
   - [Entities & Database](#41-entities--database)
   - [Service](#42-service)
   - [API Endpoints](#43-api-endpoints)
5. [Đồng bộ giỏ khi đăng nhập](#5-đồng-bộ-giỏ-khi-đăng-nhập)
6. [Luồng dữ liệu](#6-luồng-dữ-liệu)
7. [Hướng dẫn kiểm tra](#7-hướng-dẫn-kiểm-tra)
8. [Giới hạn & hướng mở rộng](#8-giới-hạn--hướng-mở-rộng)

---

## 1. Tổng quan

### Vấn đề
Khách thường bỏ sản phẩm vào giỏ **trước khi đăng nhập**. Nếu bắt buộc đăng nhập mới được thêm giỏ → trải nghiệm kém, dễ mất đơn. Nhưng nếu chỉ lưu giỏ ở trình duyệt thì không đồng bộ giữa các thiết bị.

### Giải pháp
- **Khách chưa đăng nhập:** giỏ lưu tạm trong `HttpSession` phía server (lớp "cache" nhẹ, hết hạn sau 30 phút). Không đụng tới database.
- **Người dùng đã đăng nhập:** giỏ lưu bền vững trong MySQL, gắn 1–1 với tài khoản.
- **Lúc đăng nhập:** gộp giỏ tạm vào giỏ DB để không mất sản phẩm đã chọn.

### Kết quả
- Khách thêm giỏ ngay không cần đăng nhập.
- Đăng nhập xong, sản phẩm đã chọn vẫn còn trong giỏ chính thức.
- Giỏ DB đồng bộ trên mọi thiết bị của cùng tài khoản.

---

## 2. Kiến trúc

```
                          ┌─────────────────────────────────────────┐
Khách CHƯA đăng nhập      │              Backend                     │
──────────────────        │                                          │
  POST /session-carts/add ─┼──► SessionCartController                │
  GET  /session-carts      │      └── SessionCartService             │
  ...                      │            └── HttpSession["CART"]       │   ← RAM, TTL 30'
                          │                Map<productId, quantity>   │
                          │                                          │
  ── Đăng nhập ──────────┼──► AuthenticationService.login()         │
                          │      └── CartSyncService.syncSessionCart()│
                          │            ├── đọc HttpSession["CART"]     │
                          │            ├── CartService.addItemToCart() ┼──► MySQL
                          │            └── session.removeAttribute()   │
                          │                                          │
Người dùng ĐÃ đăng nhập   │                                          │
──────────────────        │                                          │
  GET  /carts/my-cart  ───┼──► CartController                        │
  POST /carts/items       │      └── CartService                     │
  ...                      │            └── CartRepository ───────────┼──► MySQL (carts, cart_items)
                          └─────────────────────────────────────────┘
```

**Công nghệ:** Spring Session (mặc định `HttpSession` của Servlet container — lưu RAM) cho giỏ tạm; Spring Data JPA + MySQL cho giỏ bền vững.

> ℹ️ Giỏ tạm hiện dùng `HttpSession` mặc định (lưu trong RAM của server). Project đã có Redis (dùng cho OTP/reset password) nên có thể chuyển session sang Redis bằng `spring-session-data-redis` nếu cần chạy nhiều instance — xem [mục 8](#8-giới-hạn--hướng-mở-rộng).

---

## 3. Session Cart (giỏ tạm / cache)

### 3.1 Cơ chế lưu trữ

Giỏ tạm là một `Map<Long, Integer>` lưu trong session với key `"CART"`:
- **key** của map = `productId`
- **value** của map = `quantity`

```java
session.getAttribute("CART");  // Map<productId, quantity>
```

`HttpSession` mặc định tồn tại **30 phút** kể từ lần truy cập cuối (không cấu hình thêm). Hết hạn → giỏ tạm tự biến mất. Đây là lý do gọi nó là lớp **cache**: nhẹ, tạm thời, không ghi DB.

### 3.2 Service

File: `src/main/java/app/store/service/SessionCartService.java`

| Method | Mô tả |
|--------|-------|
| `getSessionCart()` | Lấy map giỏ từ session; nếu chưa có thì khởi tạo map rỗng và lưu lại |
| `addToCart(productId, quantity)` | Cộng dồn số lượng sản phẩm vào giỏ tạm |
| `removeFromCart(productId)` | Xoá 1 sản phẩm khỏi giỏ tạm |
| `clearCart()` | Xoá toàn bộ giỏ tạm (`removeAttribute("CART")`) |

### 3.3 API Endpoints

File: `src/main/java/app/store/controller/SessionCartController.java` — base path `/session-carts` (**public**, cho guest)

| Method | URL | Mô tả |
|--------|-----|-------|
| `POST` | `/session-carts/add?productId={id}&qty={n}` | Thêm sản phẩm vào giỏ tạm (mặc định `qty=1`). Kiểm tra sản phẩm tồn tại trước |
| `GET` | `/session-carts` | Xem giỏ tạm — trả `Map<productId, quantity>` |
| `DELETE` | `/session-carts/remove/{productId}` | Xoá 1 sản phẩm khỏi giỏ tạm |
| `DELETE` | `/session-carts/clear` | Xoá sạch giỏ tạm |

**Response mẫu `GET /session-carts`:**
```json
{
  "12": 2,
  "45": 1
}
```
(sản phẩm id=12 số lượng 2, id=45 số lượng 1)

> `/session-carts/**` nằm trong danh sách public endpoint của `SecurityConfig.java`.

---

## 4. DB Cart (giỏ bền vững)

### 4.1 Entities & Database

#### Bảng `cart`

File: `src/main/java/app/store/entity/Cart.java`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT PK | Tự tăng (`BaseEntityLong`) |
| `user_id` | FK | `@OneToOne` đến `users` — mỗi user 1 giỏ |
| *(quan hệ)* | — | `@OneToMany` `cartItems` (cascade ALL, orphanRemoval) |

#### Bảng `cart_item`

File: `src/main/java/app/store/entity/CartItem.java`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT PK | Tự tăng |
| `cart_id` | FK | `@ManyToOne` đến `cart` |
| `product_id` | FK | `@ManyToOne` đến `product` |
| `quantity` | INT | Số lượng |

> Giỏ DB được **tạo sẵn khi đăng ký tài khoản** (xem `UserService`, `AuthenticationService`, `GoogleAuthService`, `ApplicationInitConfig` — đều `new Cart()` gắn vào user).

### 4.2 Service

File: `src/main/java/app/store/service/CartService.java`

| Method | Auth | Mô tả |
|--------|------|-------|
| `getMyCart()` | User | Lấy giỏ của user hiện tại (theo username trong token) |
| `getCartById(cartId)` | `CART_GET_BY_ID` | Lấy giỏ theo id |
| `addItemToCart(request)` | — | Thêm sản phẩm: nếu đã có thì cộng dồn, chưa có thì tạo `CartItem`. **Kiểm tra tồn kho** (`quantity <= stockQuantity`) |
| `removeItemFromCart(cartId, itemId)` | `CART_REMOVE_ITEM` | Xoá item, kiểm tra item thuộc đúng giỏ |
| `updateItemInCart(itemId, request)` | `CART_UPDATE_ITEM` | Cập nhật số lượng item |

### 4.3 API Endpoints

File: `src/main/java/app/store/controller/CartController.java` — base path `/carts` (yêu cầu đăng nhập)

| Method | URL | Mô tả |
|--------|-----|-------|
| `GET` | `/carts/my-cart` | Lấy giỏ của user hiện tại |
| `GET` | `/carts/{cartId}` | Lấy giỏ theo id (owner/admin) |
| `POST` | `/carts/items` | Thêm sản phẩm vào giỏ (`CartItemRequest`: cartId, productId, quantity) |
| `DELETE` | `/carts/{cartId}/items/{itemId}` | Xoá sản phẩm khỏi giỏ |
| `PUT` | `/carts/items/{itemId}` | Cập nhật số lượng (`CartItemUpdateRequest`: quantity) |

**Response mẫu `GET /carts/my-cart`:**
```json
{
  "code": 1000,
  "result": {
    "id": 7,
    "cartItems": [
      { "id": 21, "productId": 12, "productName": "Camera hành trình X", "quantity": 2, "price": 1500000 }
    ]
  }
}
```

---

## 5. Đồng bộ giỏ khi đăng nhập

File: `src/main/java/app/store/service/CartSyncService.java`

Khi user đăng nhập, `AuthenticationService.login()` gọi `cartSyncService.syncSessionCart(user, session)`:

```
1. Đọc giỏ tạm từ session: Map<productId, quantity>
2. Nếu rỗng → return (không làm gì)
3. Lấy giỏ DB của user (cartRepository.findByUserId)
4. Duyệt từng (productId, quantity) trong giỏ tạm:
      CartService.addItemToCart(cartId, productId, quantity)   // cộng dồn vào giỏ DB
5. Xoá giỏ tạm: session.removeAttribute("CART")               // tránh sync lại
```

Kết quả: mọi sản phẩm khách chọn lúc chưa đăng nhập đều chuyển sang giỏ chính thức.

---

## 6. Luồng dữ liệu

### Luồng khách (guest) thêm giỏ rồi đăng nhập

```
1. Guest mở web, bấm "Thêm vào giỏ" sản phẩm #12
2. FE → POST /session-carts/add?productId=12&qty=1
3. SessionCartService.addToCart() → HttpSession["CART"] = { 12: 1 }
4. Guest thêm tiếp #45 → HttpSession["CART"] = { 12: 1, 45: 1 }
5. Guest đăng nhập → AuthenticationService.login()
6. CartSyncService.syncSessionCart():
   a. đọc { 12: 1, 45: 1 }
   b. addItemToCart vào giỏ DB của user
   c. removeAttribute("CART")
7. FE gọi GET /carts/my-cart → thấy đủ #12, #45 trong giỏ chính thức
```

### Luồng user đã đăng nhập

```
1. FE → POST /carts/items { cartId, productId, quantity }
2. CartService.addItemToCart():
   - sản phẩm đã có trong giỏ → cộng dồn quantity
   - chưa có → tạo CartItem mới
   - kiểm tra tồn kho (stockQuantity)
3. Lưu MySQL → trả CartItemResponse
```

---

## 7. Hướng dẫn kiểm tra

### Khởi động
```bash
cd auto_accessories_store-be
mvn spring-boot:run        # http://localhost:8080/api/v1
```

### Test giỏ tạm (guest)
```bash
# Thêm sản phẩm (giữ cookie session bằng -c/-b)
curl -c cookies.txt -X POST "http://localhost:8080/api/v1/session-carts/add?productId=12&qty=2"

# Xem giỏ tạm (dùng lại cookie)
curl -b cookies.txt "http://localhost:8080/api/v1/session-carts"
# → {"12":2}
```

> Phải gửi kèm cookie `JSESSIONID` (`-b cookies.txt`) thì server mới nhận đúng session, nếu không mỗi request là một session mới và giỏ luôn rỗng.

### Test đồng bộ
1. Thêm vài sản phẩm vào giỏ tạm khi chưa đăng nhập (giữ nguyên session/cookie).
2. Đăng nhập bằng chính phiên đó.
3. Gọi `GET /carts/my-cart` → các sản phẩm đã có trong giỏ DB.
4. Gọi lại `GET /session-carts` → giỏ tạm đã rỗng.

### Kiểm tra DB
```sql
SELECT * FROM cart WHERE user_id = 'your-user-id';
SELECT * FROM cart_item WHERE cart_id = 7;
```

---

## 8. Giới hạn & hướng mở rộng

- **Session lưu RAM:** giỏ tạm hiện nằm trong `HttpSession` của một instance. Khi scale nhiều instance sau load balancer (không sticky session) → giỏ tạm có thể "mất". Giải pháp: thêm `spring-session-data-redis` để đẩy session sang **Redis** (project đã cài sẵn Redis cho OTP), giỏ tạm dùng chung giữa các instance.
- **Không kiểm tra tồn kho ở giỏ tạm:** `session-carts/add` chỉ kiểm tra sản phẩm tồn tại, chưa kiểm tra số lượng kho (giỏ DB thì có).
- **Merge cộng dồn:** khi sync, số lượng được cộng dồn vào giỏ DB — nếu sản phẩm đã có sẵn trong cả 2 giỏ, tổng có thể vượt mong muốn của khách.
- **Giỏ tạm hết hạn 30 phút:** khách để lâu sẽ mất giỏ tạm; có thể tăng `server.servlet.session.timeout` nếu cần.

---

*Tài liệu cập nhật lần cuối: 2026-06-05*
