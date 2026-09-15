package app.store.service;

import app.store.dto.response.PaymentResponse;
import app.store.entity.Order;
import app.store.entity.Payment;
import app.store.entity.PayosPaymentLink;
import app.store.enums.OrderStatus;
import app.store.enums.PaymentMethod;
import app.store.enums.PaymentStatus;
import app.store.enums.WebhookOutcome;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.OrderRepository;
import app.store.repository.PaymentRepository;
import app.store.repository.PayosPaymentLinkRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.v2.paymentRequests.Transaction;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentService {

    private static final String VIEW_ANY_ORDER_AUTHORITY = "ORDER_GET_BY_ID";
    private static final String GATEWAY_NAME = "PAYOS";
    private static final String SUCCESS_CODE = "00";
    private static final String ORDER_CANCELED_REASON = "Don hang da bi huy";

    /** Link sắp hết hạn trong khoảng này thì tạo link mới, tránh khách vừa mở trang đã hết hạn. */
    private static final Duration LINK_REUSE_MARGIN = Duration.ofMinutes(2);

    /** Trạng thái link trên payOS mà khách vẫn thanh toán tiếp được. */
    private static final Set<PaymentLinkStatus> OPEN_LINK_STATUSES =
            EnumSet.of(PaymentLinkStatus.PENDING, PaymentLinkStatus.PROCESSING);

    /**
     * payOS giới hạn description 9 ký tự với tài khoản ngân hàng không liên kết qua payOS.
     * "DH" + tối đa 7 chữ số.
     */
    private static final int DESCRIPTION_DIGITS = 7;

    OrderRepository orderRepository;
    PaymentRepository paymentRepository;
    PayosPaymentLinkRepository payosPaymentLinkRepository;
    PayosGateway payosGateway;

    @NonFinal
    @Value("${payos.return-url}")
    String returnUrl;

    @NonFinal
    @Value("${payos.cancel-url}")
    String cancelUrl;

    @NonFinal
    @Value("${payos.link-expiry-minutes}")
    long linkExpiryMinutes;

    // Tạo mã đơn hàng duy nhất: DH + yyyyMMdd + 8 ký tự UUID viết hoa
    public String generateOrderCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "DH" + dateStr + randomPart;
    }

    /**
     * Trả về link thanh toán payOS cho đơn. Dùng lại link gần nhất nếu nó vẫn thanh toán được,
     * ngược lại tạo link mới.
     */
    public PaymentResponse createPayment(String orderId) {
        Order order = findAccessibleOrder(orderId);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Đơn hàng đã được thanh toán");
        }

        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new RuntimeException("Đơn hàng không sử dụng phương thức chuyển khoản");
        }

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new RuntimeException("Đơn hàng đã bị huỷ");
        }

        Optional<PayosPaymentLink> latest = payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc(order.getId());
        if (latest.isPresent()) {
            PayosPaymentLink link = latest.get();
            PaymentLink remote = payosGateway.getLink(link.getPaymentLinkId());

            // Khách đã trả nhưng webhook chưa tới: ghi nhận ngay, không mở link mới để trả lần hai
            recordTransactions(order, link, remote);
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                return toResponse(order, link);
            }

            boolean open = OPEN_LINK_STATUSES.contains(remote.getStatus());
            if (open && link.getExpiresAt().isAfter(LocalDateTime.now().plus(LINK_REUSE_MARGIN))) {
                return toResponse(order, link);
            }
            if (open) {
                cancelQuietly(link, "Tao link thanh toan moi");
            }
        }

        return toResponse(order, openPaymentLink(order));
    }

    /**
     * Trạng thái thanh toán của đơn. Khi đơn còn UNPAID thì hỏi thẳng payOS để đối soát,
     * nên vẫn cập nhật được dù webhook đến chậm, bị lỡ hoặc chưa cấu hình (dev local).
     */
    public PaymentResponse checkPaymentStatus(String orderId) {
        Order order = findAccessibleOrder(orderId);
        Optional<PayosPaymentLink> latest = payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc(order.getId());

        if (order.getPaymentStatus() == PaymentStatus.UNPAID && latest.isPresent()) {
            try {
                PayosPaymentLink link = latest.get();
                recordTransactions(order, link, payosGateway.getLink(link.getPaymentLinkId()));
            } catch (AppException | DataAccessException e) {
                // Đối soát chỉ là đường phụ: lỗi thì vẫn trả trạng thái đang có trong DB.
                // DataAccessException xảy ra khi webhook ghi cùng giao dịch trước (unique reference_code).
                log.warn("Could not reconcile order {} with payOS: {}", order.getOrderCode(), e.getMessage());
            }
        }

        return toResponse(order, latest.orElse(null));
    }

    /** Xử lý webhook payOS. Chỉ gọi sau khi {@link PayosGateway#verifyWebhook} đã xác thực chữ ký. */
    @Transactional
    public WebhookOutcome handlePayosWebhook(WebhookData data) {
        log.info("=== payOS Webhook received === orderCode={}, amount={}, code={}, reference={}",
                data.getOrderCode(), data.getAmount(), data.getCode(), data.getReference());

        if (!SUCCESS_CODE.equals(data.getCode())) {
            log.info("Ignoring payOS webhook with code={}, desc={}", data.getCode(), data.getDesc());
            return WebhookOutcome.NOT_SUCCESS;
        }

        if (data.getOrderCode() == null || data.getAmount() == null || data.getReference() == null) {
            log.error("payOS webhook payload is missing orderCode/amount/reference, rejecting as final");
            return WebhookOutcome.INVALID_PAYLOAD;
        }

        PayosPaymentLink link = payosPaymentLinkRepository.findByPayosOrderCode(data.getOrderCode()).orElse(null);
        if (link == null) {
            // Vẫn trả 200: chữ ký hợp lệ nên đây là quyết định cuối cùng, gửi lại cũng không khớp được
            log.warn("No payment link matches payOS orderCode {}: reference={}, amount={}",
                    data.getOrderCode(), data.getReference(), data.getAmount());
            return WebhookOutcome.ORDER_NOT_FOUND;
        }

        return applyTransaction(link.getOrder(), new IncomingTransaction(
                data.getAmount(), data.getReference(), data.getPaymentLinkId(), data.getDescription(),
                data.getAccountNumber(), data.getTransactionDateTime()));
    }

    /** Huỷ link còn hiệu lực của đơn chuyển khoản chưa thanh toán. Lỗi chỉ ghi log. */
    public void cancelOpenPaymentLink(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER
                || order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                .filter(link -> link.getExpiresAt().isAfter(LocalDateTime.now()))
                .ifPresent(link -> cancelQuietly(link, ORDER_CANCELED_REASON));
    }

    private PayosPaymentLink openPaymentLink(Order order) {
        long payosOrderCode = payosPaymentLinkRepository.nextPayosOrderCode();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(linkExpiryMinutes));

        CreatePaymentLinkResponse created = payosGateway.createLink(CreatePaymentLinkRequest.builder()
                .orderCode(payosOrderCode)
                .amount(toVndAmount(order.getTotalPrice()))
                .description(buildDescription(payosOrderCode))
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .expiredAt(expiresAt.getEpochSecond())
                .build());

        log.info("Created payOS payment link for order {}: payosOrderCode={}, paymentLinkId={}",
                order.getOrderCode(), payosOrderCode, created.getPaymentLinkId());

        return payosPaymentLinkRepository.save(PayosPaymentLink.builder()
                .order(order)
                .payosOrderCode(payosOrderCode)
                .paymentLinkId(created.getPaymentLinkId())
                .checkoutUrl(created.getCheckoutUrl())
                .expiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()))
                .build());
    }

    private void recordTransactions(Order order, PayosPaymentLink link, PaymentLink remote) {
        if (remote.getTransactions() == null) {
            return;
        }

        for (Transaction tx : remote.getTransactions()) {
            WebhookOutcome outcome = applyTransaction(order, new IncomingTransaction(
                    tx.getAmount(), tx.getReference(), link.getPaymentLinkId(), tx.getDescription(),
                    tx.getAccountNumber(), tx.getTransactionDateTime()));
            if (outcome != WebhookOutcome.DUPLICATE_TRANSACTION) {
                log.info("Reconciled payOS transaction {} for order {}: {}",
                        tx.getReference(), order.getOrderCode(), outcome);
            }
        }
    }

    private WebhookOutcome applyTransaction(Order order, IncomingTransaction tx) {
        // Chống xử lý trùng: cùng một giao dịch đến từ cả webhook lẫn đối soát qua API, và một
        // webhook hợp lệ có thể bị gửi lại. Chữ ký payOS không kèm timestamp để chặn replay
        // nên đây là lớp chặn chính.
        if (paymentRepository.existsByReferenceCode(tx.reference())) {
            return WebhookOutcome.DUPLICATE_TRANSACTION;
        }

        BigDecimal amount = BigDecimal.valueOf(tx.amount());

        // Đơn đã PAID mà vẫn có tiền vào -> vẫn ghi lại để thấy được mà hoàn tiền
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.warn("Order {} is already PAID but received another transfer of {}: reference={}",
                    order.getOrderCode(), amount, tx.reference());
            savePaymentRecord(order, tx, PaymentStatus.PAID);
            return WebhookOutcome.ORDER_ALREADY_PAID;
        }

        if (amount.compareTo(order.getTotalPrice()) < 0) {
            log.warn("Transfer amount {} is less than order total {}, orderCode: {}",
                    amount, order.getTotalPrice(), order.getOrderCode());
            // Vẫn lưu giao dịch để đối soát nhưng không update trạng thái đơn
            savePaymentRecord(order, tx, PaymentStatus.UNPAID);
            return WebhookOutcome.UNDERPAID;
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);
        savePaymentRecord(order, tx, PaymentStatus.PAID);

        log.info("Order {} has been marked as PAID, amount={}", order.getOrderCode(), amount);
        return WebhookOutcome.PAID;
    }

    private void cancelQuietly(PayosPaymentLink link, String reason) {
        try {
            payosGateway.cancelLink(link.getPaymentLinkId(), reason);
        } catch (AppException e) {
            log.warn("Could not cancel payOS payment link {}", link.getPaymentLinkId());
        }
    }

    private Order findAccessibleOrder(String orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean canViewAnyOrder = authentication.getAuthorities().stream()
                .anyMatch(authority -> VIEW_ANY_ORDER_AUTHORITY.equals(authority.getAuthority()));

        Optional<Order> order = canViewAnyOrder
                ? orderRepository.findById(orderId)
                : orderRepository.findByIdAndUserUsername(orderId, authentication.getName());

        return order.orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_EXISTED));
    }

    private void savePaymentRecord(Order order, IncomingTransaction tx, PaymentStatus status) {
        Payment payment = Payment.builder()
                .order(order)
                .amount(BigDecimal.valueOf(tx.amount()))
                .gateway(GATEWAY_NAME)
                .transactionCode(tx.paymentLinkId())
                .referenceCode(tx.reference())
                .transferContent(tx.description())
                .accountNumber(tx.accountNumber())
                .transactionDate(tx.transactionDateTime())
                .status(status)
                .build();
        paymentRepository.save(payment);
    }

    private PaymentResponse toResponse(Order order, PayosPaymentLink link) {
        PaymentResponse.PaymentResponseBuilder response = PaymentResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .amount(order.getTotalPrice())
                .paymentStatus(order.getPaymentStatus());

        // Đã thanh toán thì không trả link nữa
        if (link != null && order.getPaymentStatus() != PaymentStatus.PAID) {
            response.checkoutUrl(link.getCheckoutUrl())
                    .paymentLinkId(link.getPaymentLinkId())
                    .expiredAt(link.getExpiresAt());
        }

        return response.build();
    }

    private static long toVndAmount(BigDecimal totalPrice) {
        try {
            return totalPrice.longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Tổng tiền đơn hàng phải là số nguyên VND: " + totalPrice);
        }
    }

    private static String buildDescription(long payosOrderCode) {
        String digits = String.valueOf(payosOrderCode);
        if (digits.length() > DESCRIPTION_DIGITS) {
            digits = digits.substring(digits.length() - DESCRIPTION_DIGITS);
        }
        return "DH" + digits;
    }

    /** Một giao dịch tiền vào, từ webhook hoặc từ API tra cứu link của payOS. */
    private record IncomingTransaction(Long amount, String reference, String paymentLinkId,
            String description, String accountNumber, String transactionDateTime) {
    }
}
