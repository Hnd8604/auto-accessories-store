package app.store.dto.request;

import lombok.Builder;

@Builder
public record CartItemRequest(
        Long cartId,
        Long productId,
        Integer quantity
) {
}
