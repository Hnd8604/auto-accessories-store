package app.store.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostResponse {
    
    Long id;
    String title;
    String slug;
    String shortDescription;
    String thumbnailUrl;
    String content;
    Boolean published;
    Long viewCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    // Category info
    String categoryName;
    
    // Author info
    String authorId;
    String authorName;
}
