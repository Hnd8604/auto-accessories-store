package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record PostCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 255, message = "Category name must not exceed {max} characters")
        String name,
        @Size(max = 1000, message = "Description must not exceed {max} characters")
        String description
) {
}
