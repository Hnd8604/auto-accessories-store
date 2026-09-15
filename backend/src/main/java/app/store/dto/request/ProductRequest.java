package app.store.dto.request;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record ProductRequest(
        String name,
        String description,
        BigDecimal unitPrice,
        Long categoryId,
        Long brandId, // Optional - can be null
        Integer stockQuantity
) {
}
