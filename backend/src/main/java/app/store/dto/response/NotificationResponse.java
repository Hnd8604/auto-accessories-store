package app.store.dto.response;

import app.store.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
        String id,
        String title,
        String message,
        NotificationType type,
        String referenceId,
        boolean isRead,
        LocalDateTime createdAt
) {
}
