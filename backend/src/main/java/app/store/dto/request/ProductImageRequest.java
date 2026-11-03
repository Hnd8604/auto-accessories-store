package app.store.dto.request;

import lombok.Builder;

@Builder
public record ProductImageRequest(
        Long productId,
        String altText,
        Boolean isPrimary,
        Integer sortOrder
) {
}
