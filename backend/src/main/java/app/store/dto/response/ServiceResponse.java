package app.store.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
    List<String> features;
    String slug;
    Integer displayOrder;
    String primaryImageUrl;
}
