package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateConversationRequest(
        @NotBlank(message = "Tên không được để trống")
        String guestName
) {
}
