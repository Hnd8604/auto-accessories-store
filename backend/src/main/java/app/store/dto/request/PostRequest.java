package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

@Builder
public record PostRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title must not exceed {max} characters")
        String title,
        @Size(max = 1000, message = "Short description must not exceed {max} characters")
        String shortDescription,
        @NotBlank(message = "Content is required")
        String content,
        @NotNull(message = "Published status is required")
        Boolean published,
        Long categoryId
) {
}
