package app.store.mapper;

import app.store.dto.request.ProductRequest;
import app.store.dto.response.ProductResponse;
import app.store.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "productImages", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "slug", ignore = true)
    Product toProduct(ProductRequest request);


    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "primaryImageUrl", ignore = true)
    ProductResponse toProductResponse(Product product);

    @Mapping(target = "id", ignore = true) // Ignore the ID field during update
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "productImages", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping (target = "createdAt", ignore = true)
    @Mapping (target = "updatedAt", ignore = true)
    void updateProduct(@MappingTarget Product product, ProductRequest request);
}
