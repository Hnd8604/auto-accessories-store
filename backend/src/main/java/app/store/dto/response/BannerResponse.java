package app.store.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BannerResponse {
    Long id;
    String title;
    String subtitle;
    String imageUrl;
    String redirectUrl;
    String altText;
    String buttonText;
    Integer displayOrder;
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
