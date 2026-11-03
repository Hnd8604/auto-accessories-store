package app.store.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LogoutRequest(
        @NotBlank(message = "Access token is required")
        String accessToken,
        String refreshToken
) {
}
