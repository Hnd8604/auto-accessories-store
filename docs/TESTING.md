# 🧪 Lộ trình viết Unit Test cho dự án Auto Accessories Store

> Tài liệu này đưa bạn đi từ **con số 0** đến việc **phủ test toàn bộ dự án**.
> Đọc tuần tự. Mỗi giai đoạn chỉ bắt đầu khi giai đoạn trước đã "xanh" (test pass).

---

## Mục lục

- [Phần 0 — Kiến thức nền](#phần-0--kiến-thức-nền)
- [Phần 1 — Chuẩn bị môi trường](#phần-1--chuẩn-bị-môi-trường)
- [Phần 2 — Lộ trình BACKEND (7 giai đoạn)](#phần-2--lộ-trình-backend)
- [Phần 3 — Lộ trình FRONTEND (5 giai đoạn)](#phần-3--lộ-trình-frontend)
- [Phần 4 — Cheat-sheet tra nhanh](#phần-4--cheat-sheet-tra-nhanh)
- [Phần 5 — Checklist theo dõi tiến độ](#phần-5--checklist-theo-dõi-tiến-độ)

---

## Phần 0 — Kiến thức nền

### Unit test là gì?

Là đoạn code kiểm tra **tự động** một "đơn vị" nhỏ (thường 1 method/hàm) chạy đúng như mong đợi. Viết một lần, chạy lại trong vài giây mỗi khi sửa code.

### Nguyên tắc vàng: mẫu AAA

Mọi test — backend hay frontend — đều theo 3 bước:

```
Arrange (Chuẩn bị):  tạo dữ liệu đầu vào, dựng bối cảnh, cấu hình mock
Act     (Hành động):  gọi đúng 1 hàm cần test
Assert  (Kiểm chứng): so sánh kết quả thực tế với kết quả mong đợi
```

### Kim tự tháp test — viết cái gì trước?

```
        /\        E2E / Integration   ← ít, chậm, đắt (giai đoạn cuối, tùy chọn)
       /  \
      /____\      Slice test          ← vừa phải (controller, repository)
     /      \
    /________\    Unit test           ← NHIỀU NHẤT, nhanh, rẻ (bắt đầu từ đây)
```

Ta **bắt đầu từ đáy**: unit test thuần — mock hết dependency, không cần DB/Redis/Kafka.

### Cái gì NÊN và KHÔNG NÊN test

| ✅ Nên test | ❌ Không cần test |
|---|---|
| Logic nghiệp vụ trong service | Getter/setter của Lombok |
| Hàm tiện ích (util) | DTO / entity thuần |
| Xử lý lỗi & ngoại lệ | Interface MapStruct (code sinh tự động) |
| Tính toán (giá tiền, tồn kho) | File `@Configuration` |
| Nhánh điều kiện (if/else) | Thư viện bên thứ ba (shadcn/ui) |

---

## Phần 1 — Chuẩn bị môi trường

### Backend — KHÔNG cần cài gì thêm 🎉

`backend/pom.xml` đã có sẵn `spring-boot-starter-test`, bao gồm:
**JUnit 5** (bộ chạy test) + **Mockito** (tạo mock) + **AssertJ** (assert đẹp).

Chạy test:
```bash
cd backend
./mvnw test                          # chạy toàn bộ
./mvnw test -Dtest=SlugUtilTest      # chạy riêng 1 class
```

> ⚠️ Lưu ý: `StoreApplicationTests.java` hiện có `@SpringBootTest` — nó cần Postgres/Redis/Kafka + biến môi trường thật nên sẽ **fail** khi chạy unit test. Ở Giai đoạn 1 ta sẽ tạm `@Disabled` nó để tách khỏi vòng unit test.

### Frontend — cần cài Vitest (làm ở Giai đoạn F1)

Công cụ chuẩn cho Vite + React:
```bash
cd frontend
npm install -D vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event @vitest/coverage-v8
```
- **vitest** — bộ chạy test (giống JUnit của frontend)
- **jsdom** — giả lập trình duyệt trong Node
- **@testing-library/react** — render component & tương tác như người dùng

---

## Phần 2 — Lộ trình BACKEND

> **Quy ước:** file test đặt trong `src/test/java`, giống hệt package của class thật, thêm hậu tố `Test`.
> Ví dụ: `main/.../utils/SlugUtil.java` → `test/.../utils/SlugUtilTest.java`

### 🟢 Giai đoạn B1 — Hàm thuần (KHÔNG mock) — DỄ NHẤT, BẮT ĐẦU TỪ ĐÂY

**Mục tiêu:** làm quen `@Test`, `assertThat`, `@ParameterizedTest`. Không có dependency nào.

| File cần test | Nội dung |
|---|---|
| `utils/SlugUtil.java` | bỏ dấu tiếng Việt, `đ→d`, gom dấu `-`, null/rỗng; `createUniqueSlug` với callback giả |
| `utils/SortUtils.java` | `"name,ASC"`, chỉ field không direction, null → fallback `Sort.by(ASC,"id")`, rác → fallback |

**Ví dụ khung sườn:**
```java
package app.store.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.params.ParameterizedTest;
import org.junit.jupiter.api.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilTest {
    private final SlugUtil slugUtil = new SlugUtil();

    @ParameterizedTest
    @CsvSource({
        "'Lốp xe Ô tô', 'lop-xe-o-to'",
        "'Đèn LED',      'den-led'",
        "'A---B',        'a-b'"
    })
    void toSlug_variousInputs(String input, String expected) {
        assertThat(slugUtil.toSlug(input)).isEqualTo(expected);
    }

    @Test
    void createUniqueSlug_shouldAppendNumber_whenExists() {
        var existsChecker = (java.util.function.Function<String, Boolean>)
            slug -> slug.equals("den-led") || slug.equals("den-led-1");
        assertThat(slugUtil.createUniqueSlug("den-led", existsChecker))
            .isEqualTo("den-led-2");
    }
}
```

**Hoàn thành khi:** `./mvnw test -Dtest=SlugUtilTest,SortUtilsTest` xanh.

---

### 🟢 Giai đoạn B2 — Service đơn giản với Mockito

**Mục tiêu:** học `@Mock`, `@InjectMocks`, `when().thenReturn()`, `verify()`, test cả happy path lẫn lỗi.

Các service dùng `@RequiredArgsConstructor` (Lombok) → `@InjectMocks` tự tiêm mock qua constructor.

| File cần test | Vì sao chọn | Điểm cần kiểm |
|---|---|---|
| `service/ProductService.java` | ít dependency, logic rõ | create (set slug/category), not-found → `AppException`, brand không thuộc category → `BRAND_NOT_IN_CATEGORY` |
| `service/CategoryService.java` | tương tự | CRUD + not-found |
| `service/BrandService.java` | tương tự | CRUD + not-found |

**Khung sườn chuẩn:**
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock BrandRepository brandRepository;
    @Mock ProductMapper productMapper;
    @Mock SlugUtil slugUtil;
    @InjectMocks ProductService productService;   // object thật, được tiêm 5 mock

    @Test
    void getProductById_shouldThrow_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
            .isInstanceOf(AppException.class)
            .extracting(e -> ((AppException) e).getErrorCode())
            .isEqualTo(ErrorCode.PRODUCT_NOT_EXISTED);

        verify(productMapper, never()).toProductResponse(any());
    }
}
```

**3 điều PHẢI nhớ về dự án này:**
1. **`@PreAuthorize` KHÔNG chạy** trong unit test (chỉ hoạt động qua Spring proxy). → không cần giả lập quyền/đăng nhập.
2. `@InjectMocks` tiêm qua constructor nhờ Lombok — hoạt động sẵn.
3. Dùng `ArgumentCaptor<Product>` để "chụp" object truyền vào `save()` và kiểm slug/category đã set đúng.

**Hoàn thành khi:** 3 service trên xanh, mỗi service có ít nhất 1 happy path + 1 test lỗi.

---

### 🟡 Giai đoạn B3 — Service nghiệp vụ phức tạp

**Mục tiêu:** nâng cao — `doThrow` cho void, `BigDecimal`, nhiều nhánh.

| File cần test | Điểm giá trị cao |
|---|---|
| `service/OrderService.java` | tính `totalPrice` nhiều item, trừ tồn kho, xoá cart item khi về 0, `quantity > stock` → `IllegalArgumentException`, **Kafka lỗi vẫn không hỏng đơn**, `cancelOrder` hoàn kho + trạng thái sai → lỗi |
| `service/CartService.java` | thêm/xoá/cập nhật item, gộp cart |
| `service/PostService.java` | tạo/sửa slug, phân trang |

**Kỹ thuật mới:**
```java
// Method trả về void → dùng doThrow (KHÔNG dùng when().thenThrow())
doThrow(new RuntimeException("Kafka down"))
    .when(orderEventProducer).publishOrderCreated(any());

assertThatCode(() -> orderService.createOrderFromCart(request))
    .doesNotThrowAnyException();          // đơn vẫn thành công

// So sánh BigDecimal bằng compareTo, KHÔNG bằng equals
assertThat(order.getTotalPrice()).isEqualByComparingTo("200000");
```

Nếu test hàm dùng `SecurityContextHolder` (vd `getMyOrder`): set context trong test và dọn ở `@AfterEach { SecurityContextHolder.clearContext(); }`.

---

### ✅ Giai đoạn B4 — Các service còn lại — ĐÃ HOÀN THÀNH

Toàn bộ 22 service còn lại đã có test. Tổng cộng **239 test, 0 failure**.

| Nhóm | Test class | Số test | Điểm nhấn đã phủ |
|---|---|---:|---|
| **Cao** | `AuthenticationServiceTest` | 18 | register (trùng user/email/thiếu role), sai mật khẩu → `UNAUTHENTICATED`, refresh bằng accessToken → lỗi, token đã logout → lỗi, đổi mật khẩu 4 nhánh |
| | `ResetPasswordServiceTest` | 12 | OTP hết hạn, sai OTP → tăng `otpAttempt`, quá 5 lần → **xoá phiên**, sai bước, cooldown gửi lại |
| | `OtpServiceTest` | 6 | Redis lưu **hash** chứ không lưu OTP thô, OTP dùng 1 lần |
| | `PaymentServiceTest` | 16 | QR VietQR, webhook SePay: tiền ra bị bỏ qua, thiếu tiền → không PAID, đơn đã PAID → bỏ qua, xác thực API key |
| | `GoogleAuthServiceTest` | 6 | tạo user mới / link googleId theo email / cập nhật avatar, lỗi Google → `GOOGLE_AUTH_FAILED` |
| | `ProfessionalServiceServiceTest` | 13 | parse/serialize `features` JSON, JSON hỏng không làm sập API, chọn ảnh primary |
| **Vừa** | `UserServiceTest` | 12 | chỉ đổi password/roles khi request có gửi |
| | `RoleServiceTest` | 8 | lưu DB **trước** rồi mới sync Redis (`inOrder`) |
| | `PermissionServiceTest` | 5 | CRUD + not-found |
| | `NotificationServiceTest` | 8 | tạo thông báo + push SSE, `markAsRead` của người khác → lỗi |
| | `ProductImageServiceTest` / `ServiceImageServiceTest` | 9 + 8 | không upload Cloudinary khi entity không tồn tại, ảnh không thuộc sản phẩm → lỗi |
| | `PostCategoryServiceTest` | 12 | trùng tên, slug chỉ đổi khi tên đổi, danh mục còn bài viết → không cho xoá |
| | `BannerServiceTest` | 11 | `isActive` mặc định true, chỉ upload ảnh mới khi có file |
| | `ConversationServiceTest` / `ChatMessageServiceTest` | 9 + 3 | tin từ CUSTOMER mới tăng unread, broadcast đúng topic |
| **Thấp** | `SessionCartServiceTest` | 5 | cộng dồn số lượng cùng sản phẩm |
| | `CartSyncServiceTest` | 3 | gộp giỏ session → giỏ DB rồi xoá session |
| | `MailServiceTest` | 4 | subject/nội dung/người nhận đúng |
| | `SseEmitterServiceTest` | 5 | 1 user nhiều tab = nhiều emitter |
| | `OrderEventProducerTest` | 2 | gửi đúng topic, key = `orderId` |
| | `OrderNotificationConsumerTest` | 4 | payload hỏng → **ném lỗi** để Kafka retry |

#### 🆕 6 kỹ thuật mới học được ở B4

**1. `ReflectionTestUtils.setField` — nạp giá trị cho field `@Value`**

Unit test không chạy Spring nên `@Value` luôn `null`. Set tay:
```java
@BeforeEach
void setUp() {
    ReflectionTestUtils.setField(authenticationService, "SIGNER_KEY", "…64 ký tự…");
    ReflectionTestUtils.setField(authenticationService, "ACCESS_DURATION", 3600L);
}
```
> ⚠️ HS512 bắt buộc khoá ≥ 64 ký tự, ngắn hơn sẽ ném `KeyLengthException`.

**2. `@Spy` — object THẬT thay vì mock rỗng**

`ObjectMapper` mà mock thì `readValue()` trả `null`, test mất ý nghĩa. Dùng đồ thật:
```java
@Spy ObjectMapper objectMapper = new ObjectMapper();
@InjectMocks OrderNotificationConsumer consumer;   // @Spy cũng được tiêm vào
```
| | Mock | Spy |
|---|---|---|
| Hành vi mặc định | trả null/0/rỗng | chạy code thật |
| Dùng cho | dependency I/O (DB, mail, Kafka) | tiện ích thuần (ObjectMapper, encoder) |

**3. `mockConstruction` — chặn `new` bên trong service**

`GoogleAuthService` tự `new RestTemplate()` nên không @Mock được. Mockito chặn luôn lệnh `new`:
```java
try (MockedConstruction<RestTemplate> ignored = mockConstruction(RestTemplate.class,
        (mock, context) -> lenient().when(mock.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"google-token\"}")))) {
    googleAuthService.authenticateWithGoogle(request);   // không hề gọi mạng thật
}
```
Chỉ có hiệu lực **trong khối try** → hết khối là `new RestTemplate()` trở lại bình thường.

**4. `lenient()` — tắt cảnh báo "stub thừa"**

`MockitoExtension` mặc định STRICT: stub khai báo mà không dùng → fail. Khi một stub chỉ dùng ở vài nhánh (như 2 instance RestTemplate, mỗi cái chỉ gọi 1 method), bọc `lenient()`.

**5. `inOrder()` — kiểm THỨ TỰ gọi, không chỉ có gọi hay không**

Có những chỗ sai thứ tự là sinh bug dữ liệu:
```java
var order = inOrder(roleRepository, rolePermissionRepository);
order.verify(roleRepository).save(role);                          // ghi DB trước
order.verify(rolePermissionRepository).syncRolePermissionsFromDb("ADMIN"); // rồi mới sync Redis
```

**6. Test có `SecurityContextHolder` — nhớ dọn ở `@AfterEach`**
```java
SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("john", null, List.of()));
…
@AfterEach void tearDown() { SecurityContextHolder.clearContext(); }
```
Không dọn → context rò rỉ sang test sau, gây fail ngẫu nhiên tuỳ thứ tự chạy.

**Mẹo nhỏ khác dùng trong B4**
- Mock lồng nhau cho Redis: mock `RedisTemplate` **và** `ValueOperations`, rồi `when(redis.opsForValue()).thenReturn(valueOps)`.
- Field có khởi tạo sẵn (`PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);`) **không** vào constructor của `@RequiredArgsConstructor` → không mock được → test bằng `BCryptPasswordEncoder` thật.
- Kiểm token thật thay vì so chuỗi: `SignedJWT.parse(token).getJWTClaimsSet().getSubject()`.
- Giả file upload: `new MockMultipartFile("file", "anh.png", "image/png", bytes)`.
- `MimeMessageHelper(message, true, …)` tạo multipart → phải duyệt đệ quy `Multipart` mới lấy được HTML để assert.

> Với service có logic bảo mật (Auth, ResetPassword, Otp): test kỹ nhánh **sai mật khẩu / OTP hết hạn / token không hợp lệ** — đây là nơi bug gây hậu quả nặng nhất.

---

### 🔵 Giai đoạn B5 — (Tùy chọn) Controller slice test

**Mục tiêu:** kiểm tầng HTTP — mapping URL, mã trạng thái, body JSON.

Dùng `@WebMvcTest(ProductController.class)` + `MockMvc` + `@MockitoBean ProductService`.
Ở đây `@PreAuthorize` CÓ chạy → cần `@WithMockUser(authorities = "...")` hoặc tắt security cho test.

```java
@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ProductService productService;

    @Test
    void getProductById_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(new ProductResponse());
        mockMvc.perform(get("/products/id/1"))
               .andExpect(status().isOk());
    }
}
```

Bắt đầu với 2-3 controller quan trọng: `ProductController`, `OrderController`, `AuthenticationController`.

---

### 🔵 Giai đoạn B6 — (Tùy chọn) Repository slice test

**Mục tiêu:** kiểm các query tự viết (`@Query`, `findBySlug`, `existsByIdAndCategoriesId`...).

Dùng `@DataJpaTest` + H2 in-memory (thêm dependency `com.h2database:h2` scope test). Chỉ cần test các method **custom**, không test method có sẵn của `JpaRepository`.

Ứng viên: `ProductRepository.findBySlug`, `BrandRepository.existsByIdAndCategoriesId`, `OrderRepository.getOrderByUserName`.

---

### 🔴 Giai đoạn B7 — (Nâng cao, tùy chọn) Integration test

Dùng **Testcontainers** (Postgres + Redis thật trong Docker) cho vài luồng end-to-end quan trọng nhất (đặt hàng, thanh toán). Nặng, cần Docker — chỉ làm khi các giai đoạn trên đã vững.

---

## Phần 3 — Lộ trình FRONTEND

> Cú pháp đối chiếu: `@Test` → `it(...)`, `assertThat(x).isEqualTo(y)` → `expect(x).toBe(y)`, `@Mock` → `vi.fn()`, `verify(...)` → `expect(...).toHaveBeenCalledWith(...)`.

### 🟢 Giai đoạn F1 — Cài đặt & test hàm thuần

1. Cài Vitest (xem [Phần 1](#frontend--cần-cài-vitest-làm-ở-giai-đoạn-f1)).
2. Tạo `frontend/vitest.config.ts`: alias `@` → `./src`, `environment: 'jsdom'`, `setupFiles: './src/test/setup.ts'`.
3. Tạo `frontend/src/test/setup.ts`: import `@testing-library/jest-dom/vitest`, `cleanup()` trong `afterEach`, stub `window.matchMedia`.
4. Thêm script vào `package.json`: `"test": "vitest"`, `"test:run": "vitest run"`.

Test hàm thuần đầu tiên:

| File cần test | Nội dung |
|---|---|
| `utils/cn.ts` | gộp class, bỏ falsy, `tailwind-merge` xử lý trùng (`px-2 px-4` → `px-4`) |
| `utils/contentRenderer.ts` | markdown → HTML, input rỗng → `''`, **`<script>` bị loại (chống XSS)** |

```typescript
import { describe, it, expect } from "vitest";
import { renderContent } from "./contentRenderer";

