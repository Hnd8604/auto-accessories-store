package app.store.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    /*
     * Error Code Convention
     *
     * 1000 - 1099 : Common / User
     * 1100 - 1199 : Authentication / Authorization
     * 1200 - 1299 : Role
     * 1300 - 1399 : Permission
     * 1400 - 1499 : Password Reset
     * 1500 - 1599 : Password Change
     * 1600 - 1699 : External Identity Provider
     * 2000 - 2099 : Product / Product Image
     * 2100 - 2199 : Category
     * 2200 - 2299 : Brand
     * 3000 - 3099 : Order
     * 3100 - 3199 : Cart
     * 3200 - 3299 : Payment / Webhook
     * 4000 - 4099 : Post / Post Category
     * 4100 - 4199 : Banner
     * 5000 - 5999 : External Integration (Reserved)
     * 6000 - 6099 : Notification
     * 7000 - 7099 : Professional Service
     * 8000 - 8099 : Live Chat
     * 9000 - 9099 : HTTP / Request / Platform
     * 9999        : Uncategorized Error
     */

    USER_EXISTED(1001, "User existed", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1002, "Email existed", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1101, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1102, "You do not have permission", HttpStatus.FORBIDDEN),
    ROLE_NOT_EXISTED(1201, "Role not existed", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_EXISTED(1301, "Permission not existed", HttpStatus.NOT_FOUND),
    EMAIL_NOT_EXISTED(1401, "Email does not exist", HttpStatus.NOT_FOUND),
    RESET_SESSION_NOT_FOUND(1402, "Password reset session not found or expired", HttpStatus.BAD_REQUEST),
    RESET_INVALID_STEP(1403, "Invalid verification step", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1404, "OTP has expired", HttpStatus.BAD_REQUEST),
    OTP_INVALID(1405, "OTP is incorrect", HttpStatus.BAD_REQUEST),
    OTP_MAX_ATTEMPTS_EXCEEDED(1406, "Maximum OTP attempts exceeded", HttpStatus.TOO_MANY_REQUESTS),
    OTP_RESEND_TOO_SOON(1407, "Please wait before requesting a new OTP", HttpStatus.TOO_MANY_REQUESTS),
    WRONG_CURRENT_PASSWORD(1501, "Current password is incorrect", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_SAME_AS_CURRENT(1502, "New password must be different from current password", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRMATION_MISMATCH(1503, "Password confirmation does not match", HttpStatus.BAD_REQUEST),
    GOOGLE_AUTH_FAILED(1601, "Google sign-in failed", HttpStatus.UNAUTHORIZED),

    PRODUCT_NOT_EXISTED(2001, "Product not existed", HttpStatus.NOT_FOUND),
    PRODUCT_IMAGE_NOT_EXISTED(2002, "Product image not existed", HttpStatus.NOT_FOUND),
    IMAGE_NOT_IN_PRODUCT(2003, "Image does not belong to this product", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_EXISTED(2101, "Category not existed", HttpStatus.NOT_FOUND),
    BRAND_NOT_EXISTED(2201, "Brand not existed", HttpStatus.NOT_FOUND),
    BRAND_NOT_IN_CATEGORY(2202, "Brand does not belong to category", HttpStatus.BAD_REQUEST),

    ORDER_NOT_EXISTED(3001, "Order not existed", HttpStatus.NOT_FOUND),
    ORDER_NOT_CANCELABLE(3002, "Only orders with status PENDING or PROCESSING can be canceled", HttpStatus.CONFLICT),
    CART_NOT_EXISTED(3101, "Cart not existed", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_EXISTED(3102, "Cart item not existed", HttpStatus.NOT_FOUND),
    INVALID_QUANTITY(3103, "Quantity must be greater than 0", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(3104, "Not enough stock available", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_IN_CART(3105, "Cart item does not belong to this cart", HttpStatus.NOT_FOUND),
    WEBHOOK_INVALID_SIGNATURE(3201, "Webhook signature verification failed", HttpStatus.UNAUTHORIZED),
    PAYMENT_GATEWAY_ERROR(3202, "Cannot connect to payment gateway, please try again", HttpStatus.BAD_GATEWAY),
    ORDER_ALREADY_PAID(3203, "Order has already been paid", HttpStatus.CONFLICT),
    ORDER_PAYMENT_METHOD_INVALID(3204, "Order does not use bank transfer", HttpStatus.CONFLICT),
    ORDER_CANCELED(3205, "Order has been canceled", HttpStatus.CONFLICT),

    POST_NOT_EXISTED(4001, "Post not existed", HttpStatus.NOT_FOUND),
    POST_CATEGORY_NOT_EXISTED(4002, "Post category not existed", HttpStatus.NOT_FOUND),
    POST_CATEGORY_EXISTED(4003, "Post category name already exists", HttpStatus.CONFLICT),
    POST_CATEGORY_HAS_POSTS(4004, "Cannot delete a category that still has posts", HttpStatus.CONFLICT),
    BANNER_NOT_EXISTED(4101, "Banner not existed", HttpStatus.NOT_FOUND),

    NOTIFICATION_NOT_FOUND(6001, "Notification not found", HttpStatus.NOT_FOUND),

    SERVICE_NOT_EXISTED(7001, "Service not existed", HttpStatus.NOT_FOUND),
    SERVICE_IMAGE_NOT_EXISTED(7002, "Service image not existed", HttpStatus.NOT_FOUND),
    IMAGE_NOT_IN_SERVICE(7003, "Image does not belong to this service", HttpStatus.NOT_FOUND),
    IMAGE_STORAGE_ERROR(7004, "Image storage service failed, please try again", HttpStatus.BAD_GATEWAY),

    CONVERSATION_NOT_EXISTED(8001, "Conversation not existed", HttpStatus.NOT_FOUND),
    CONVERSATION_CLOSED(8002, "Conversation is closed", HttpStatus.BAD_REQUEST),

    MALFORMED_REQUEST(9001, "Malformed request", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED(9002, "HTTP method not supported", HttpStatus.METHOD_NOT_ALLOWED),
    ENDPOINT_NOT_FOUND(9003, "Endpoint not found", HttpStatus.NOT_FOUND),
    PAYLOAD_TOO_LARGE(9004, "File size exceeds the limit", HttpStatus.PAYLOAD_TOO_LARGE),
    DATA_CONFLICT(9005, "Data conflicts with existing records", HttpStatus.CONFLICT),
    UNSUPPORTED_MEDIA_TYPE(9006, "Media type is not supported", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    NOT_ACCEPTABLE(9007, "Requested response type is not supported", HttpStatus.NOT_ACCEPTABLE),
    ASYNC_REQUEST_TIMEOUT(9008, "Request timed out", HttpStatus.SERVICE_UNAVAILABLE),
    VALIDATION_ERROR(9009, "Invalid request data", HttpStatus.BAD_REQUEST),
    INVALID_ARGUMENT(9010, "Invalid argument", HttpStatus.BAD_REQUEST),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
