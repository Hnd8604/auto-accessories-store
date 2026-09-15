package app.store.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ChatMessageResponse(
        String id,
        String conversationId,
        String senderType,
        String content,
        LocalDateTime createdAt
) {
}
