package app.store.dto.event;

import app.store.enums.PaymentMethod;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record OrderCreatedEvent(
        String orderId,
        String orderCode,
        String userId,
        String userEmail,
        String recipientName,
        BigDecimal totalPrice,
        PaymentMethod paymentMethod,
        LocalDateTime createdAt
) {
}
