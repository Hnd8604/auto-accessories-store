package app.store.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PostCategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long postCount
) {
}
