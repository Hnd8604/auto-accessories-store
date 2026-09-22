# Giải thích `GlobalExceptionHandler`

Tài liệu này giải thích cách
[`GlobalExceptionHandler`](../backend/src/main/java/app/store/exception/GlobalExceptionHandler.java)
xử lý exception tập trung cho REST API Spring Boot của dự án.

Các file liên quan:

- [`ErrorCode`](../backend/src/main/java/app/store/exception/ErrorCode.java): định nghĩa mã lỗi, thông báo mặc định và HTTP status.
- [`AppException`](../backend/src/main/java/app/store/exception/AppException.java): exception nghiệp vụ của ứng dụng.
- [`ApiResponse`](../backend/src/main/java/app/store/dto/response/auth/ApiResponse.java): định dạng JSON trả về cho client.
- [`GlobalExceptionHandlerTest`](../backend/src/test/java/app/store/exception/GlobalExceptionHandlerTest.java): kiểm thử hành vi của các handler.

---

## 1. Vai trò của class

`GlobalExceptionHandler` là nơi chuyển các exception phát sinh trong controller,
service, repository hoặc Spring Framework thành response HTTP thống nhất.

Luồng xử lý tổng quát:

```text
Request
   |
   v
Controller -> Service -> Repository
   |                         |
   +------ exception <-------+
              |
              v
   GlobalExceptionHandler
              |
              v
HTTP status + headers + ApiResponse JSON
```

Nhờ có class này, từng controller không cần lặp lại các khối `try/catch` để tạo
response lỗi. Nó cũng ngăn client nhìn thấy stack trace, câu SQL, tên constraint
hoặc các thông tin nội bộ khác.

---

