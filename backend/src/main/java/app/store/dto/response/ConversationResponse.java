package app.store.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ConversationResponse(
        String id,
        String guestName,
        String channel,
        String status,
        int unreadCount,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt,
        String lastMessage
) {
}
