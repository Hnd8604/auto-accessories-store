package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ServiceRequest(
        @NotBlank(message = "Service name is required")
        @Size(max = 255, message = "Service name must not exceed {max} characters")
        String name,
        String shortDescription,
        String fullDescription,
        @PositiveOrZero(message = "Display order must not be negative")
        Integer displayOrder
) {
}