## 2. Annotation trên class

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
```

### `@RestControllerAdvice`

`@RestControllerAdvice` kết hợp hai chức năng:

- `@ControllerAdvice`: áp dụng các handler cho nhiều REST controller.
- `@ResponseBody`: serialize giá trị trả về thành response body, thường là JSON.

Khi một exception được ném ra, Spring tìm phương thức có `@ExceptionHandler`
phù hợp nhất và gọi phương thức đó.

### `@Slf4j`

Lombok tự sinh logger có tên `log`, nhờ đó class có thể gọi:

```java
log.debug(...);
log.warn(...);
log.error(...);
```

---

## 3. Định dạng response lỗi

Phần lớn handler trả về:

```java
ResponseEntity<ApiResponse<?>>
```

`ResponseEntity` chứa ba phần:

- Body: một `ApiResponse`.
- Headers: các HTTP header cần gửi về client.
- Status: ví dụ `400`, `403`, `404`, `409` hoặc `500`.

`ApiResponse` được khai báo như sau:

```java
public record ApiResponse<T>(
        int code,
        String message,
        T result) {
}
```

Do class có `@JsonInclude(JsonInclude.Include.NON_NULL)`, trường `result` không
được đưa vào JSON khi có giá trị `null`. Một response lỗi thông thường có dạng:

```json
{
  "code": 9001,
  "message": "Malformed request"
}
```

Trong đó:

- HTTP status là chuẩn giao tiếp HTTP, ví dụ `400 Bad Request`.
- `code` là mã nghiệp vụ của ứng dụng, ví dụ `9001`.
- `message` là thông báo ổn định để client hiển thị hoặc xử lý.

---

## 4. Bảng ánh xạ exception

| Exception | `ErrorCode` | HTTP status | Ý nghĩa |
|---|---|---:|---|
| `AppException` | Mã nằm trong exception | Tùy mã | Lỗi nghiệp vụ chủ động |
| `AccessDeniedException` | `UNAUTHORIZED` | 403 | Người dùng không đủ quyền |
| `DataIntegrityViolationException` với SQL state `23505`/`23503` | `DATA_CONFLICT` | 409 | Trùng unique hoặc xung đột foreign key |
| Các lỗi `DataAccessException`, `TransactionException` khác | `UNCATEGORIZED_EXCEPTION` | 500 | Lỗi database/transaction |
| `MethodArgumentNotValidException` | `VALIDATION_ERROR` | 400 | DTO request body không hợp lệ |
| `HandlerMethodValidationException` | `VALIDATION_ERROR` | 400 | Parameter của controller không hợp lệ |
| `HttpMessageNotReadableException` | `MALFORMED_REQUEST` | 400 | Không đọc/parse được request body |
| `TypeMismatchException` | `MALFORMED_REQUEST` | 400 | Sai kiểu path/query parameter |
| `MissingServletRequestParameterException` | `MALFORMED_REQUEST` | 400 | Thiếu query parameter bắt buộc |
| `MissingServletRequestPartException` | `MALFORMED_REQUEST` | 400 | Thiếu một phần multipart bắt buộc |
| `ServletRequestBindingException` | `MALFORMED_REQUEST` | 400 | Lỗi binding request khác |
| `HttpRequestMethodNotSupportedException` | `METHOD_NOT_ALLOWED` | 405 | Dùng sai HTTP method |
| `NoHandlerFoundException`, `NoResourceFoundException` | `ENDPOINT_NOT_FOUND` | 404 | Không tìm thấy endpoint/tài nguyên |
| `MaxUploadSizeExceededException` | `PAYLOAD_TOO_LARGE` | 413 | File upload vượt giới hạn |
| `HttpMediaTypeNotSupportedException` | `UNSUPPORTED_MEDIA_TYPE` | 415 | `Content-Type` không được hỗ trợ |
| `HttpMediaTypeNotAcceptableException` | `NOT_ACCEPTABLE` | 406 | Server không tạo được kiểu response client yêu cầu |
| `AsyncRequestTimeoutException` | `ASYNC_REQUEST_TIMEOUT` | 503 | Request bất đồng bộ hết thời gian |
| `AsyncRequestNotUsableException` | Không tạo response mới | — | Async response/kết nối không dùng được nữa |
| `ConversionNotSupportedException` | `UNCATEGORIZED_EXCEPTION` | 500 | Server thiếu hoặc sai converter |
| `HttpMessageNotWritableException` | `UNCATEGORIZED_EXCEPTION` | 500 | Không serialize được response |
| `MultipartException` | `MALFORMED_REQUEST` | 400 | Multipart request sai định dạng |
| Exception còn lại | `UNCATEGORIZED_EXCEPTION` | 500 | Lỗi chưa được dự đoán |

Spring chọn handler có kiểu exception cụ thể nhất. Ví dụ,
`DataIntegrityViolationException` là một `DataAccessException`, nhưng nó vẫn đi
vào handler dành riêng cho `DataIntegrityViolationException`. Vị trí trước/sau
của các phương thức trong file không quyết định độ ưu tiên.

---

## 5. Xử lý `AppException`

```java
@ExceptionHandler(AppException.class)
ResponseEntity<ApiResponse<?>> handlingAppException(AppException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    if (errorCode.getStatusCode().is5xxServerError()) {
        log.error("Application failure, returning {}", errorCode.getStatusCode(), exception);
    }
    return build(errorCode, exception.getMessage());
}
```

`AppException` là exception nghiệp vụ do ứng dụng chủ động ném ra:

```java
throw new AppException(ErrorCode.ORDER_NOT_CANCELABLE);
```

Response tương ứng:

```http
HTTP/1.1 409 Conflict
Content-Type: application/json
```

```json
{
  "code": 1011,
  "message": "Only orders with status PENDING or PROCESSING can be canceled"
}
```

`AppException` cũng hỗ trợ custom message:

```java
throw new AppException(
        ErrorCode.INVALID_ARGUMENT,
        "Quantity must be greater than available stock");
