package app.store.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class ProductImage extends BaseEntityLong{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "product_id")
    Product product;
    String imageUrl;
    String altText;
    Boolean isPrimary;
    Integer sortOrder;
}
