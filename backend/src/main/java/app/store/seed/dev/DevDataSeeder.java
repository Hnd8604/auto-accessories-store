package app.store.seed.dev;

import app.store.entity.*;
import app.store.enums.ConversationStatus;
import app.store.enums.NotificationType;
import app.store.enums.OrderStatus;
import app.store.enums.PaymentGateway;
import app.store.enums.PaymentMethod;
import app.store.enums.PaymentStatus;
import app.store.enums.SenderType;
import app.store.seed.dev.DevSeedData.*;
import app.store.utils.SlugUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Nạp dữ liệu mẫu để test giao diện: danh mục, thương hiệu, ~85 sản phẩm, khách hàng,
 * đơn hàng + thanh toán, thông báo, giỏ hàng, bài viết, dịch vụ, banner và live chat.
 *
 * <ul>
 *   <li>Chỉ chạy ở profile {@code dev}, tắt bằng {@code app.seed.enabled=false}.</li>
 *   <li>Chỉ chạy khi bảng {@code product} đang trống — restart app không bị nhân đôi dữ liệu.
 *       Muốn seed lại: xoá volume Postgres (hoặc {@code TRUNCATE}) rồi khởi động lại.</li>
 *   <li>Chạy ở {@link ApplicationReadyEvent}, tức là sau khi {@code ApplicationInitConfig}
 *       đã tạo tài khoản admin (dùng làm tác giả bài viết).</li>
 *   <li>Random cố định seed nên mỗi lần nạp lại ra cùng một bộ dữ liệu.</li>
 * </ul>
 *
 * <p>Ảnh dùng placehold.co. URL có đuôi {@code .png} vì
 * {@code CloudinaryService.extractPublicIdFromUrl} cần dấu chấm trong tên file, không
 * có thì xoá ảnh seed trong trang admin sẽ lỗi.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
@org.springframework.core.annotation.Order(10)
public class DevDataSeeder {

    /** Mật khẩu chung của mọi tài khoản khách hàng được seed. */
    static final String DEFAULT_PASSWORD = "12345678";

    private static final DateTimeFormatter ORDER_CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter PAYOS_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int ORDER_COUNT = 60;

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final SlugUtil slugUtil;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${payos.link-expiry-minutes:15}")
    private long payosLinkExpiryMinutes;

    private final Random random = new Random(20241103);
    private final LocalDateTime now = LocalDateTime.now().withNano(0);

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        Long productCount = jdbc.queryForObject("SELECT count(*) FROM product", Long.class);
        if (productCount != null && productCount > 0) {
            log.info("[DevDataSeeder] Đã có {} sản phẩm, bỏ qua seed dữ liệu mẫu", productCount);
            return;
        }
        log.info("[DevDataSeeder] Bắt đầu nạp dữ liệu mẫu...");

        Map<String, Category> categories = seedCategories();
        Map<String, Brand> brands = seedBrands();
        List<Product> products = seedProducts(categories, brands);
        seedCategoryBrandLinks(categories, brands);

        List<User> customers = seedCustomers();
        List<Order> orders = seedOrders(customers, products);
        seedCartItems(customers, products);

        seedPosts();
        seedServices();
        seedBanners(products);
        seedChats();

