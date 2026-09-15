package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SendChatMessageRequest(
        @NotBlank
        String conversationId,
        @NotBlank
        String content,
        @NotBlank
        String senderType // CUSTOMER | ADMIN
) {
}
