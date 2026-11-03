package app.store.dto.response;

import app.store.enums.ConversationChannel;
import app.store.enums.ConversationStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ConversationResponse(
        String id,
        String guestName,
        ConversationChannel channel,
        ConversationStatus status,
        int unreadCount,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt,
        String lastMessage
) {
}