```

Handler lấy mã và HTTP status từ `ErrorCode`, nhưng dùng message nằm trong
exception. Nếu mã lỗi thuộc nhóm `5xx`, exception được ghi log ở mức `error`.

Không nên truyền trực tiếp message thô của database hoặc thư viện bên ngoài vào
custom message vì nội dung đó sẽ được gửi cho client.

---

## 6. Lỗi phân quyền

```java
@ExceptionHandler(AccessDeniedException.class)
ResponseEntity<ApiResponse<?>> handlingAccessDeniedException() {
    return build(ErrorCode.UNAUTHORIZED);
}
```

`AccessDeniedException` thường xuất hiện khi người dùng đã đăng nhập nhưng không
có quyền thực hiện hành động. Response là:

```http
HTTP/1.1 403 Forbidden
```

```json
{
  "code": 1102,
  "message": "You do not have permission"
}
```

Tên enum `UNAUTHORIZED` hơi dễ nhầm vì trong chuẩn HTTP:

- `401 Unauthorized`: chưa được xác thực.
- `403 Forbidden`: đã xác thực nhưng không đủ quyền.

Ở dự án này, `UNAUTHORIZED` ánh xạ đến `HttpStatus.FORBIDDEN`, nên hành vi HTTP
vẫn đúng dù tên có thể đổi thành `FORBIDDEN` để rõ nghĩa hơn.

---

## 7. Lỗi ràng buộc dữ liệu

```java
private static final String UNIQUE_VIOLATION = "23505";
private static final String FOREIGN_KEY_VIOLATION = "23503";
```

Hai SQL state này biểu diễn:

- `23505`: vi phạm unique constraint, ví dụ email đã tồn tại.
- `23503`: vi phạm foreign key, ví dụ xóa bản ghi vẫn đang được tham chiếu.

Handler lấy SQL state từ chuỗi nguyên nhân của exception:

```java
@ExceptionHandler(DataIntegrityViolationException.class)
ResponseEntity<ApiResponse<?>> handlingDataIntegrityViolationException(
        DataIntegrityViolationException exception) {
    String sqlState = findSqlState(exception);
    if (UNIQUE_VIOLATION.equals(sqlState) || FOREIGN_KEY_VIOLATION.equals(sqlState)) {
        log.warn("Database conflict (SQL state {})", sqlState);
        return build(ErrorCode.DATA_CONFLICT);
    }

    log.error("Unexpected data integrity failure", exception);
    return build(ErrorCode.UNCATEGORIZED_EXCEPTION);
}
```

Các xung đột dự kiến trả về:

```http
HTTP/1.1 409 Conflict
```

```json
{
  "code": 9005,
  "message": "Data conflicts with existing records"
}
```

Các lỗi toàn vẹn khác, chẳng hạn SQL state `23502` cho vi phạm `NOT NULL`, được
xem là lỗi server bất ngờ và trả `500`. Message gốc của database không được gửi
về client, tránh làm lộ tên bảng, tên cột hoặc constraint.

Handler kế tiếp bắt các lỗi database và transaction rộng hơn:

```java
@ExceptionHandler({DataAccessException.class, TransactionException.class})
ResponseEntity<ApiResponse<?>> handlingInfrastructureException(RuntimeException exception) {
    log.error("Infrastructure failure", exception);
    return build(ErrorCode.UNCATEGORIZED_EXCEPTION);
}
```

Ví dụ: mất kết nối database, query thất bại, deadlock hoặc transaction không
commit được.

---

## 8. Hai loại lỗi validation

### 8.1. Validation của request body

`MethodArgumentNotValidException` thường phát sinh từ `@Valid @RequestBody`:

```java
@PostMapping
void create(@Valid @RequestBody UserCreationRequest request) {
}
```

Nếu DTO có constraint:

```java
@Size(min = 8, message = "Password must be at least 8 characters")
private String password;
```

thì response có thể là:

```json
{
  "code": 9009,
  "message": "Password must be at least 8 characters"
}
```

### 8.2. Validation của method parameter

`HandlerMethodValidationException` xử lý constraint đặt trực tiếp trên parameter:

```java
@GetMapping
void list(
        @RequestParam
        @Min(value = 1, message = "Page must be at least 1")
        int page) {
}
```

Request `GET /products?page=0` sẽ nhận lỗi `VALIDATION_ERROR` với message
`Page must be at least 1`.

Cả hai handler gọi `firstValidationMessage()`, vì vậy API chỉ trả thông báo lỗi
đầu tiên thay vì toàn bộ danh sách lỗi.

---

## 9. Request không hợp lệ

### Request body không đọc được

`HttpMessageNotReadableException` xuất hiện khi JSON sai cú pháp, field sai kiểu,
enum không hợp lệ hoặc body không parse được:

```json
{
  "code": 9001,
  "message": "Malformed request body"
}
```

Handler không trả `exception.getMessage()` vì message của Jackson thường dài,
khó hiểu và có thể chứa tên class Java nội bộ.

### Path/query parameter sai kiểu

Với endpoint nhận `Long id`, request `/products/abc` làm phát sinh
`MethodArgumentTypeMismatchException`. Đoạn pattern matching:

```java
exception instanceof MethodArgumentTypeMismatchException mismatch
```

vừa kiểm tra subtype vừa tạo biến `mismatch`. Response là:

```json
{
  "code": 9001,
  "message": "Invalid value for parameter: id"
}
```

### Thiếu request parameter

Nếu endpoint khai báo `@RequestParam String keyword` nhưng request không có
`keyword`, handler trả:

```json
{
  "code": 9001,
  "message": "Missing required parameter: keyword"
}
```

### Thiếu multipart part

Nếu endpoint yêu cầu `@RequestPart MultipartFile image` nhưng request không có
phần `image`, handler trả:

```json
{
  "code": 9001,
  "message": "Missing required part: image"
}
```

### Multipart request sai định dạng

`MultipartException` xử lý các trường hợp như thiếu boundary, body upload bị cắt
hoặc dữ liệu multipart không parse được. Response dùng message ổn định
`Malformed multipart request`.

`MaxUploadSizeExceededException` có handler cụ thể hơn, vì vậy file vượt dung
lượng sẽ trả `413`, không đi vào handler multipart chung.

---

## 10. Các lỗi giao thức HTTP

### Sai HTTP method

Nếu endpoint chỉ nhận `POST` nhưng client gọi bằng `GET`, Spring ném
`HttpRequestMethodNotSupportedException`:

```http
HTTP/1.1 405 Method Not Allowed
Allow: POST
```

```json
{
  "code": 9002,
  "message": "HTTP method not supported"
}
```

Handler truyền `exception.getHeaders()` vào response để giữ header `Allow`.

### Không tìm thấy endpoint

`NoHandlerFoundException` và `NoResourceFoundException` cùng được chuyển thành:

```http
HTTP/1.1 404 Not Found
```

```json
{
  "code": 9003,
  "message": "Endpoint not found"
}
```

### File quá lớn

`MaxUploadSizeExceededException` trả `413 Payload Too Large`:

```json
{
  "code": 9004,
  "message": "File size exceeds the limit"
}
```

### `Content-Type` không được hỗ trợ

Nếu endpoint chỉ nhận JSON nhưng client gửi `Content-Type: text/plain`, handler
cho `HttpMediaTypeNotSupportedException` trả `415 Unsupported Media Type`:

```json
{
  "code": 9006,
  "message": "Media type is not supported"
}
```

### Client không chấp nhận kiểu response

Nếu client gửi `Accept: text/plain` trong khi endpoint chỉ tạo JSON, Spring ném
`HttpMediaTypeNotAcceptableException`. Handler trả `406 Not Acceptable` với body
trống:

```java
return new ResponseEntity<>(
        null,
        exception.getHeaders(),
        ErrorCode.NOT_ACCEPTABLE.getStatusCode());
