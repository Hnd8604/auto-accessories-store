package app.store.dto.request;

import app.store.enums.PaymentMethod;

import java.util.List;
import lombok.Builder;

@Builder
public record OrderCreationRequest(
        // Không có userId: người đặt hàng luôn là user trong JWT, không lấy từ body.
        String nameRecipient,
        String phoneRecipient,
        String addressRecipient,
        String note,
        PaymentMethod paymentMethod, // COD hoặc BANK_TRANSFER
        List<OrderDetailRequest> orderDetails
) {
}
