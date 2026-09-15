package app.store.dto.response;

import java.util.Set;
import lombok.Builder;

@Builder
public record RoleResponse(
        String name,
        String description,
        Set<PermissionResponse> permissions
) {
}
