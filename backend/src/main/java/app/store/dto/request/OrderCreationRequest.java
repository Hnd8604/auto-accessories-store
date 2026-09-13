package app.store.dto.request;


import app.store.enums.PaymentMethod;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderCreationRequest {

    // Không có userId: người đặt hàng luôn là user trong JWT, không lấy từ body.
    String nameRecipient;
    String phoneRecipient;
    String addressRecipient;
    String note;
    PaymentMethod paymentMethod;  // COD hoặc BANK_TRANSFER
    List<OrderDetailRequest> orderDetails;
}
