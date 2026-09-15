package app.store.dto.request;

import java.util.Set;
import lombok.Builder;

@Builder
public record RoleRequest(
        String name,
        String description,
        Set<String> permissions // Set of permission names so we only use strings instead of full objects
) {
}
