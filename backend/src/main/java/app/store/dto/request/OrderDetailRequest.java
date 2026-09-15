package app.store.dto.request;

import lombok.Builder;

@Builder
public record OrderDetailRequest(
        Long productId,
        Integer quantity
) {
}
