package app.store.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatusChangedEvent {
    String orderId;
    String orderCode;
    String userId;
    String userEmail;
    String recipientName;
    String oldStatus;
    String newStatus;
    LocalDateTime changedAt;
}
