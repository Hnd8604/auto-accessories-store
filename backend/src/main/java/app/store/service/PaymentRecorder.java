package app.store.service;

import app.store.entity.Order;
import app.store.entity.Payment;
import app.store.enums.PaymentGateway;
import app.store.enums.PaymentStatus;
import app.store.enums.WebhookOutcome;
import app.store.repository.OrderRepository;
import app.store.repository.PaymentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Ghi nhận một giao dịch tiền vào của payOS: cập nhật trạng thái đơn + lưu bản ghi Payment
 * trong cùng một transaction.
 * <p>
 * Tách khỏi {@link PaymentService} để có transaction riêng khi đối soát qua API
 * (createPayment / checkPaymentStatus không có transaction vì còn gọi payOS). Nếu webhook đã ghi
 * cùng giao dịch trước (trùng reference_code) thì chỉ transaction của lần ghi này rollback, không
 * kéo theo cả request. Gọi từ webhook (đã có transaction) thì dùng chung transaction của webhook.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentRecorder {

    OrderRepository orderRepository;
    PaymentRepository paymentRepository;

    @Transactional
    public WebhookOutcome apply(Order order, IncomingTransaction tx) {
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

    private void savePaymentRecord(Order order, IncomingTransaction tx, PaymentStatus status) {
        Payment payment = Payment.builder()
                .order(order)
                .amount(BigDecimal.valueOf(tx.amount()))
                .gateway(PaymentGateway.PAYOS)
                .transactionCode(tx.paymentLinkId())
                .referenceCode(tx.reference())
                .transferContent(tx.description())
                .accountNumber(tx.accountNumber())
                .transactionDate(tx.transactionDateTime())
                .status(status)
                .build();
        paymentRepository.save(payment);
    }

    /** Một giao dịch tiền vào, từ webhook hoặc từ API tra cứu link của payOS. */
    public record IncomingTransaction(Long amount, String reference, String paymentLinkId,
            String description, String accountNumber, String transactionDateTime) {
    }
}
