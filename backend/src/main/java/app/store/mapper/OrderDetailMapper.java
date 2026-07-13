package app.store.mapper;

import app.store.dto.request.OrderDetailRequest;
import app.store.dto.response.OrderDetailResponse;
import app.store.entity.CartItem;
import app.store.entity.Order;
import app.store.entity.OrderDetail;
import app.store.entity.ProductImage;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderDetailMapper {


    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "unitPrice", source = "product.unitPrice")
    @Mapping(target = "productImage", source = "orderDetail", qualifiedByName = "extractPrimaryImage")
    OrderDetailResponse toOrderDetailResponse(OrderDetail orderDetail);

    List<OrderDetailResponse> toOrderDetailResponseList(List<OrderDetail> orderDetails);

    @Named("extractPrimaryImage")
    default String extractPrimaryImage(OrderDetail orderDetail) {
        if (orderDetail.getProduct() == null || orderDetail.getProduct().getProductImages() == null) {
            return null;
        }
        return orderDetail.getProduct().getProductImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(
                        orderDetail.getProduct().getProductImages().stream()
                                .map(ProductImage::getImageUrl)
                                .findFirst()
                                .orElse(null)
                );
    }
}

