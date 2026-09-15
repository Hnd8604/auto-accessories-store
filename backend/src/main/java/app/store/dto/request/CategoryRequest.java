package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 255, message = "Category name must not exceed {max} characters")
        String name,
        String description
) {
}
