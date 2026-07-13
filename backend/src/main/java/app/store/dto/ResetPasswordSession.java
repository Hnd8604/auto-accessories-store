package app.store.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordSession {

    String userId;
    String email;
    String step;
    String otpHash;
    int otpAttempt;
    long otpExpireAt;
    long createdAt;
    long lastSentAt;

    public static final String STEP_EMAIL_VERIFIED = "EMAIL_VERIFIED";
    public static final String STEP_OTP_VERIFIED = "OTP_VERIFIED";

    public static final int MAX_OTP_ATTEMPTS = 5;
    public static final long OTP_TTL_MILLIS = 3 * 60 * 1000; // 3 minutes
    public static final long SESSION_TTL_MILLIS = 5 * 60 * 1000; // 5 minutes  
    public static final long RESEND_COOLDOWN_MILLIS = 60 * 1000; // 1 minute
}
