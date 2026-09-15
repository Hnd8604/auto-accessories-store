package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record GoogleAuthRequest(
        @NotBlank(message = "Authorization code không được để trống")
        String code
) {
}
