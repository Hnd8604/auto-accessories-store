package app.store.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)

public class CartItemResponse {
    Long id;
    Long cartId;
    Long productId;
//    String productName;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal totalPrice;
}
