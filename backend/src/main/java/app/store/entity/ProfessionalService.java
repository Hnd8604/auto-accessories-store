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

    @Column(nullable = false)
    String name;

    @Column(columnDefinition = "TEXT")
    String shortDescription;

    @Column(columnDefinition = "TEXT")
    String fullDescription;

    @Column(nullable = false)
    String slug;

    @Column(nullable = false)
    Integer displayOrder = 0;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ServiceImage> images;

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (displayOrder == null)
            displayOrder = 0;
    }
}
