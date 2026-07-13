package app.store.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostCategoryResponse {
    Long id;
    String name;
    String slug;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Long postCount;
}