```

Không thể trả `ApiResponse` JSON an toàn vì client đã tuyên bố rằng nó không
chấp nhận JSON.

---

## 11. Lỗi async và streaming

### Async request hết thời gian

`AsyncRequestTimeoutException` có thể xuất hiện với `Callable`, `DeferredResult`,
SSE hoặc streaming response.

```java
if (response.isCommitted()) {
    return null;
}
```

Nếu response chưa được gửi, handler trả:

```http
HTTP/1.1 503 Service Unavailable
```

```json
{
  "code": 9008,
  "message": "Request timed out"
}
```

Nếu response đã committed, status/header/body đã bắt đầu được gửi nên handler
không thể an toàn ghi một response lỗi mới và phải trả `null`.

### Async response không còn dùng được

`AsyncRequestNotUsableException` thường xảy ra khi client đóng kết nối, trình
duyệt hủy request hoặc server ghi vào một stream đã đóng. Handler chỉ ghi log ở
mức `debug` và không tạo response mới vì kết nối không còn dùng được.

---

## 12. Lỗi phía server

### Không hỗ trợ conversion

`ConversionNotSupportedException` thường thể hiện server thiếu hoặc cấu hình sai
converter. Nó khác `TypeMismatchException`, vốn thường do dữ liệu client gửi sai.
Vì đây là lỗi phía server nên response là `500`.

### Không serialize được response

`HttpMessageNotWritableException` xảy ra khi Jackson không thể chuyển kết quả của
controller thành JSON, ví dụ do vòng lặp object, getter ném exception hoặc kiểu dữ
liệu không serialize được. Handler ghi đầy đủ exception vào log và trả lỗi tổng
quát.

Nếu response đã được ghi một phần, server có thể không thay thế nó hoàn toàn bằng
JSON lỗi dù handler đã bắt được exception.

### Handler cuối cùng

```java
@ExceptionHandler(Exception.class)
ResponseEntity<ApiResponse<?>> handlingUnhandledException(Exception exception) {
    log.error("Unhandled exception", exception);
    return build(ErrorCode.UNCATEGORIZED_EXCEPTION);
}
```

Đây là lưới an toàn cho mọi exception chưa có handler cụ thể:

```http
HTTP/1.1 500 Internal Server Error
```

```json
{
  "code": 9999,
  "message": "Uncategorized error"
}
```

Message thật và stack trace chỉ được ghi vào log, không gửi cho client.

---

## 13. Các helper method

### Chuỗi overload `build()`

Class có bốn overload:

```java
build(errorCode)
build(errorCode, message)
build(errorCode, message, headers)
build(errorCode, message, headers, statusCode)
```

Chuỗi gọi mặc định:

```text
build(errorCode)
  -> build(errorCode, null)
      -> build(errorCode, null, HttpHeaders.EMPTY)
          -> build(errorCode, null, headers, errorCode.getStatusCode())
