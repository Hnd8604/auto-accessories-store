package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import app.store.dto.request.SepayWebhookRequest;
import app.store.entity.Order;
import app.store.entity.Payment;
import app.store.enums.PaymentMethod;
import app.store.enums.PaymentStatus;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.OrderRepository;
import app.store.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    PaymentRepository paymentRepository;
    @InjectMocks
    PaymentService paymentService;

    private static final String ORDER_CODE = "DH20240115A1B2C3D4";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "bankAccountNumber", "0123456789");
        ReflectionTestUtils.setField(paymentService, "bankName", "MBBank");
        ReflectionTestUtils.setField(paymentService, "bankAccountName", "NGUYEN VAN A");
        ReflectionTestUtils.setField(paymentService, "bankCode", "MB");
        ReflectionTestUtils.setField(paymentService, "sepayApiKey", "secret-api-key");
    }

    private Order buildOrder(PaymentStatus status, PaymentMethod method) {
        Order order = new Order();
        order.setId("o1");
        order.setOrderCode(ORDER_CODE);
        order.setTotalPrice(BigDecimal.valueOf(500_000));
        order.setPaymentStatus(status);
        order.setPaymentMethod(method);
        return order;
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
    void createPayment_shouldBuildVietQrUrl() {
        when(orderRepository.findById("o1"))
                .thenReturn(Optional.of(buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER)));

        var response = paymentService.createPayment("o1");

        assertThat(response.getQrCodeUrl())
                .startsWith("https://qr.sepay.vn/img?")
                .contains("bank=MB")
                .contains("acc=0123456789")
                .contains("amount=500000")     // BigDecimal -> số nguyên, không có phần thập phân
                .contains("des=" + ORDER_CODE);
        assertThat(response.getPaymentContent()).isEqualTo(ORDER_CODE);
        assertThat(response.getBankName()).isEqualTo("MBBank");
    }

    @Test
    void createPayment_shouldThrow_whenOrderNotFound() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment("missing"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_EXISTED);
    }

    @Test
    void createPayment_shouldThrow_whenAlreadyPaid() {
        when(orderRepository.findById("o1"))
                .thenReturn(Optional.of(buildOrder(PaymentStatus.PAID, PaymentMethod.BANK_TRANSFER)));

        assertThatThrownBy(() -> paymentService.createPayment("o1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã được thanh toán");
    }

    @Test
    void createPayment_shouldThrow_whenNotBankTransfer() {
        when(orderRepository.findById("o1"))
                .thenReturn(Optional.of(buildOrder(PaymentStatus.UNPAID, PaymentMethod.COD)));

        assertThatThrownBy(() -> paymentService.createPayment("o1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chuyển khoản");
    }

    // ==================== checkPaymentStatus ====================

    @Test
    void checkPaymentStatus_shouldIncludeQr_whenUnpaid() {
        when(orderRepository.findById("o1"))
                .thenReturn(Optional.of(buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER)));

        var response = paymentService.checkPaymentStatus("o1");

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(response.getQrCodeUrl()).isNotNull();
    }

    @Test
    void checkPaymentStatus_shouldNotIncludeQr_whenPaid() {
        when(orderRepository.findById("o1"))
                .thenReturn(Optional.of(buildOrder(PaymentStatus.PAID, PaymentMethod.BANK_TRANSFER)));

        var response = paymentService.checkPaymentStatus("o1");

        assertThat(response.getQrCodeUrl()).isNull();
    }

    // ==================== handleSepayWebhook ====================

    private SepayWebhookRequest webhook(String transferType, String content, long amount) {
        return SepayWebhookRequest.builder()
                .gateway("MBBank")
                .transferType(transferType)
                .transferAmount(amount)
                .content(content)
                .code("TF123456")
                .referenceCode("FT24015ABCDE")
                .accountNumber("0123456789")
                .transactionDate("2024-01-15 10:30:00")
                .build();
    }

    @Test
    void handleSepayWebhook_shouldMarkOrderPaid_whenAmountEnough() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        when(orderRepository.findByOrderCode(ORDER_CODE)).thenReturn(Optional.of(order));

        // Nội dung chuyển khoản thực tế thường có thêm chữ và dấu cách quanh mã đơn
        boolean result = paymentService.handleSepayWebhook(
                webhook("in", "CT DEN chuyen tien " + ORDER_CODE + " cam on", 500_000L));

        assertThat(result).isTrue();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(orderRepository).save(order);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("500000");
    }

    @Test
    void handleSepayWebhook_shouldNotMarkPaid_whenAmountLessThanTotal() {
        Order order = buildOrder(PaymentStatus.UNPAID, PaymentMethod.BANK_TRANSFER);
        when(orderRepository.findByOrderCode(ORDER_CODE)).thenReturn(Optional.of(order));

        paymentService.handleSepayWebhook(webhook("in", ORDER_CODE, 100_000L));

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        verify(orderRepository, never()).save(any());

        // Vẫn lưu lại giao dịch để đối soát, nhưng ở trạng thái UNPAID
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void handleSepayWebhook_shouldIgnoreOutgoingTransaction() {
        boolean result = paymentService.handleSepayWebhook(webhook("out", ORDER_CODE, 500_000L));

        assertThat(result).isTrue();
        verify(orderRepository, never()).findByOrderCode(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleSepayWebhook_shouldIgnore_whenContentBlank() {
        assertThat(paymentService.handleSepayWebhook(webhook("in", "   ", 500_000L))).isTrue();

        verify(orderRepository, never()).findByOrderCode(any());
    }

    @Test
    void handleSepayWebhook_shouldIgnore_whenNoOrderCodeInContent() {
        assertThat(paymentService.handleSepayWebhook(webhook("in", "chuyen tien lung tung", 500_000L)))
                .isTrue();

        verify(orderRepository, never()).findByOrderCode(any());
    }

    @Test
    void handleSepayWebhook_shouldIgnore_whenOrderNotFound() {
        when(orderRepository.findByOrderCode(ORDER_CODE)).thenReturn(Optional.empty());

        assertThat(paymentService.handleSepayWebhook(webhook("in", ORDER_CODE, 500_000L))).isTrue();

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleSepayWebhook_shouldSkip_whenOrderAlreadyPaid() {
        Order order = buildOrder(PaymentStatus.PAID, PaymentMethod.BANK_TRANSFER);
        when(orderRepository.findByOrderCode(ORDER_CODE)).thenReturn(Optional.of(order));

        assertThat(paymentService.handleSepayWebhook(webhook("in", ORDER_CODE, 500_000L))).isTrue();

        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any()); // không ghi trùng giao dịch
    }

    // ==================== verifyApiKey ====================

    @Test
    void verifyApiKey_shouldPass_withRawKeyOrWithPrefix() {
        assertThatCode(() -> paymentService.verifyApiKey("secret-api-key")).doesNotThrowAnyException();
        assertThatCode(() -> paymentService.verifyApiKey("Apikey secret-api-key")).doesNotThrowAnyException();
        assertThatCode(() -> paymentService.verifyApiKey("Bearer secret-api-key")).doesNotThrowAnyException();
    }

    @Test
    void verifyApiKey_shouldThrow_whenMissingOrWrong() {
        assertThatThrownBy(() -> paymentService.verifyApiKey(null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.WEBHOOK_INVALID_SIGNATURE);

        assertThatThrownBy(() -> paymentService.verifyApiKey("  "))
                .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> paymentService.verifyApiKey("sai-key"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.WEBHOOK_INVALID_SIGNATURE);
    }
}
