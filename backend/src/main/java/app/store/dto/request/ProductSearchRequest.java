package app.store.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSearchRequest {
    String keyword;
    String category;      // hoặc categoryId
    Double minPrice;
    Double maxPrice;
    Boolean inStock;
    String brand;         // thêm brand
}
