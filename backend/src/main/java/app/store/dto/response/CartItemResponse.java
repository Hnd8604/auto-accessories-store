package app.store.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record CartItemResponse(
        Long id,
        Long cartId,
        Long productId,
        // String productName;
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
