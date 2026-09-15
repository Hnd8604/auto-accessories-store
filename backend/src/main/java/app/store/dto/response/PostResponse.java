package app.store.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PostResponse(
        Long id,
        String title,
        String slug,
        String shortDescription,
        String thumbnailUrl,
        String content,
        Boolean published,
        Long viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // Category info
        String categoryName,
        // Author info
        String authorId,
        String authorName
) {
}
