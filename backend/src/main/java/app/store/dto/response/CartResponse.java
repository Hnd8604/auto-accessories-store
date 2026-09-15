package app.store.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record CartResponse(
        // BigDecimal totalPrice;
        // Integer totalItems;
        // BigDecimal price; add after has voucher
        Long id,
        String userId,
        List<CartItemResponse> items
) {
}
