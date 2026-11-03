package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.Set;

@Builder
public record RoleRequest(
        @NotBlank(message = "Role name is required")
        @Size(max = 255, message = "Role name must not exceed {max} characters")
        String name,
        @Size(max = 255, message = "Description must not exceed {max} characters")
        String description,
        Set<String> permissions // tên permission, không phải object
) {
}
