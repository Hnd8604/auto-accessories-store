package app.store.dto.response;

import lombok.Builder;

@Builder
public record ServiceResponse(
        Long id,
        String name,
        String shortDescription,
        String fullDescription,
        String slug,
        Integer displayOrder,
        String primaryImageUrl
) {
}
