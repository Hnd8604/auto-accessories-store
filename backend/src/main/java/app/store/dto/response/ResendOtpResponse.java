package app.store.dto.response;

import lombok.Builder;

@Builder
public record ResendOtpResponse(
        String sessionId,
        String maskedEmail
) {
}
