package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

@Builder
public record PostCategoryRequest(
        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 255, message = "Tên danh mục không được vượt quá 255 ký tự")
        String name,
        @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
        String description
) {
}
