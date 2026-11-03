package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least {min} characters")
        String newPassword,
        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {
}
