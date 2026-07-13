package app.store.exception;

import lombok.Getter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {


    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ARGUMENT(9998, "Invalid argument: {message}", HttpStatus.BAD_REQUEST),

    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1002, "Email existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be between 3 and 20 characters long", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),

    INVALID_DATE_OF_BIRTH(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),

    // user

    // product
    PRODUCT_NOT_EXISTED(1005, "Product not existed", HttpStatus.NOT_FOUND),
    // category
    CATEGORY_NOT_EXISTED(1005, "Category not existed", HttpStatus.NOT_FOUND),
    // brand
    BRAND_NOT_EXISTED(1005, "Brand not existed", HttpStatus.NOT_FOUND),
    BRAND_NOT_IN_CATEGORY(1006, "Brand does not belong to category", HttpStatus.BAD_REQUEST),
    // order
    ORDER_NOT_EXISTED(1005, "Order not existed", HttpStatus.NOT_FOUND),
    // cart
    CART_NOT_EXISTED(1005, "Cart not existed", HttpStatus.NOT_FOUND),
    // cart item
    CART_ITEM_NOT_EXISTED(1005, "Cart item not existed", HttpStatus.NOT_FOUND),

    // product image
    PRODUCT_IMAGE_NOT_EXISTED(1005, "Product image not existed", HttpStatus.NOT_FOUND),
    // role
    ROLE_NOT_EXISTED(1005, "Role not existed", HttpStatus.NOT_FOUND),

    // permission
    PERMISSION_NOT_EXISTED(1005, "Permission not existed", HttpStatus.NOT_FOUND),
    
    // Password Reset
    EMAIL_NOT_EXISTED(2001, "Email không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    RESET_SESSION_NOT_FOUND(2002, "Phiên đặt lại mật khẩu không tồn tại hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    RESET_INVALID_STEP(2003, "Bước xác thực không hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(2004, "Mã OTP đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_INVALID(2005, "Mã OTP không đúng", HttpStatus.BAD_REQUEST),
    OTP_MAX_ATTEMPTS_EXCEEDED(2006, "Đã vượt quá số lần thử OTP cho phép", HttpStatus.TOO_MANY_REQUESTS),
    OTP_RESEND_TOO_SOON(2007, "Vui lòng đợi trước khi gửi lại OTP", HttpStatus.TOO_MANY_REQUESTS),

    // Payment / Webhook
    WEBHOOK_INVALID_SIGNATURE(3001, "Webhook API key verification failed", HttpStatus.UNAUTHORIZED),

    // Change Password
    WRONG_CURRENT_PASSWORD(4001, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_SAME_AS_CURRENT(4002, "Mật khẩu mới không được trùng mật khẩu hiện tại", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRMATION_MISMATCH(4003, "Xác nhận mật khẩu không khớp", HttpStatus.BAD_REQUEST),

    // Google OAuth2
    GOOGLE_AUTH_FAILED(5001, "Đăng nhập bằng Google thất bại", HttpStatus.UNAUTHORIZED),

    // Notification
    NOTIFICATION_NOT_FOUND(6001, "Notification not found", HttpStatus.NOT_FOUND),

    // Professional Service
    SERVICE_NOT_EXISTED(7001, "Service not existed", HttpStatus.NOT_FOUND),
    SERVICE_IMAGE_NOT_EXISTED(7002, "Service image not existed", HttpStatus.NOT_FOUND)
    ;

    private ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }


    private HttpStatusCode statusCode;
    private int code;
    private String message;



}