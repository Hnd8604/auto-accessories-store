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

    String userId;
    String nameRecipient;
    String phoneRecipient;
    String addressRecipient;
    String note;
    PaymentMethod paymentMethod;  // COD hoặc BANK_TRANSFER
    List<OrderDetailRequest> orderDetails;
}
