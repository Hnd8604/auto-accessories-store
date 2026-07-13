package app.store.mapper;

import app.store.dto.request.BrandRequest;
import app.store.dto.response.BrandResponse;
import app.store.dto.response.BrandResponse;
import app.store.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BrandMapper {


    @Mapping(target = "productCount", expression = "java(brand.getProducts() != null ? (long) brand.getProducts().size() : 0L)")
    BrandResponse toBrandResponse(Brand brand);

    @Mapping(target = "products", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "slug", ignore = true)
    Brand toBrand(BrandRequest brandRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateBrand(@MappingTarget Brand brand, BrandRequest brandRequest);
}
