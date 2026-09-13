package app.store.entity;

import app.store.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment extends BaseEntityLong {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Order order;

    @Column(nullable = false)
    BigDecimal amount;

    String gateway; // Cổng thanh toán (PAYOS)
    String transactionCode; // paymentLinkId của payOS
    String referenceCode; // Mã tham chiếu giao dịch ngân hàng, duy nhất
    String transferContent; // Nội dung chuyển khoản
    String accountNumber; // Số tài khoản nhận tiền
    String transactionDate; // Ngày giao dịch

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentStatus status; // UNPAID, PAID, REFUNDED

}
