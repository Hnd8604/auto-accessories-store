package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

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
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.v2.paymentRequests.Transaction;
import vn.payos.model.webhooks.WebhookData;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    PaymentRepository paymentRepository;
    @Mock
    PayosPaymentLinkRepository payosPaymentLinkRepository;
    @Mock
    PayosGateway payosGateway;
    @InjectMocks
    PaymentService paymentService;

    private static final String ORDER_CODE = "DH20240115A1B2C3D4";
    private static final long PAYOS_ORDER_CODE = 42L;
    private static final String PAYMENT_LINK_ID = "link-42";
    private static final String CHECKOUT_URL = "https://pay.payos.vn/web/link-42";
    private static final String REFERENCE_CODE = "FT24015ABCDE";
    private static final String OWNER = "u1"; // user.id — principal name là sub = user.id
    private static final String RETURN_URL = "https://shop.test/";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "returnUrl", RETURN_URL);
        ReflectionTestUtils.setField(paymentService, "cancelUrl", RETURN_URL);
        ReflectionTestUtils.setField(paymentService, "linkExpiryMinutes", 15L);
        authenticateAs(OWNER, "ORDER_GET_MY_ORDER");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String userId, String... authorities) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(userId, null, authorities));
    }

    private Order buildOrder(PaymentStatus status, PaymentMethod method) {
        Order order = new Order();
        order.setId("o1");
        order.setOrderCode(ORDER_CODE);
        order.setTotalPrice(new BigDecimal("500000.00")); // numeric(38,2) như trong DB
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(status);
        order.setPaymentMethod(method);
        return order;
    }

    private PayosPaymentLink buildLink(Order order, LocalDateTime expiresAt) {
        return PayosPaymentLink.builder()
                .order(order)
                .payosOrderCode(PAYOS_ORDER_CODE)
                .paymentLinkId(PAYMENT_LINK_ID)
                .checkoutUrl(CHECKOUT_URL)
                .expiresAt(expiresAt)
                .build();
    }

    private static PaymentLink remoteLink(PaymentLinkStatus status, List<Transaction> transactions) {
        return PaymentLink.builder()
                .id(PAYMENT_LINK_ID)
                .orderCode(PAYOS_ORDER_CODE)
                .amount(500_000L)
                .amountPaid(0L)
                .amountRemaining(500_000L)
                .status(status)
                .createdAt("2026-09-13T10:00:00+07:00")
                .transactions(transactions)
                .build();
    }

    private static Transaction transaction(long amount) {
        return Transaction.builder()
                .reference(REFERENCE_CODE)
                .amount(amount)
                .accountNumber("113366668888")
                .description("DH42")
                .transactionDateTime("2026-09-13 10:05:00")
                .build();
    }

    private void givenOwnOrder(Order order) {
        when(orderRepository.findByIdAndUserId("o1", OWNER)).thenReturn(Optional.of(order));
    }

    private void givenPayosCreatesLink(long payosOrderCode, String paymentLinkId, String checkoutUrl) {
        when(payosPaymentLinkRepository.nextPayosOrderCode()).thenReturn(payosOrderCode);
        when(payosGateway.createLink(any())).thenReturn(CreatePaymentLinkResponse.builder()
                .bin("970422")
                .accountNumber("113366668888")
                .accountName("NGUYEN VAN A")
                .amount(500_000L)
                .description("DH" + payosOrderCode)
                .orderCode(payosOrderCode)
                .currency("VND")
                .paymentLinkId(paymentLinkId)
                .status(PaymentLinkStatus.PENDING)
                .checkoutUrl(checkoutUrl)
                .qrCode("000201010212")
                .build());
        when(payosPaymentLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================== generateOrderCode ====================

    @Test
    void generateOrderCode_shouldMatchPattern_andBeUnique() {
        String code1 = paymentService.generateOrderCode();
        String code2 = paymentService.generateOrderCode();

        assertThat(code1).matches("DH\\d{8}[A-Z0-9]{8}");
        assertThat(code1).isNotEqualTo(code2);
    }

    // ==================== createPayment ====================

    @Test
    void createPayment_shouldCreatePayosLink_whenOrderHasNoLink() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        givenOwnOrder(order);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1")).thenReturn(Optional.empty());
        givenPayosCreatesLink(PAYOS_ORDER_CODE, PAYMENT_LINK_ID, CHECKOUT_URL);

        var response = paymentService.createPayment("o1");

        ArgumentCaptor<CreatePaymentLinkRequest> request = ArgumentCaptor.forClass(CreatePaymentLinkRequest.class);
        verify(payosGateway).createLink(request.capture());
        assertThat(request.getValue().getOrderCode()).isEqualTo(PAYOS_ORDER_CODE);
        assertThat(request.getValue().getAmount()).isEqualTo(500_000L); // 500000.00 -> số nguyên
        assertThat(request.getValue().getDescription()).isEqualTo("DH42").hasSizeLessThanOrEqualTo(9);
        assertThat(request.getValue().getReturnUrl()).isEqualTo(RETURN_URL);
        assertThat(request.getValue().getCancelUrl()).isEqualTo(RETURN_URL);
        assertThat(request.getValue().getExpiredAt())
                .isCloseTo(Instant.now().plus(15, ChronoUnit.MINUTES).getEpochSecond(), within(5L));

        ArgumentCaptor<PayosPaymentLink> saved = ArgumentCaptor.forClass(PayosPaymentLink.class);
        verify(payosPaymentLinkRepository).save(saved.capture());
        assertThat(saved.getValue().getOrder()).isSameAs(order);
        assertThat(saved.getValue().getPayosOrderCode()).isEqualTo(PAYOS_ORDER_CODE);
        assertThat(saved.getValue().getPaymentLinkId()).isEqualTo(PAYMENT_LINK_ID);

        assertThat(response.checkoutUrl()).isEqualTo(CHECKOUT_URL);
        assertThat(response.paymentLinkId()).isEqualTo(PAYMENT_LINK_ID);
        assertThat(response.expiredAt()).isNotNull();
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void createPayment_shouldKeepDescriptionWithinPayosLimit_whenOrderCodeIsLong() {
        givenOwnOrder(buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER));
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1")).thenReturn(Optional.empty());
        givenPayosCreatesLink(123_456_789L, PAYMENT_LINK_ID, CHECKOUT_URL);

        paymentService.createPayment("o1");

        ArgumentCaptor<CreatePaymentLinkRequest> request = ArgumentCaptor.forClass(CreatePaymentLinkRequest.class);
        verify(payosGateway).createLink(request.capture());
        assertThat(request.getValue().getDescription()).isEqualTo("DH3456789");
    }

    @Test
    void createPayment_shouldReuseLatestLink_whenStillPending() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        givenOwnOrder(order);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1"))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().plusMinutes(10))));
        when(payosGateway.getLink(PAYMENT_LINK_ID)).thenReturn(remoteLink(PaymentLinkStatus.PENDING, List.of()));

        var response = paymentService.createPayment("o1");

        assertThat(response.checkoutUrl()).isEqualTo(CHECKOUT_URL);
        verify(payosGateway, never()).createLink(any());
        verify(payosGateway, never()).cancelLink(anyString(), anyString());
    }

    @Test
    void createPayment_shouldOpenNewLink_whenLatestLinkWasCancelledOnPayos() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        givenOwnOrder(order);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1"))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().plusMinutes(10))));
        when(payosGateway.getLink(PAYMENT_LINK_ID)).thenReturn(remoteLink(PaymentLinkStatus.CANCELLED, List.of()));
        givenPayosCreatesLink(43L, "link-43", "https://pay.payos.vn/web/link-43");

        var response = paymentService.createPayment("o1");

        assertThat(response.checkoutUrl()).isEqualTo("https://pay.payos.vn/web/link-43");
        verify(payosGateway, never()).cancelLink(anyString(), anyString());
    }

    @Test
    void createPayment_shouldCancelAndReplaceLink_whenAboutToExpire() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        givenOwnOrder(order);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1"))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().plusSeconds(30))));
        when(payosGateway.getLink(PAYMENT_LINK_ID)).thenReturn(remoteLink(PaymentLinkStatus.PENDING, List.of()));
        givenPayosCreatesLink(43L, "link-43", "https://pay.payos.vn/web/link-43");

        var response = paymentService.createPayment("o1");

        // Link cũ phải bị huỷ để khách không trả được vào cả hai link
        verify(payosGateway).cancelLink(eq(PAYMENT_LINK_ID), anyString());
        assertThat(response.paymentLinkId()).isEqualTo("link-43");
    }

    @Test
    void createPayment_shouldRecordPayment_andNotOpenNewLink_whenPayosAlreadyPaid() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        givenOwnOrder(order);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1"))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().minusMinutes(1))));
        when(payosGateway.getLink(PAYMENT_LINK_ID))
                .thenReturn(remoteLink(PaymentLinkStatus.PAID, List.of(transaction(500_000L))));

        var response = paymentService.createPayment("o1");

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.checkoutUrl()).isNull();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository).save(any(Payment.class));
        verify(payosGateway, never()).createLink(any());
    }

    @Test
    void createPayment_shouldReject_whenTotalHasFractionalVnd() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        order.setTotalPrice(new BigDecimal("500000.50"));
        givenOwnOrder(order);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1")).thenReturn(Optional.empty());
        when(payosPaymentLinkRepository.nextPayosOrderCode()).thenReturn(PAYOS_ORDER_CODE);

        assertThatThrownBy(() -> paymentService.createPayment("o1"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(payosGateway, never()).createLink(any());
    }

    @Test
    void createPayment_shouldThrow_whenOrderNotFound() {
        when(orderRepository.findByIdAndUserId("missing", OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment("missing"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_EXISTED);
    }

    @Test
    void createPayment_shouldHideOrder_whenCallerIsNotOwner() {
        authenticateAs("mallory-id", "ORDER_GET_MY_ORDER");
        when(orderRepository.findByIdAndUserId("o1", "mallory-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment("o1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_EXISTED);
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void createPayment_shouldThrow_whenAlreadyPaid() {
        givenOwnOrder(buildOrder(PaymentStatus.PAID, PaymentMethod.BANK_TRANSFER));

        assertThatThrownBy(() -> paymentService.createPayment("o1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã được thanh toán");
        verify(payosGateway, never()).createLink(any());
    }

    @Test
    void createPayment_shouldThrow_whenNotBankTransfer() {
        givenOwnOrder(buildOrder(PaymentStatus.UNPAID, PaymentMethod.COD));

        assertThatThrownBy(() -> paymentService.createPayment("o1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chuyển khoản");
    }

    @Test
    void createPayment_shouldThrow_whenOrderCanceled() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        order.setStatus(OrderStatus.CANCELED);
        givenOwnOrder(order);

        assertThatThrownBy(() -> paymentService.createPayment("o1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("huỷ");
        verify(payosGateway, never()).createLink(any());
    }

    // ==================== checkPaymentStatus ====================

    @Test
    void checkPaymentStatus_shouldReturnLink_whenUnpaidOnPayos() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        givenOwnOrder(order);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1"))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().plusMinutes(10))));
        when(payosGateway.getLink(PAYMENT_LINK_ID)).thenReturn(remoteLink(PaymentLinkStatus.PENDING, List.of()));

        var response = paymentService.checkPaymentStatus("o1");

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(response.checkoutUrl()).isEqualTo(CHECKOUT_URL);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void checkPaymentStatus_shouldMarkPaid_whenPayosReportsTransactionButWebhookMissing() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        givenOwnOrder(order);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1"))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().plusMinutes(10))));
        when(payosGateway.getLink(PAYMENT_LINK_ID))
                .thenReturn(remoteLink(PaymentLinkStatus.PAID, List.of(transaction(500_000L))));

        var response = paymentService.checkPaymentStatus("o1");

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.checkoutUrl()).isNull();
        verify(orderRepository).save(order);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getReferenceCode()).isEqualTo(REFERENCE_CODE);
        assertThat(captor.getValue().getGateway()).isEqualTo("PAYOS");
        assertThat(captor.getValue().getTransactionCode()).isEqualTo(PAYMENT_LINK_ID);
    }

    @Test
    void checkPaymentStatus_shouldReturnDbStatus_whenPayosUnavailable() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        givenOwnOrder(order);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1"))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().plusMinutes(10))));
        when(payosGateway.getLink(PAYMENT_LINK_ID)).thenThrow(new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR));

        var response = paymentService.checkPaymentStatus("o1");

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void checkPaymentStatus_shouldNotCallPayos_whenAlreadyPaid() {
        Order order = buildOrder(PaymentStatus.PAID, PaymentMethod.BANK_TRANSFER);
        givenOwnOrder(order);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1"))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().plusMinutes(10))));

        var response = paymentService.checkPaymentStatus("o1");

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.checkoutUrl()).isNull();
        verify(payosGateway, never()).getLink(anyString());
    }

    @Test
    void checkPaymentStatus_shouldHideOrder_whenCallerIsNotOwner() {
        authenticateAs("mallory-id", "ORDER_GET_MY_ORDER");
        when(orderRepository.findByIdAndUserId("o1", "mallory-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.checkPaymentStatus("o1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_EXISTED);
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void checkPaymentStatus_shouldAllowAnyOrder_whenCallerHasOrderGetById() {
        authenticateAs("admin", "ROLE_ADMIN", "ORDER_GET_BY_ID");
        when(orderRepository.findById("o1"))
                .thenReturn(Optional.of(buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER)));
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1")).thenReturn(Optional.empty());

        var response = paymentService.checkPaymentStatus("o1");

        assertThat(response.orderId()).isEqualTo("o1");
        verify(orderRepository, never()).findByIdAndUserId(any(), any());
    }

    // ==================== handlePayosWebhook ====================

    private static WebhookData webhook(String code, long amount) {
        return WebhookData.builder()
                .orderCode(PAYOS_ORDER_CODE)
                .amount(amount)
                .description("DH42")
                .accountNumber("113366668888")
                .reference(REFERENCE_CODE)
                .transactionDateTime("2026-09-13 10:05:00")
                .currency("VND")
                .paymentLinkId(PAYMENT_LINK_ID)
                .code(code)
                .desc("Thành công")
                .build();
    }

    private Order givenLinkForPayosOrderCode(PaymentStatus status) {
        Order order = buildOrder(status, PaymentMethod.BANK_TRANSFER);
        when(payosPaymentLinkRepository.findByPayosOrderCode(PAYOS_ORDER_CODE))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().plusMinutes(10))));
        return order;
    }

    @Test
    void handlePayosWebhook_shouldMarkOrderPaid_whenAmountEnough() {
        Order order = givenLinkForPayosOrderCode(PaymentStatus.UNPAID);

        WebhookOutcome outcome = paymentService.handlePayosWebhook(webhook("00", 500_000L));

        assertThat(outcome).isEqualTo(WebhookOutcome.PAID);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(orderRepository).save(order);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("500000");
        assertThat(captor.getValue().getReferenceCode()).isEqualTo(REFERENCE_CODE);
        assertThat(captor.getValue().getTransactionCode()).isEqualTo(PAYMENT_LINK_ID);
    }

    @Test
    void handlePayosWebhook_shouldNotMarkPaid_whenAmountLessThanTotal() {
        Order order = givenLinkForPayosOrderCode(PaymentStatus.UNPAID);

        WebhookOutcome outcome = paymentService.handlePayosWebhook(webhook("00", 100_000L));

        assertThat(outcome).isEqualTo(WebhookOutcome.UNDERPAID);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        verify(orderRepository, never()).save(any());

        // Vẫn lưu lại giao dịch để đối soát, nhưng ở trạng thái UNPAID
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void handlePayosWebhook_shouldIgnore_whenTransactionNotSuccessful() {
        WebhookOutcome outcome = paymentService.handlePayosWebhook(webhook("01", 500_000L));

        assertThat(outcome).isEqualTo(WebhookOutcome.NOT_SUCCESS);
        verify(payosPaymentLinkRepository, never()).findByPayosOrderCode(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handlePayosWebhook_shouldReturnOrderNotFound_whenNoLinkMatches() {
        when(payosPaymentLinkRepository.findByPayosOrderCode(PAYOS_ORDER_CODE)).thenReturn(Optional.empty());

        assertThat(paymentService.handlePayosWebhook(webhook("00", 500_000L)))
                .isEqualTo(WebhookOutcome.ORDER_NOT_FOUND);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handlePayosWebhook_shouldStillRecordPayment_whenOrderAlreadyPaid() {
        givenLinkForPayosOrderCode(PaymentStatus.PAID);

        assertThat(paymentService.handlePayosWebhook(webhook("00", 500_000L)))
                .isEqualTo(WebhookOutcome.ORDER_ALREADY_PAID);

        verify(orderRepository, never()).save(any());

        // Tiền vào đơn đã trả vẫn phải nhìn thấy được để còn hoàn tiền
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void handlePayosWebhook_shouldSkip_whenTransactionAlreadyProcessed() {
        givenLinkForPayosOrderCode(PaymentStatus.UNPAID);
        when(paymentRepository.existsByReferenceCode(REFERENCE_CODE)).thenReturn(true);

        assertThat(paymentService.handlePayosWebhook(webhook("00", 500_000L)))
                .isEqualTo(WebhookOutcome.DUPLICATE_TRANSACTION);

        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handlePayosWebhook_shouldRejectAsFinal_whenAmountMissing() {
        WebhookData data = new WebhookData();
        data.setOrderCode(PAYOS_ORDER_CODE);
        data.setReference(REFERENCE_CODE);
        data.setCode("00");

        // Payload sai vĩnh viễn: phải là outcome cuối cùng chứ không được ném NPE
        assertThat(paymentService.handlePayosWebhook(data)).isEqualTo(WebhookOutcome.INVALID_PAYLOAD);

        verify(payosPaymentLinkRepository, never()).findByPayosOrderCode(any());
        verify(paymentRepository, never()).save(any());
    }

    // ==================== cancelOpenPaymentLink ====================

    @Test
    void cancelOpenPaymentLink_shouldCancelLatestUnexpiredLink() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1"))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().plusMinutes(10))));

        paymentService.cancelOpenPaymentLink(order);

        verify(payosGateway).cancelLink(eq(PAYMENT_LINK_ID), anyString());
    }

    @Test
    void cancelOpenPaymentLink_shouldSkip_whenOrderPaidOrCod() {
        paymentService.cancelOpenPaymentLink(buildOrder(PaymentStatus.PAID, PaymentMethod.BANK_TRANSFER));
        paymentService.cancelOpenPaymentLink(buildOrder(PaymentStatus.UNPAID, PaymentMethod.COD));

        verify(payosPaymentLinkRepository, never()).findFirstByOrderIdOrderByIdDesc(any());
        verify(payosGateway, never()).cancelLink(anyString(), anyString());
    }

    @Test
    void cancelOpenPaymentLink_shouldNotThrow_whenPayosFails() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        when(payosPaymentLinkRepository.findFirstByOrderIdOrderByIdDesc("o1"))
                .thenReturn(Optional.of(buildLink(order, LocalDateTime.now().plusMinutes(10))));
        doThrow(new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR))
                .when(payosGateway).cancelLink(anyString(), anyString());

        paymentService.cancelOpenPaymentLink(order);

        verify(payosGateway).cancelLink(eq(PAYMENT_LINK_ID), anyString());
    }
}
