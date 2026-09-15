package app.store.dto.response;

import lombok.Builder;

/**
 * Response for Step 2: Verify OTP.
 * Returns confirmation that OTP was verified.
 */
@Builder
public record VerifyOtpResponse(
        String sessionId,
        boolean verified,
        Integer remainingAttempts
) {
}
