package app.store.mapper;


import app.store.dto.request.CartRequest;
import app.store.dto.response.CartCreationResponse;
import app.store.dto.response.CartResponse;
import app.store.entity.Cart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = {CartItemMapper.class})
public interface CartMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "items", source = "cartItems")
    CartResponse toCartResponse(Cart cart);

//    @Mapping(target = "cartItems", ignore = true)
//    @Mapping(target = "user", ignore = true)
//    Cart toCart(CartRequest request);

//    @Mapping(target = "userId", source = "user.id")
//    CartCreationResponse toCartCreationResponse(Cart cart);
}
