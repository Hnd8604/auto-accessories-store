package app.store.dto.request;

import app.store.enums.OrderStatus;
import app.store.enums.PaymentStatus;
import lombok.Builder;

@Builder
public record OrderUpdateByAdminRequest(
        PaymentStatus paymentStatus,
        OrderStatus orderStatus
) {
}
