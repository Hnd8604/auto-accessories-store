package app.store.dto.event;

import app.store.enums.OrderStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record OrderStatusChangedEvent(
        String orderId,
        String orderCode,
        String userId,
        String userEmail,
        String recipientName,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        LocalDateTime changedAt
) {
}
