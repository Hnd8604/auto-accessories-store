package app.store.dto.response;

import lombok.Builder;

@Builder
public record ProductImageResponse(
        Long id,
        Long productId,
        String imageUrl,
        String altText,
        Boolean isPrimary,
        Integer sortOrder
) {
}
