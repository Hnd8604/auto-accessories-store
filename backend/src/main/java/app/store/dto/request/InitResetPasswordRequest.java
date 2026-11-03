package app.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record InitResetPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email
) {
}
