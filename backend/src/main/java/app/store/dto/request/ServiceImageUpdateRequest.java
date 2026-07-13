package app.store.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceImageUpdateRequest {
    String altText;
    Boolean isPrimary;
    Integer sortOrder;
}
