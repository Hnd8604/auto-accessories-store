package app.store.dto.response;

import app.store.enums.SenderType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ChatMessageResponse(
        String id,
        String conversationId,
        SenderType senderType,
        String content,
        LocalDateTime createdAt
) {
}
