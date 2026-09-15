package app.store.dto.request;

import lombok.Builder;

@Builder
public record PermissionRequest(
        String name,
        String description
) {
}
