package app.store.mapper;

import app.store.dto.request.CategoryRequest;
import app.store.dto.response.CategoryResponse;
import app.store.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring",uses = {ProductMapper.class})
public interface CategoryMapper {
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "brands", ignore = true)
    @Mapping(target = "slug", ignore = true)
    Category toCategory(CategoryRequest request);

    @Mapping(target = "productCount", expression = "java(category.getProducts() != null ? (long) category.getProducts().size() : 0L)")
    CategoryResponse toCategoryResponse(Category category);

    @Mapping(target = "id", ignore = true) // Ignore the ID field during update
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "brands", ignore = true)
    @Mapping(target="createdAt", ignore = true)
    @Mapping(target="updatedAt", ignore = true)
    void updateCategory(@MappingTarget Category category, CategoryRequest request);
}
