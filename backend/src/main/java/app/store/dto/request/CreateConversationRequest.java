package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateConversationRequest(
        @NotBlank(message = "Name is required")
        String guestName
) {
}
