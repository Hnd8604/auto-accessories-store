package app.store.mapper;

import app.store.dto.request.OrderCreationRequest;
import app.store.dto.response.OrderResponse;
import app.store.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


import java.math.BigDecimal;

@Mapper(componentModel = "spring",  uses = {OrderDetailMapper.class})  // Add this 'uses' attribute
public interface OrderMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "orderDetails", source = "orderDetails")
    OrderResponse toOrderResponse(Order order);


    @Mapping(target="user", ignore = true)
    @Mapping(target="status", constant = "PENDING")
    @Mapping(target="paymentStatus", constant = "UNPAID")
    @Mapping(target = "orderDetails", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "orderCode", ignore = true)
    Order createOrder(OrderCreationRequest request);
//
//    @Mapping(target="user", ignore = true)
//    @Mapping(target = "orderDetails", ignore = true)
//    @Mapping(target = "totalPrice", ignore = true)
//    Order updateOrder(OrderUpdateRequest request);
}
