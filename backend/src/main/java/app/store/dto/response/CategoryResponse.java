package app.store.dto.response;

import lombok.Builder;

@Builder
public record CategoryResponse(
        String id,
        String name,
        String description,
        String slug,
        Long productCount
) {
}
