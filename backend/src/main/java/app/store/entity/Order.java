package app.store.entity;

import app.store.enums.OrderStatus;
import app.store.enums.PaymentMethod;
import app.store.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends BaseEntityUUID {

   // Many-to-One: each order belongs to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    User user;

    @Column(nullable = false)
    BigDecimal totalPrice;

    @Column(nullable = false)
    String nameRecipient;

    @Column(nullable = false)
    String phoneRecipient;

    @Column(nullable = false)
    String addressRecipient;

    String note;

    @Column(unique = true, nullable = false)
    String orderCode;  // Mã đơn hàng duy nhất, dùng trong nội dung chuyển khoản (VD: DH20240115ABC123)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentStatus paymentStatus;

    // One-to-Many: an order has many order details
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderDetail> orderDetails;
}
