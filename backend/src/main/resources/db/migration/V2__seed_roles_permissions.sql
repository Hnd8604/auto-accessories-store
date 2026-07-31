-- ============================================================
-- V2: Seed role & permission (thay cho app/store/seed/SeedRolePerms.java)
--
-- Trước đây seed nằm trong CommandLineRunner có @Profile("dev"),
-- nên DB production rỗng sẽ không có role nào -> ApplicationInitConfig
-- ném ROLE_NOT_EXISTED và app chết ngay khi khởi động. Đưa vào migration
-- thì mọi môi trường đều có cùng bộ dữ liệu.
--
-- LƯU Ý: phần đồng bộ role->permission sang REDIS không nằm ở đây được.
-- Tầng phân quyền đọc từ Redis key `role:<TEN>:perms`, và Flyway chỉ nói
-- chuyện với Postgres. Việc đó do RolePermissionRedisSync (Java) đảm nhiệm,
-- chạy ở mọi profile sau khi app khởi động.
--
-- Toàn bộ lệnh đều ON CONFLICT DO NOTHING để chạy lại không vỡ.
-- ============================================================

INSERT INTO permission (name, description) VALUES
    ('BANNER_CREATE', 'Create banner'),
    ('BANNER_DELETE', 'Delete banner'),
    ('BANNER_UPDATE', 'Update banner'),
    ('BRAND_CREATE', 'Create brand'),
    ('BRAND_DELETE', 'Delete brand'),
    ('BRAND_UPDATE', 'Update brand'),
    ('CART_ADD_ITEM', 'Add item to cart'),
    ('CART_GET_BY_ID', 'Get cart by ID'),
    ('CART_REMOVE_ITEM', 'Remove item from cart'),
    ('CART_UPDATE_ITEM', 'Update item in cart'),
    ('CATEGORY_CREATE', 'Create category'),
    ('CATEGORY_DELETE', 'Delete category'),
    ('CATEGORY_UPDATE', 'Update category'),
    ('IMAGE_DELETE', 'Delete image'),
    ('IMAGE_UPLOAD', 'Upload image'),
    ('NOTIFICATION_GET_MY', 'Get my notifications'),
    ('ORDER_CANCEL', 'Cancel order'),
    ('ORDER_CREATE', 'Create order'),
    ('ORDER_DELETE', 'Delete order'),
    ('ORDER_GET_ALL', 'Get all orders'),
    ('ORDER_GET_BY_ID', 'Get order by ID'),
    ('ORDER_GET_MY_ORDER', 'Get my orders'),
    ('ORDER_UPDATE_BY_ADMIN', 'Update order by admin'),
    ('ORDER_UPDATE_BY_USER', 'Update order by user'),
    ('PERMISSION_CREATE', 'Create permission'),
    ('PERMISSION_GET_ALL', 'Get all permissions'),
    ('PERMISSION_UPDATE', 'Update permission'),
    ('POST_CATEGORY_CREATE', 'Create post category'),
    ('POST_CATEGORY_DELETE', 'Delete post category'),
    ('POST_CATEGORY_UPDATE', 'Update post category'),
    ('POST_CREATE', 'Create post'),
    ('POST_DELETE', 'Delete post'),
    ('POST_GET_ALL', 'Get all posts'),
    ('POST_TOGGLE_PUBLISH', 'Toggle post publish status'),
    ('POST_UPDATE', 'Update post'),
    ('PRODUCT_CREATE', 'Create product'),
    ('PRODUCT_DELETE', 'Delete product'),
    ('PRODUCT_IMAGE_CREATE', 'Create product image'),
    ('PRODUCT_IMAGE_DELETE', 'Delete product image'),
    ('PRODUCT_IMAGE_SET_PRIMARY', 'Set primary image'),
    ('PRODUCT_IMAGE_UPDATE', 'Update product image'),
    ('PRODUCT_UPDATE', 'Update product'),
    ('ROLE_ADD_PERMISSIONS', 'Add permissions to role'),
    ('ROLE_CREATE', 'Create role'),
    ('ROLE_DELETE', 'Delete role'),
    ('ROLE_GET_ALL', 'Get all roles'),
    ('ROLE_GET_BY_ID', 'Get role by ID'),
    ('ROLE_REMOVE_PERMISSIONS', 'Remove permissions from role'),
    ('ROLE_UPDATE', 'Update role'),
    ('SERVICE_CREATE', 'Create professional service'),
    ('SERVICE_DELETE', 'Delete professional service'),
    ('SERVICE_IMAGE_CREATE', 'Create service image'),
    ('SERVICE_IMAGE_DELETE', 'Delete service image'),
    ('SERVICE_IMAGE_SET_PRIMARY', 'Set primary service image'),
    ('SERVICE_IMAGE_UPDATE', 'Update service image'),
    ('SERVICE_UPDATE', 'Update professional service'),
    ('USER_CREATE', 'Create user'),
    ('USER_DELETE', 'Delete user'),
    ('USER_GET_ALL', 'Get all users'),
    ('USER_GET_BY_ID', 'Get user by ID'),
    ('USER_UPDATE', 'Update user')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role (name, description) VALUES
    ('ADMIN', 'Administrator'),
    ('USER', 'User')
ON CONFLICT (name) DO NOTHING;

-- ADMIN: toàn quyền -> gán mọi permission đang có
INSERT INTO role_permissions (role_id, permissions_id)
SELECT 'ADMIN', name FROM permission
ON CONFLICT DO NOTHING;

-- USER: khách hàng, chỉ thao tác trên giỏ hàng / đơn của chính mình
INSERT INTO role_permissions (role_id, permissions_id) VALUES
    ('USER', 'CART_ADD_ITEM'),
    ('USER', 'CART_GET_BY_ID'),
    ('USER', 'CART_REMOVE_ITEM'),
    ('USER', 'CART_UPDATE_ITEM'),
    ('USER', 'NOTIFICATION_GET_MY'),
    ('USER', 'ORDER_CANCEL'),
    ('USER', 'ORDER_CREATE'),
    ('USER', 'ORDER_GET_MY_ORDER'),
    ('USER', 'ORDER_UPDATE_BY_USER')
ON CONFLICT DO NOTHING;
