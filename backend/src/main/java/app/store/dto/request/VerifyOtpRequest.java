package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record VerifyOtpRequest(
        @NotBlank(message = "Session ID không được để trống")
        String sessionId,
        @NotBlank(message = "OTP không được để trống")
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP phải là 6 chữ số")
        String otp
) {
}
