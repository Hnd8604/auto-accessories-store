package app.store.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class ProfessionalService extends BaseEntityLong {

    String name;

    @Column(columnDefinition = "TEXT")
    String shortDescription;

    @Column(columnDefinition = "TEXT")
    String fullDescription;

    @Column(columnDefinition = "TEXT")
    String features;

    String slug;

    Integer displayOrder;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ServiceImage> images;
}
