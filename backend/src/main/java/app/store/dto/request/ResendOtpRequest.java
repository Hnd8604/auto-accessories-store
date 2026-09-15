package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ResendOtpRequest(
        @NotBlank(message = "Session ID không được để trống")
        String sessionId
) {
}
