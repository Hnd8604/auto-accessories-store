package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BannerRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    String title;

    String subtitle;

    String redirectUrl;

    String altText;

    String buttonText;

    Integer displayOrder;

    Boolean isActive;
}
