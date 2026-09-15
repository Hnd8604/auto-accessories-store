package app.store.dto.event;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record OrderStatusChangedEvent(
        String orderId,
        String orderCode,
        String userId,
        String userEmail,
        String recipientName,
        String oldStatus,
        String newStatus,
        LocalDateTime changedAt
) {
}
