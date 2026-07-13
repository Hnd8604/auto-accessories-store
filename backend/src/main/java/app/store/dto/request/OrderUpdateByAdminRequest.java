package app.store.dto.request;

import app.store.enums.OrderStatus;
import app.store.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

public class OrderUpdateByAdminRequest {
    PaymentStatus paymentStatus;
    OrderStatus orderStatus;
}