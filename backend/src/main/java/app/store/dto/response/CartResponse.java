package app.store.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartResponse {
//    BigDecimal totalPrice;
//    Integer totalItems;
    //BigDecimal price; add after has voucher
    Long id;
    String userId;
    List<CartItemResponse> items;
}