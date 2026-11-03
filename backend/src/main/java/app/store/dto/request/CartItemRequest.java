package app.store.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

// Không có cartId: luôn thêm vào giỏ của user trong JWT.
@Builder
public record CartItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId,
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        Integer quantity
) {
}