        log.info("[DevDataSeeder] Xong: {} danh mục, {} thương hiệu, {} sản phẩm, {} khách hàng, {} đơn hàng. "
                        + "Đăng nhập khách hàng bằng email (vd. '{}') / mật khẩu '{}'",
                categories.size(), brands.size(), products.size(), customers.size(), orders.size(),
                customers.isEmpty() ? "-" : customers.get(0).getEmail(), DEFAULT_PASSWORD);
    }

    // ------------------------------------------------------------------
    // Danh mục, thương hiệu, sản phẩm
    // ------------------------------------------------------------------

    private Map<String, Category> seedCategories() {
        Map<String, Category> result = new LinkedHashMap<>();
        for (CategorySeed s : DevSeedData.CATEGORIES) {
            Category c = Category.builder()
                    .name(s.name())
                    .description(s.description())
                    .slug(slugUtil.toSlug(s.name()))
                    .brands(new HashSet<>())
                    .build();
            em.persist(c);
            result.put(s.key(), c);
        }
        return result;
    }

    private Map<String, Brand> seedBrands() {
        Map<String, Brand> result = new LinkedHashMap<>();
        for (BrandSeed s : DevSeedData.BRANDS) {
            Brand b = Brand.builder()
                    .name(s.name())
                    .description(s.description())
                    .slug(slugUtil.toSlug(s.name()))
                    .build();
            em.persist(b);
            result.put(s.key(), b);
        }
        return result;
    }

    private List<Product> seedProducts(Map<String, Category> categories, Map<String, Brand> brands) {
        Map<String, String> colorByCategory = new HashMap<>();
        DevSeedData.CATEGORIES.forEach(c -> colorByCategory.put(c.key(), c.color()));

        List<Product> result = new ArrayList<>();
        for (ProductSeed s : DevSeedData.PRODUCTS) {
            Brand brand = brands.get(s.brand());
            Product p = new Product();
            p.setName(s.name());
            p.setSlug(slugUtil.toSlug(s.name()));
            p.setCategory(categories.get(s.category()));
            p.setBrand(brand);
            p.setUnitPrice(BigDecimal.valueOf(s.price()));
            p.setStockQuantity(randomStock());
            p.setDescription(productDescription(s, brand));

            // 1 ảnh chính + 1-2 ảnh phụ
            List<ProductImage> images = new ArrayList<>();
            int imageCount = 2 + random.nextInt(2);
            for (int i = 0; i < imageCount; i++) {
                ProductImage img = new ProductImage();
                img.setProduct(p);
                img.setImageUrl(placeholder(800, 800, colorByCategory.get(s.category()),
                        brand.getName(), i == 0 ? s.name() : s.name() + " - " + (i + 1)));
                img.setAltText(i == 0 ? s.name() : s.name() + " - ảnh " + (i + 1));
                img.setIsPrimary(i == 0);
                img.setSortOrder(i);
                images.add(img);
            }
            p.setProductImages(images);

            em.persist(p);
            result.add(p);
        }
        em.flush();
        // Rải ngày tạo trong 6 tháng để sắp xếp "mới nhất" có ý nghĩa
        for (Product p : result) {
            backdate("product", p.getId(), now.minusDays(random.nextInt(180)).minusMinutes(random.nextInt(1440)));
        }
        return result;
    }

    /** Hầu hết còn hàng, một ít sắp hết và hết hàng để test hiển thị. */
    private int randomStock() {
        int roll = random.nextInt(100);
        if (roll < 6) return 0;
        if (roll < 16) return 1 + random.nextInt(4);
        return 10 + random.nextInt(140);
    }

    private String productDescription(ProductSeed s, Brand brand) {
        return s.highlight() + "\n\n"
                + "Sản phẩm chính hãng " + brand.getName() + ", nguyên seal, đầy đủ phụ kiện đi kèm.\n\n"
                + "- Bảo hành chính hãng 12 tháng, 1 đổi 1 trong 7 ngày nếu lỗi do nhà sản xuất\n"
                + "- Hỗ trợ lắp đặt tại cửa hàng hoặc tận nơi\n"
                + "- Giao hàng toàn quốc, kiểm tra hàng trước khi thanh toán";
    }

    /** category_brand: brand nào có sản phẩm trong danh mục nào thì gắn vào danh mục đó. */
    private void seedCategoryBrandLinks(Map<String, Category> categories, Map<String, Brand> brands) {
        for (ProductSeed s : DevSeedData.PRODUCTS) {
            categories.get(s.category()).getBrands().add(brands.get(s.brand()));
        }
    }

    // ------------------------------------------------------------------
    // Khách hàng, đơn hàng, thanh toán, thông báo, giỏ hàng
    // ------------------------------------------------------------------

    private List<User> seedCustomers() {
        Role userRole = em.find(Role.class, "USER");
        if (userRole == null) {
            throw new IllegalStateException("Thiếu role USER — Flyway V2 chưa chạy?");
        }
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

        List<User> result = new ArrayList<>();
        for (CustomerSeed s : DevSeedData.CUSTOMERS) {
            if (jdbc.queryForObject("SELECT count(*) FROM users WHERE username = ?", Long.class, s.username()) > 0) {
                continue;
            }
            User u = User.builder()
                    .username(s.username())
                    .password(encodedPassword)
                    .email(s.username() + "@example.com")
                    .fullName(s.fullName())
                    .phoneNumber(s.phone())
                    .roles(new HashSet<>(Set.of(userRole)))
                    .build();
            Cart cart = Cart.builder().user(u).cartItems(new ArrayList<>()).build();
            u.setCart(cart);
            em.persist(u);
            result.add(u);
        }
        em.flush();

        for (User u : result) {
            LocalDateTime joined = now.minusDays(100 + random.nextInt(80));
            backdate("users", u.getId(), joined);
            addNotification(u, "Chào mừng bạn đến với cửa hàng",
                    "Cảm ơn " + u.getFullName() + " đã đăng ký tài khoản. Khám phá ngay các ưu đãi dành cho thành viên mới!",
                    NotificationType.SYSTEM, null, joined, true);
        }
        return result;
    }

    private List<Order> seedOrders(List<User> customers, List<Product> products) {
        if (customers.isEmpty()) return List.of();
        List<Order> result = new ArrayList<>();
        NumberFormat vnd = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        for (int i = 0; i < ORDER_COUNT; i++) {
            User user = customers.get(random.nextInt(customers.size()));
            // Đơn gần đây dày hơn đơn cũ
            int daysAgo = (int) Math.floor(Math.pow(random.nextDouble(), 1.6) * 90);
            LocalDateTime createdAt = now.minusDays(daysAgo).minusMinutes(random.nextInt(600));
            if (createdAt.isAfter(now)) createdAt = now.minusMinutes(5);

            OrderStatus status = pickStatus(daysAgo);
            PaymentMethod method = random.nextInt(100) < 45 ? PaymentMethod.BANK_TRANSFER : PaymentMethod.COD;
            PaymentStatus paymentStatus = pickPaymentStatus(status, method);

            Order order = new Order();
            order.setUser(user);
            order.setNameRecipient(user.getFullName());
            order.setPhoneRecipient(user.getPhoneNumber());
            order.setAddressRecipient(pick(DevSeedData.ADDRESSES));
            order.setNote(random.nextInt(100) < 35 ? pick(DevSeedData.ORDER_NOTES) : null);
            order.setOrderCode("DH" + createdAt.format(ORDER_CODE_DATE) + randomHex(8));
            order.setStatus(status);
            order.setPaymentMethod(method);
            order.setPaymentStatus(paymentStatus);

            // 1-4 sản phẩm khác nhau, sản phẩm rẻ thì hay mua nhiều hơn
            List<Product> shuffled = new ArrayList<>(products);
            Collections.shuffle(shuffled, random);
            int lineCount = 1 + (int) Math.floor(Math.pow(random.nextDouble(), 2) * 4);
            List<OrderDetail> details = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Product p : shuffled.subList(0, lineCount)) {
                int qty = p.getUnitPrice().compareTo(BigDecimal.valueOf(1_000_000)) < 0 ? 1 + random.nextInt(3) : 1;
                OrderDetail d = new OrderDetail();
                d.setOrder(order);
                d.setProduct(p);
                d.setQuantity(qty);
                details.add(d);
                total = total.add(p.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
            }
            order.setOrderDetails(details);
            order.setTotalPrice(total);
            em.persist(order);

            // Đơn chuyển khoản đã trả tiền: giả lập đúng dữ liệu PayOS để lại sau khi thanh toán
            // (1 link thanh toán + 1 giao dịch, giống PaymentService/PaymentRecorder ghi).
            Payment payment = null;
            PayosPaymentLink link = null;
            if (method == PaymentMethod.BANK_TRANSFER && paymentStatus != PaymentStatus.UNPAID) {
                Long payosOrderCode = jdbc.queryForObject("SELECT nextval('payos_order_code_seq')", Long.class);
                String paymentLinkId = randomHex(32).toLowerCase();
                link = PayosPaymentLink.builder()
                        .order(order)
                        .payosOrderCode(payosOrderCode)
                        .paymentLinkId(paymentLinkId)
                        .checkoutUrl("https://pay.payos.vn/web/" + paymentLinkId)
                        .expiresAt(createdAt.plusMinutes(payosLinkExpiryMinutes))
                        .build();
                em.persist(link);

                LocalDateTime paidAt = createdAt.plusMinutes(1 + random.nextInt(10));
                payment = Payment.builder()
                        .order(order)
                        .amount(total)
                        .gateway(PaymentGateway.PAYOS)
                        .transactionCode(paymentLinkId)
                        .referenceCode("FT" + (10_000_000_000L + (long) (random.nextDouble() * 89_999_999_999L)))
                        .transferContent(payosDescription(payosOrderCode))
                        .accountNumber("0001234567890")
                        .transactionDate(paidAt.format(PAYOS_DATE))
                        .status(paymentStatus)
                        .build();
                em.persist(payment);
            }
            em.flush();

            backdate("orders", order.getId(), createdAt);
            for (OrderDetail d : details) backdate("order_detail", d.getId(), createdAt);
            if (link != null) backdate("payos_payment_links", link.getId(), createdAt);
            if (payment != null) backdate("payments", payment.getId(), createdAt.plusMinutes(5));

            // Thông báo giống OrderNotificationConsumer tạo ra
            boolean read = daysAgo > 5;
            addNotification(user, "Đặt hàng thành công",
                    "Đơn hàng #" + order.getOrderCode() + " đã được đặt thành công. Tổng tiền: " + vnd.format(total),
                    NotificationType.ORDER_CREATED, order.getId(), createdAt, read);
            if (payment != null) {
                addNotification(user, "Thanh toán thành công",
                        "Đã nhận " + vnd.format(total) + " cho đơn hàng #" + order.getOrderCode(),
                        NotificationType.PAYMENT_RECEIVED, order.getId(), createdAt.plusMinutes(5), read);
            }
            if (status != OrderStatus.PENDING) {
                OrderStatus previous = switch (status) {
                    case PROCESSING, CANCELED -> OrderStatus.PENDING;
                    case SHIPPED -> OrderStatus.PROCESSING;
                    case DELIVERED -> OrderStatus.SHIPPED;
                    default -> OrderStatus.PENDING;
                };
                addNotification(user, "Cập nhật đơn hàng #" + order.getOrderCode(),
                        "Đơn hàng #" + order.getOrderCode() + " đã chuyển trạng thái từ " + previous + " sang " + status,
                        status == OrderStatus.CANCELED ? NotificationType.ORDER_CANCELED : NotificationType.ORDER_STATUS_CHANGED,
                        order.getId(), createdAt.plusHours(4 + random.nextInt(48)), read && random.nextBoolean());
            }
            result.add(order);
        }
        return result;
    }

    /** Đơn càng cũ càng có khả năng đã giao xong; đơn mới thì đang chờ/xử lý. */
    private OrderStatus pickStatus(int daysAgo) {
        int roll = random.nextInt(100);
        if (daysAgo <= 2) {
            return roll < 65 ? OrderStatus.PENDING : roll < 90 ? OrderStatus.PROCESSING : OrderStatus.CANCELED;
        }
        if (daysAgo <= 7) {
            return roll < 15 ? OrderStatus.PENDING : roll < 40 ? OrderStatus.PROCESSING
                    : roll < 80 ? OrderStatus.SHIPPED : roll < 90 ? OrderStatus.DELIVERED : OrderStatus.CANCELED;
        }
        return roll < 82 ? OrderStatus.DELIVERED : OrderStatus.CANCELED;
    }

    private PaymentStatus pickPaymentStatus(OrderStatus status, PaymentMethod method) {
        if (method == PaymentMethod.COD) {
            return status == OrderStatus.DELIVERED ? PaymentStatus.PAID : PaymentStatus.UNPAID;
        }
        return switch (status) {
            case PENDING -> random.nextBoolean() ? PaymentStatus.PAID : PaymentStatus.UNPAID;
            case CANCELED -> random.nextBoolean() ? PaymentStatus.REFUNDED : PaymentStatus.UNPAID;
            default -> PaymentStatus.PAID;
        };
    }

    private void seedCartItems(List<User> customers, List<Product> products) {
        // Vài khách đang có sẵn hàng trong giỏ
        for (User u : customers.subList(0, Math.min(5, customers.size()))) {
            List<Product> shuffled = new ArrayList<>(products);
            Collections.shuffle(shuffled, random);
            for (Product p : shuffled.subList(0, 1 + random.nextInt(4))) {
                if (p.getStockQuantity() == 0) continue;
                CartItem item = CartItem.builder()
                        .cart(u.getCart())
                        .product(p)
                        .quantity(1 + random.nextInt(Math.min(2, p.getStockQuantity())))
                        .build();
                u.getCart().getCartItems().add(item);
                em.persist(item);
            }
        }
    }

    private void addNotification(User user, String title, String message, NotificationType type,
                                 String referenceId, LocalDateTime createdAt, boolean read) {
        if (createdAt.isAfter(now)) createdAt = now;
        Notification n = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .isRead(read)
                .build();
        em.persist(n);
        em.flush();
        backdate("notifications", n.getId(), createdAt);
    }

    // ------------------------------------------------------------------
    // Nội dung: bài viết, dịch vụ, banner, live chat
    // ------------------------------------------------------------------

    private void seedPosts() {
        User author = em.createQuery("SELECT u FROM User u WHERE u.username = :u", User.class)
                .setParameter("u", adminUsername)
                .getResultStream().findFirst().orElse(null);

        Map<String, PostCategory> categories = new HashMap<>();
        for (String[] c : DevSeedData.POST_CATEGORIES) {
            PostCategory pc = PostCategory.builder()
                    .name(c[0])
                    .slug(slugUtil.toSlug(c[0]))
                    .description(c[1])
                    .build();
            em.persist(pc);
            categories.put(c[0], pc);
        }

        List<PostSeed> seeds = DevSeedData.POSTS;
        for (int i = 0; i < seeds.size(); i++) {
            PostSeed s = seeds.get(i);
            Post post = Post.builder()
                    .title(s.title())
                    .slug(slugUtil.toSlug(s.title()))
                    .shortDescription(s.shortDescription())
                    .content(s.content())
                    .thumbnailUrl(placeholder(1200, 630, "334155", s.category(), s.title()))
                    .published(s.published())
                    .viewCount(s.published() ? 50L + random.nextInt(3000) : 0L)
                    .category(categories.get(s.category()))
                    .author(author)
                    .build();
            em.persist(post);
            em.flush();
            // Bài đầu danh sách là mới nhất
            backdate("posts", post.getId(), now.minusDays(i * 6L + random.nextInt(4)).minusHours(random.nextInt(12)));
        }
    }

    private void seedServices() {
        List<ServiceSeed> seeds = DevSeedData.SERVICES;
        for (int i = 0; i < seeds.size(); i++) {
            ServiceSeed s = seeds.get(i);
            ProfessionalService service = new ProfessionalService();
            service.setName(s.name());
            service.setSlug(slugUtil.toSlug(s.name()));
            service.setShortDescription(s.shortDescription());
            // Entity không có cột features riêng nên ghép vào cuối mô tả
            StringBuilder full = new StringBuilder(s.fullDescription()).append("\n");
            s.features().forEach(f -> full.append("\n✓ ").append(f));
            service.setFullDescription(full.toString());
            service.setDisplayOrder(i + 1);

            List<ServiceImage> images = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                ServiceImage img = new ServiceImage();
                img.setService(service);
                img.setImageUrl(placeholder(1200, 800, "0f766e", "Dịch vụ", s.name() + (j == 0 ? "" : " - " + (j + 1))));
                img.setAltText(s.name() + " - ảnh " + (j + 1));
                img.setIsPrimary(j == 0);
                img.setSortOrder(j);
                images.add(img);
            }
            service.setImages(images);
            em.persist(service);
        }
    }

    private void seedBanners(List<Product> products) {
        Map<String, String> slugByName = new HashMap<>();
        products.forEach(p -> slugByName.put(p.getName(), p.getSlug()));

        List<BannerSeed> seeds = DevSeedData.BANNERS;
        for (int i = 0; i < seeds.size(); i++) {
            BannerSeed s = seeds.get(i);
            String redirect = s.redirectUrl();
            if (redirect.startsWith("product:")) {
                redirect = "/products/" + slugByName.get(redirect.substring("product:".length()));
            }
            em.persist(Banner.builder()
                    .title(s.title())
                    .subtitle(s.subtitle())
                    .imageUrl(placeholder(1920, 640, s.color(), s.title(), s.subtitle()))
                    .redirectUrl(redirect)
                    .altText(s.title())
                    .buttonText(s.buttonText())
                    .displayOrder(i + 1)
                    .isActive(true)
                    .build());
        }
    }

    private void seedChats() {
        for (ChatSeed s : DevSeedData.CHATS) {
            LocalDateTime start = now.minusHours(1 + random.nextInt(72));
            Conversation c = Conversation.builder()
                    .guestName(s.guestName())
                    .status(ConversationStatus.valueOf(s.status()))
                    .build();
            em.persist(c);
            em.flush();

            LocalDateTime at = start;
            int unread = 0;
            List<String> messageIds = new ArrayList<>();
            List<LocalDateTime> times = new ArrayList<>();
            for (String[] m : s.messages()) {
                ChatMessage msg = ChatMessage.builder()
                        .conversationId(c.getId())
                        .senderType(SenderType.valueOf(m[0]))
                        .content(m[1])
                        .build();
                em.persist(msg);
                messageIds.add(msg.getId());
                times.add(at);
                // Đếm tin khách gửi sau tin trả lời cuối cùng của admin
                unread = "ADMIN".equals(m[0]) ? 0 : unread + 1;
                at = at.plusMinutes(1 + random.nextInt(15));
            }
            LocalDateTime last = times.get(times.size() - 1);
            c.setLastMessageAt(last);
            c.setUnreadCount("OPEN".equals(s.status()) ? unread : 0);
            em.flush();

            backdate("conversations", c.getId(), start);
            for (int i = 0; i < messageIds.size(); i++) {
                jdbc.update("UPDATE chat_messages SET created_at = ? WHERE id = ?",
                        Timestamp.valueOf(times.get(i)), messageIds.get(i));
            }
        }
    }

    // ------------------------------------------------------------------
    // Tiện ích
    // ------------------------------------------------------------------

    /**
     * JPA auditing luôn ghi đè createdAt = thời điểm hiện tại khi persist,
     * nên phải sửa lại bằng SQL sau khi flush.
     */
    private void backdate(String table, Object id, LocalDateTime createdAt) {
        Timestamp ts = Timestamp.valueOf(createdAt);
        jdbc.update("UPDATE " + table + " SET created_at = ?, updated_at = ? WHERE id = ?", ts, ts, id);
    }

    /** Ảnh placeholder có chữ (bỏ dấu tiếng Việt vì font của placehold.co không chắc hỗ trợ). */
    private String placeholder(int w, int h, String bgColor, String line1, String line2) {
        String text = toAscii(line1) + "\\n" + toAscii(line2);
        return "https://placehold.co/" + w + "x" + h + "/" + bgColor + "/ffffff.png?font=roboto&text="
                + URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private String toAscii(String s) {
        // Tận dụng SlugUtil để bỏ dấu, rồi đổi '-' lại thành khoảng trắng và viết hoa chữ đầu
        String[] words = slugUtil.toSlug(s).split("-");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    /** Giống PaymentService.buildDescription: "DH" + tối đa 7 chữ số cuối của orderCode PayOS. */
    private String payosDescription(long payosOrderCode) {
        String digits = String.valueOf(payosOrderCode);
        return "DH" + (digits.length() > 7 ? digits.substring(digits.length() - 7) : digits);
    }

    private String randomHex(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append("0123456789ABCDEF".charAt(random.nextInt(16)));
        return sb.toString();
    }

    private <T> T pick(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}
