package app.store.dto.request;

import lombok.Builder;

@Builder
public record ServiceImageRequest(
        Long serviceId,
        String altText,
        Boolean isPrimary,
        Integer sortOrder
) {
}
