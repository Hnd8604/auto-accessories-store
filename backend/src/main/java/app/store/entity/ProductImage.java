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
public class ProductImage extends BaseEntityLong {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(nullable = false)
    String imageUrl;

    String altText;

    @Column(nullable = false)
    Boolean isPrimary = false;

    @Column(nullable = false)
    Integer sortOrder = 0;

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (isPrimary == null)
            isPrimary = false;
        if (sortOrder == null)
            sortOrder = 0;
    }
}
