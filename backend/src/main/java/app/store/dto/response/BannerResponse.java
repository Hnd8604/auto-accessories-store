package app.store.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record BannerResponse(
        Long id,
        String title,
        String subtitle,
        String imageUrl,
        String redirectUrl,
        String altText,
        String buttonText,
        Integer displayOrder,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
