package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record PermissionRequest(
        @NotBlank(message = "Permission name is required")
        @Size(max = 255, message = "Permission name must not exceed {max} characters")
        String name,
        @Size(max = 255, message = "Description must not exceed {max} characters")
        String description
) {
}
