package app.store.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductSearchRequest(
        String keyword,
        String category,
        @PositiveOrZero(message = "Minimum price must not be negative")
        BigDecimal minPrice,
        @PositiveOrZero(message = "Maximum price must not be negative")
        BigDecimal maxPrice,
        Boolean inStock,
        String brand
) {
}
