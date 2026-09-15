package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record BannerRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed {max} characters")
        String title,
        @Size(max = 255, message = "Subtitle must not exceed {max} characters")
        String subtitle,
        @Size(max = 255, message = "Redirect URL must not exceed {max} characters")
        String redirectUrl,
        @Size(max = 255, message = "Alt text must not exceed {max} characters")
        String altText,
        @Size(max = 255, message = "Button text must not exceed {max} characters")
        String buttonText,
        @PositiveOrZero(message = "Display order must not be negative")
        Integer displayOrder,
        Boolean isActive
) {
}
