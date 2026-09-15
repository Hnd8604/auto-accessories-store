package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record BannerRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        String title,
        String subtitle,
        String redirectUrl,
        String altText,
        String buttonText,
        Integer displayOrder,
        Boolean isActive
) {
}
