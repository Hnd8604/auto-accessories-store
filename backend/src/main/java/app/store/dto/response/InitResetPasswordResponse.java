package app.store.dto.response;

import lombok.Builder;

@Builder
public record InitResetPasswordResponse(
        String sessionId,
        String maskedEmail
) {
}
