package app.store.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Link thanh toán đã tạo trên payOS cho một đơn hàng.
 */
@Entity
@Table(name = "payos_payment_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PayosPaymentLink extends BaseEntityLong {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Order order;

    @Column(nullable = false, unique = true)
    Long payosOrderCode; // orderCode gửi sang payOS, lấy từ payos_order_code_seq

    @Column(nullable = false, unique = true)
    String paymentLinkId;

    @Column(nullable = false, length = 1024)
    String checkoutUrl;

    @Column(nullable = false)
    LocalDateTime expiresAt;
}
