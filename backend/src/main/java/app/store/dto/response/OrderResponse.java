package app.store.dto.response;

import app.store.enums.OrderStatus;
import app.store.enums.PaymentMethod;
import app.store.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record OrderResponse(
        String id,
        String userId,
        String orderCode,
        BigDecimal totalPrice,
        String nameRecipient,
        String phoneRecipient,
        String addressRecipient,
        String note,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        LocalDateTime createdAt,
        List<OrderDetailResponse> orderDetails
) {
}
