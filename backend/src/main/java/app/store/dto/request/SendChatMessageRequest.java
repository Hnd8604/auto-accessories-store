package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;


@Builder
public record SendChatMessageRequest(
        @NotBlank(message = "Conversation ID is required")
        String conversationId,
        @NotBlank(message = "Message content is required")
        @Size(max = 2000, message = "Message content must not exceed {max} characters")
        String content
) {
}
