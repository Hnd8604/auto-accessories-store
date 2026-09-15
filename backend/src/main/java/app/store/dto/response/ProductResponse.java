package app.store.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder(toBuilder = true)
public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal unitPrice,
        String categoryName,
        String brandName, // Optional - can be null
        Integer stockQuantity,
        String slug,
        String primaryImageUrl // URL of the primary image, derived from productImages
) {
}