describe("renderContent", () => {
  it("loại bỏ script độc hại (chống XSS)", () => {
    const clean = renderContent("Hi <script>alert(1)</script>");
    expect(clean).not.toContain("<script>");
  });
});
```

---

### 🟢 Giai đoạn F2 — Mock module (lớp API)

**Mục tiêu:** học `vi.mock` — test lớp gọi API mà không chạm mạng thật.

Tất cả file trong `features/*/api/` đều theo cùng pattern (gọi `http.request(path, options)`):

| File cần test |
|---|
| `features/cart/api/carts.ts` |
| `features/cart/api/session-carts.ts` |
| `features/products/api/products.ts` |
| `features/products/api/productImages.ts` |

```typescript
import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("@/features/auth/api/auth", () => ({
  http: { request: vi.fn() },
}));
import { CartsApi } from "./carts";
import { http } from "@/features/auth/api/auth";

describe("CartsApi", () => {
  beforeEach(() => vi.clearAllMocks());

  it("removeItem ghép đúng URL", () => {
    CartsApi.removeItem(1, 2);
    expect(http.request).toHaveBeenCalledWith(
      "/carts/1/items/2", { method: "DELETE" }
    );
  });
});
```

---

### 🟡 Giai đoạn F3 — Context & Hooks

**Mục tiêu:** test logic React qua `renderHook` + wrapper.

| File cần test | Điểm kiểm |
|---|---|
| `context/cart-context.tsx` | `itemCount` (guest vs đăng nhập), `addToCart` guest gọi `SessionCartsApi` / đăng nhập gọi `CartsApi`, gọi `useCart()` ngoài Provider → throw |
| `context/auth-context.tsx` | login lưu token, logout xoá token, `isAuthenticated` đúng trạng thái |
| `context/notification-context.tsx` | thêm/đọc thông báo, đếm chưa đọc |
| `hooks/use-mobile.tsx` | trả `true/false` theo `matchMedia` |

Dùng `QueryClientProvider` (QueryClient mới mỗi test, `retry: false`) làm wrapper cho các context dùng React Query.

---

### 🔵 Giai đoạn F4 — Component

**Mục tiêu:** render component và tương tác như người dùng (`@testing-library/user-event`).

Ưu tiên component **có logic** (form, validate, tính toán), bỏ qua component thuần hiển thị và toàn bộ `components/ui/*` (shadcn — thư viện ngoài).

Ứng viên: `features/cart/components/Cart.tsx`, `Checkout.tsx`, các form trong `features/auth`, `features/products`.

```typescript
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

// Query theo role/label/text — góc nhìn người dùng, KHÔNG dùng test-id/class
const button = screen.getByRole("button", { name: /thêm vào giỏ/i });
await userEvent.click(button);
expect(mockAddToCart).toHaveBeenCalled();
```

---

### 🔵 Giai đoạn F5 — Đo coverage & lấp lỗ hổng

```bash
npm run test:coverage
```
Xem file/nhánh nào chưa được test, bổ sung. Đặt mục tiêu thực tế (vd 70-80% cho thư mục `features` và `utils`), không cố ép 100%.

---

## Phần 4 — Cheat-sheet tra nhanh

### Mockito (Backend)

| Muốn... | Viết |
|---|---|
| Trả giá trị | `when(mock.f()).thenReturn(x)` |
| Ném lỗi (có return) | `when(mock.f()).thenThrow(new ...())` |
| Ném lỗi (void) | `doThrow(new ...()).when(mock).f()` |
| Kiểm đã gọi | `verify(mock).f()` |
| Kiểm KHÔNG gọi | `verify(mock, never()).f()` |
| Kiểm gọi N lần | `verify(mock, times(2)).f()` |
| Bắt tham số | `ArgumentCaptor.forClass(X.class)` |
| Khớp bất kỳ / cụ thể | `any()`, `anyLong()`, `eq(val)` |
| Object thật (không mock) | `@Spy ObjectMapper m = new ObjectMapper()` |
| Chặn `new X()` trong service | `mockConstruction(X.class, (mock, ctx) -> …)` |
| Bỏ qua cảnh báo stub thừa | `lenient().when(…)` |
| Kiểm thứ tự gọi | `inOrder(a, b); order.verify(a)…` |
| Set field `@Value` | `ReflectionTestUtils.setField(svc, "KEY", val)` |

### AssertJ (Backend)

| Muốn... | Viết |
|---|---|
| Bằng nhau | `assertThat(x).isEqualTo(y)` |
| Cùng object | `assertThat(x).isSameAs(y)` |
| BigDecimal | `assertThat(x).isEqualByComparingTo("100")` |
| Ném exception | `assertThatThrownBy(() -> ...).isInstanceOf(AppException.class)` |
| Không ném | `assertThatCode(() -> ...).doesNotThrowAnyException()` |
| List | `assertThat(list).hasSize(2).contains(a)` |

### Vitest (Frontend)

| Muốn... | Viết |
|---|---|
| Bằng (primitive) | `expect(x).toBe(y)` |
| Bằng (object) | `expect(x).toEqual(y)` |
| Chứa chuỗi con | `expect(s).toContain("abc")` |
| Hàm giả | `const f = vi.fn()` |
| Mock module | `vi.mock("đường-dẫn", () => ({...}))` |
| Kiểm đã gọi | `expect(f).toHaveBeenCalledWith(...)` |
| Reset mock | `vi.clearAllMocks()` |
| Chờ async | `await waitFor(() => expect(...))` |

---

## Phần 5 — Checklist theo dõi tiến độ

### Backend
- [x] **B1** — `SlugUtilTest`, `SortUtilsTest`
- [x] **B2** — `ProductServiceTest`, `CategoryServiceTest`, `BrandServiceTest`
- [x] **B3** — `OrderServiceTest`, `CartServiceTest`, `PostServiceTest`
- [x] **B4** — Auth/Payment/ResetPassword/Google/Otp + 22 service còn lại (239 test, 0 failure)
- [ ] **B5** — (tùy chọn) Controller slice test
- [ ] **B6** — (tùy chọn) Repository slice test
- [ ] **B7** — (tùy chọn) Integration test với Testcontainers

### Frontend
- [ ] **F1** — Cài Vitest + `cn.test.ts`, `contentRenderer.test.ts`
- [ ] **F2** — Test lớp API (`carts`, `session-carts`, `products`, `productImages`)
- [ ] **F3** — Context & hooks (`cart-context`, `auth-context`, `notification-context`, `use-mobile`)
- [ ] **F4** — Component có logic (Cart, Checkout, các form)
- [ ] **F5** — Đo coverage & lấp lỗ hổng

---

## Đang ở đâu & làm gì tiếp

**Đã xong:** B1 → B4. Toàn bộ tầng service của backend đã được phủ unit test.

```bash
cd backend && ./mvnw test        # 239 test, ~20 giây, không cần Postgres/Redis/Kafka
```

**Bước tiếp theo — chọn 1 trong 2 hướng:**

| Hướng | Khi nào chọn | Bắt đầu từ |
|---|---|---|
| **B5 — Controller slice test** | muốn kiểm tầng HTTP + phân quyền `@PreAuthorize` (ở B1–B4 annotation này KHÔNG chạy) | `ProductController`, `OrderController`, `AuthenticationController` |
| **F1 — Frontend** | muốn phủ nốt nửa còn lại của dự án | cài Vitest + `cn.test.ts`, `contentRenderer.test.ts` |

Gợi ý: làm **F1** trước — frontend hiện đang 0% coverage, giá trị thu về lớn hơn B5. 💪
