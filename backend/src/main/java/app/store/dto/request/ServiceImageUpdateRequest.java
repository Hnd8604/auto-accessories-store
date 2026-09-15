package app.store.dto.request;

import lombok.Builder;

@Builder
public record ServiceImageUpdateRequest(
        String altText,
        Boolean isPrimary,
        Integer sortOrder
) {
}
