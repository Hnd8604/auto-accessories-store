package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ConfirmResetPasswordRequest(
        @NotBlank(message = "Session ID is required")
        String sessionId,
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least {min} characters")
        String newPassword
) {
}
