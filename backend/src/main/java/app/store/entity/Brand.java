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
public class Brand extends BaseEntityLong{
    String name;

    @Column(columnDefinition = "TEXT")
    String description;
    @OneToMany(mappedBy ="brand", cascade = CascadeType.ALL)
    List<Product> products;

    @ManyToMany(mappedBy = "brands")
    Set<Category> categories;

    @Column(unique = true, nullable = false, length = 255)
    String slug;
}
