package app.store.dto.response;

import lombok.Builder;

@Builder
public record PermissionResponse(
        String name,
        String description
) {
}
