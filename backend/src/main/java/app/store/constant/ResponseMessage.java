// src/main/java/app/store/constant/ResponseMessage.java
package app.store.constant;

public final class ResponseMessage {

    private ResponseMessage() {
        // Private constructor to prevent instantiation
    }

    // Authentication Messages
    public static final String AUTHENTICATE_SUCCESS = "Authenticate successful";
    public static final String REFRESH_SUCCESS = "Refresh successful";
    public static final String INTROSPECT_SUCCESS = "Introspect successful";
    public static final String LOGOUT_SUCCESS = "Logout successful";
    public static final String REGISTER_SUCCESS = "Register successful";
    // Password Reset Flow Messages
    public static final String INIT_RESET_PASSWORD_SUCCESS = "Mã OTP đã được gửi đến email của bạn";
    public static final String VERIFY_OTP_SUCCESS = "Xác thực OTP thành công";
    public static final String CONFIRM_RESET_PASSWORD_SUCCESS = "Đặt lại mật khẩu thành công";
    public static final String RESEND_OTP_SUCCESS = "Mã OTP mới đã được gửi";
    public static final String CHANGE_PASSWORD_SUCCESS = "Đổi mật khẩu thành công";
    // User Messages
    public static final String CREATE_USER_SUCCESS = "User created successfully";
    public static final String GET_ALL_USERS_SUCCESS = "Get all users successful";
    public static final String GET_MY_INFO_SUCCESS = "Get my info successful";
    public static final String GET_USER_SUCCESS = "Get user successful";
    public static final String UPDATE_USER_SUCCESS = "User updated successfully";
    public static final String DELETE_USER_SUCCESS = "User deleted successfully";

    // Cart Messages
    public static final String GET_CART_SUCCESS = "Get cart successful";
    public static final String ADD_ITEM_SUCCESS = "Item added to cart successfully";
    public static final String REMOVE_ITEM_SUCCESS = "Item removed from cart successfully";
    public static final String UPDATE_ITEM_SUCCESS = "Item updated in cart successfully";

    // Category Messages
    public static final String CREATE_CATEGORY_SUCCESS = "Category created successfully";
    public static final String GET_ALL_CATEGORIES_SUCCESS = "Get all categories successful";
    public static final String GET_CATEGORY_SUCCESS = "Get category successful";
    public static final String UPDATE_CATEGORY_SUCCESS = "Category updated successfully";
    public static final String DELETE_CATEGORY_SUCCESS = "Category deleted successfully";
    public static final String GET_CATEGORY_BRANDS_SUCCESS = "Get category brands successful";
    public static final String UPDATE_CATEGORY_BRANDS_SUCCESS = "Category brands updated successfully";

    // Brand Messages
    public static final String CREATE_BRAND_SUCCESS = "Brand created successfully";
    public static final String GET_ALL_BRANDS_SUCCESS = "Get all brands successful";
    public static final String GET_BRAND_SUCCESS = "Get brand successful";
    public static final String UPDATE_BRAND_SUCCESS = "Brand updated successfully";
    public static final String DELETE_BRAND_SUCCESS = "Brand deleted successfully";

    // Banner Messages
    public static final String CREATE_BANNER_SUCCESS = "Banner created successfully";
    public static final String GET_ALL_BANNERS_SUCCESS = "Get all banners successful";
    public static final String GET_BANNER_SUCCESS = "Get banner successful";
    public static final String UPDATE_BANNER_SUCCESS = "Banner updated successfully";
    public static final String DELETE_BANNER_SUCCESS = "Banner deleted successfully";

    // Product Messages
    public static final String CREATE_PRODUCT_SUCCESS = "Product created successfully";
    public static final String GET_ALL_PRODUCTS_SUCCESS = "Get all products successful";
    public static final String GET_PRODUCT_SUCCESS = "Get product successful";
    public static final String UPDATE_PRODUCT_SUCCESS = "Product updated successfully";
    public static final String DELETE_PRODUCT_SUCCESS = "Product deleted successfully";
    public static final String GET_ALL_PRODUCTS_BY_CATEGORY_SUCCESS = "Get all products by category successful";
    public static final String GET_ALL_PRODUCTS_BY_BRAND_SUCCESS = "Get all products by brand successful";

