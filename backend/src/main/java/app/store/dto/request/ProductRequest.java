package app.store.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed {max} characters")
        String name,
        String description,
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0", message = "Unit price must not be negative")
        BigDecimal unitPrice,
        @NotNull(message = "Category ID is required")
        Long categoryId,
        Long brandId,
        @NotNull(message = "Stock quantity is required")
        @PositiveOrZero(message = "Stock quantity must not be negative")
        Integer stockQuantity
) {
}
