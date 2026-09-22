package app.store.dto.event;

import app.store.enums.PaymentMethod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

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
