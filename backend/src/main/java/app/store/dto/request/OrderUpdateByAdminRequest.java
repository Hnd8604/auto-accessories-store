package app.store.dto.request;

import app.store.enums.OrderStatus;
import app.store.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record OrderUpdateByAdminRequest(
        @NotNull(message = "Payment status is required")
        PaymentStatus paymentStatus,
        @NotNull(message = "Order status is required")
        OrderStatus orderStatus
) {
}
