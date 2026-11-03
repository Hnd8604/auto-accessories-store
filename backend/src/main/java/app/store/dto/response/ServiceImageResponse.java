package app.store.dto.response;

import lombok.Builder;

@Builder
public record ServiceImageResponse(
        Long id,
        Long serviceId,
        String imageUrl,
        String altText,
        Boolean isPrimary,
        Integer sortOrder
) {
}
