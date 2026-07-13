package app.store.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceImageResponse {
    Long id;
    Long serviceId;
    String imageUrl;
    String altText;
    Boolean isPrimary;
    Integer sortOrder;
}
