package app.store.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record BrandResponse(
        Long id,
        String name,
        String description,
        String slug,
        Long productCount
) {
}
