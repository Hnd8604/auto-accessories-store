package app.store.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category extends BaseEntityLong {
    String name;
    @Column(columnDefinition = "TEXT")
    String description;
    @OneToMany(mappedBy ="category", cascade = CascadeType.ALL)
    List<Product> products;
    @ManyToMany
    @JoinTable(
            name = "category_brand",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "brand_id")
    )
    Set<Brand> brands;
    @Column(unique = true, nullable = false, length = 255)
    String slug;
}
