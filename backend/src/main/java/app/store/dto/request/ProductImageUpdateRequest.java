package app.store.dto.request;

import lombok.Builder;

@Builder
public record ProductImageUpdateRequest(
        String imageUrl,
        String altText,
        Boolean isPrimary,
        Integer sortOrder
) {
}
