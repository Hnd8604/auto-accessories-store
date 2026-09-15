package app.store.dto.request;

import lombok.Builder;

@Builder
public record ProductSearchRequest(
        String keyword,
        String category, // hoặc categoryId
        Double minPrice,
        Double maxPrice,
        Boolean inStock,
        String brand // thêm brand
) {
}
