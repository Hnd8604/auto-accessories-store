package app.store.mapper;


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

//    @Mapping(target = "userId", source = "user.id")
//    CartCreationResponse toCartCreationResponse(Cart cart);
}
