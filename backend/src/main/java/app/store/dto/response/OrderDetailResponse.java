package app.store.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record OrderDetailResponse(
        String id,
        String orderId,
        String productId,
        String productName,
        BigDecimal unitPrice,
        String productImage,
        Integer quantity
) {
}
