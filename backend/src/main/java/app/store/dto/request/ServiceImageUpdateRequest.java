package app.store.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ServiceImageUpdateRequest(
        @Size(max = 255, message = "Alt text must not exceed {max} characters")
        String altText,
        Boolean isPrimary,
        @PositiveOrZero(message = "Sort order must not be negative")
        Integer sortOrder
) {
}