```

Overload cuối tạo `ResponseEntity` thật sự:

```java
return new ResponseEntity<>(response(errorCode, message), headers, statusCode);
```

Cách viết này tránh lặp code và cho phép từng handler chỉ truyền các giá trị mà
nó cần thay đổi.

### Tạo `ApiResponse`

```java
private ApiResponse<?> response(ErrorCode errorCode, String message) {
    return ApiResponse.builder()
            .code(errorCode.getCode())
            .message(message != null ? message : errorCode.getMessage())
            .build();
}
```

Nếu handler cung cấp custom message thì dùng message đó. Nếu `message == null`,
hàm dùng message mặc định trong `ErrorCode`.

### Lấy validation message đầu tiên

```java
private String firstValidationMessage(
        Iterable<? extends MessageSourceResolvable> errors) {
    for (MessageSourceResolvable error : errors) {
        if (error.getDefaultMessage() != null) {
            return error.getDefaultMessage();
        }
    }
    return ErrorCode.VALIDATION_ERROR.getMessage();
}
```

Hàm duyệt các lỗi và trả message đầu tiên khác `null`. Nếu không có message, nó
dùng fallback `Invalid request data`.

Ưu điểm là response đơn giản; hạn chế là frontend không nhận được toàn bộ lỗi của
các field trong cùng một request.

### Tìm SQL state trong chuỗi nguyên nhân

```java
private String findSqlState(Throwable exception) {
    Throwable current = exception;
    while (current != null) {
        if (current instanceof SQLException sqlException) {
            return sqlException.getSQLState();
        }
        current = current.getCause();
    }
    return null;
}
```

Exception database thường bị bọc qua nhiều lớp:

```text
DataIntegrityViolationException
└── Hibernate exception
    └── SQLException (SQL state = 23505)
```

Hàm lần lượt đi qua `getCause()` cho đến khi gặp `SQLException`. Cách so sánh
`UNIQUE_VIOLATION.equals(sqlState)` cũng an toàn khi `sqlState` là `null`.

---

## 14. Chiến lược ghi log

Class đang chia mức log theo tính chất lỗi:

| Mức log | Trường hợp |
|---|---|
| `debug` | Request sai định dạng, upload quá lớn, timeout hoặc client ngắt kết nối |
| `warn` | Xung đột database dự kiến như unique/foreign key |
| `error` | Lỗi server, database, transaction, serialize hoặc exception không lường trước |

Các lỗi `4xx` dự kiến không cần luôn ghi stack trace ở mức `error`, trong khi lỗi
`5xx` cần đủ thông tin để điều tra trên server.

---

## 15. Điểm mạnh và điểm có thể cải thiện

### Điểm mạnh

- Tạo định dạng lỗi thống nhất trên toàn API.
- Phân biệt lỗi client `4xx` với lỗi server `5xx`.
- Không làm lộ message nội bộ hoặc stack trace.
- Giữ các HTTP header quan trọng như `Allow`.
- Có xử lý riêng cho multipart, async và content negotiation.
- Chuyển xung đột database dự kiến thành `409 Conflict`.
- Có catch-all để tránh response lỗi mặc định thiếu nhất quán.
- Các overload `build()` giảm lặp code.
- Có test cho các nhánh xử lý quan trọng.

### Điểm có thể cải thiện

- Đổi tên `UNAUTHORIZED` thành `FORBIDDEN` để khớp với HTTP status `403`.
- Trả danh sách/map validation error nếu frontend cần hiển thị lỗi theo từng field.
- Duy trì quy ước mã theo domain và kiểm tra không trùng mã bằng `ErrorCodeTest`.
- Tách lỗi unique và foreign key thành message nghiệp vụ rõ hơn khi cần.
- Trong `findSqlState()`, nếu gặp `SQLException` có SQL state `null`, có thể tiếp
  tục duyệt cause thay vì trả ngay `null`.
- Chỉ dùng custom message của `AppException` khi nội dung đã được kiểm soát và an
  toàn để hiển thị cho client.

---

## 16. Kết luận

`GlobalExceptionHandler` là lớp chuyển đổi giữa exception nội bộ của Java/Spring
và hợp đồng lỗi HTTP dành cho client. Nó giúp controller gọn hơn, frontend nhận
response nhất quán hơn, đồng thời bảo vệ chi tiết triển khai bên trong hệ thống.
