package app.store.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationResponse {
    String id;
    String guestName;
    String channel;
    String status;
    int unreadCount;
    LocalDateTime lastMessageAt;
    LocalDateTime createdAt;
    String lastMessage;
}
