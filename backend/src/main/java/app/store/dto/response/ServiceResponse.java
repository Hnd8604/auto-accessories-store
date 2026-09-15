package app.store.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceResponse {
    Long id;
    String name;
    String shortDescription;
    String fullDescription;
    String slug;
    Integer displayOrder;
    String primaryImageUrl;
}
