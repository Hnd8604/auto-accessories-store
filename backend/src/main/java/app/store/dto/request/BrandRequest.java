package app.store.dto.request;

import lombok.Builder;

@Builder
public record BrandRequest(
        String name,
        String description
) {
}