    // Order Messages
    public static final String CREATE_ORDER_SUCCESS = "Order created successfully";
    public static final String GET_ALL_ORDERS_SUCCESS = "Get all orders successful";
    public static final String GET_ORDER_SUCCESS = "Get order successful";
    public static final String GET_MY_ORDER_SUCCESS = "Get my order successful";
    public static final String UPDATE_ORDER_BY_ADMIN_SUCCESS = "Order updated successfully by admin";
    public static final String UPDATE_ORDER_BY_USER_SUCCESS = "Order updated successfully by user";
    public static final String DELETE_ORDER_SUCCESS = "Order deleted successfully";
    public static final String CANCEL_ORDER_SUCCESS = "Order cancelled successfully";

    // Payment Messages
    public static final String CREATE_PAYMENT_SUCCESS = "Payment QR created successfully";
    public static final String CHECK_PAYMENT_STATUS_SUCCESS = "Payment status retrieved successfully";
    public static final String SEPAY_WEBHOOK_SUCCESS = "SePay webhook processed successfully";

    // Permission Messages
    public static final String CREATE_PERMISSION_SUCCESS = "Permission created successfully";
    public static final String GET_ALL_PERMISSIONS_SUCCESS = "Get all permissions successful";
    public static final String GET_PERMISSION_SUCCESS = "Get permission successful";
    public static final String UPDATE_PERMISSION_SUCCESS = "Permission updated successfully";
    public static final String DELETE_PERMISSION_SUCCESS = "Permission deleted successfully";

    // Role Messages
    public static final String CREATE_ROLE_SUCCESS = "Role created successfully";
    public static final String GET_ALL_ROLES_SUCCESS = "Get all roles successful";
    public static final String GET_ROLE_SUCCESS = "Get role successful";
    public static final String UPDATE_ROLE_SUCCESS = "Role updated successfully";
    public static final String DELETE_ROLE_SUCCESS = "Role deleted successfully";

    // Post Category Messages
    public static final String CREATE_POST_CATEGORY_SUCCESS = "Post category created successfully";
    public static final String GET_ALL_POST_CATEGORIES_SUCCESS = "Get all post categories successful";
    public static final String GET_POST_CATEGORY_SUCCESS = "Get post category successful";
    public static final String UPDATE_POST_CATEGORY_SUCCESS = "Post category updated successfully";
    public static final String DELETE_POST_CATEGORY_SUCCESS = "Post category deleted successfully";
    public static final String SEARCH_POST_CATEGORIES_SUCCESS = "Search post categories successful";

    // Post Messages
    public static final String CREATE_POST_SUCCESS = "Post created successfully";
    public static final String GET_ALL_POSTS_SUCCESS = "Get all posts successful";
    public static final String GET_POST_SUCCESS = "Get post successful";
    public static final String UPDATE_POST_SUCCESS = "Post updated successfully";
    public static final String DELETE_POST_SUCCESS = "Post deleted successfully";
    public static final String TOGGLE_POST_PUBLISH_SUCCESS = "Post publish status toggled successfully";
    public static final String SEARCH_POSTS_SUCCESS = "Search posts successful";
    public static final String GET_POSTS_BY_CATEGORY_SUCCESS = "Get posts by category successful";
    public static final String GET_RELATED_POSTS_SUCCESS = "Get related posts successful";
    public static final String GET_MOST_VIEWED_POSTS_SUCCESS = "Get most viewed posts successful";

    // Notification Messages
    public static final String GET_NOTIFICATIONS_SUCCESS = "Get notifications successful";
    public static final String GET_UNREAD_COUNT_SUCCESS = "Get unread count successful";
    public static final String MARK_NOTIFICATION_READ_SUCCESS = "Notification marked as read";
    public static final String MARK_ALL_NOTIFICATIONS_READ_SUCCESS = "All notifications marked as read";


}