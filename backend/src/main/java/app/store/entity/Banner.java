package app.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Banner extends BaseEntityLong {
    String title;

    String subtitle;

    @Column(columnDefinition = "TEXT")
    String imageUrl;

    String redirectUrl;

    String altText;

    String buttonText;

    Integer displayOrder;

    @Builder.Default
    Boolean isActive = true;
}
