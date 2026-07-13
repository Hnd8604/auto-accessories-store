package app.store.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response for Step 2: Verify OTP.
 * Returns confirmation that OTP was verified.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyOtpResponse {

    String sessionId;
    boolean verified;
    Integer remainingAttempts;
}
