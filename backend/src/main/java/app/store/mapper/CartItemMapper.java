package app.store.mapper;


import app.store.dto.request.CartItemRequest;
import app.store.dto.response.CartItemResponse;
import app.store.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface CartItemMapper {


    @Mapping(target = "cartId", source = "cart.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "unitPrice", source = "product.unitPrice")
    @Mapping(target = "totalPrice", expression = "java(item.getProduct() != null && item.getProduct().getUnitPrice() != null ? item.getProduct().getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())) : null)")
    CartItemResponse toCartItemResponse(CartItem item);


//    @Mapping(target = "product", ignore = true)
//    @Mapping(target = "cart", ignore = true)
//    CartItem toCartItem(CartItemRequest request);

}