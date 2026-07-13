package app.store.dto.response;

import app.store.enums.OrderStatus;
import app.store.enums.PaymentMethod;
import app.store.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    String id;
    String userId;
    String orderCode;
    BigDecimal totalPrice;
    String nameRecipient;
    String phoneRecipient;
    String addressRecipient;
    String note;
    OrderStatus status;
    PaymentMethod paymentMethod;
    PaymentStatus paymentStatus;
    LocalDateTime createdAt;
    List<OrderDetailResponse> orderDetails;
}
