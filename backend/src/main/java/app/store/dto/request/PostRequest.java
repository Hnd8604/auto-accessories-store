package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostRequest {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 500, message = "Tiêu đề không được vượt quá 500 ký tự")
    String title;
    
    @Size(max = 1000, message = "Mô tả ngắn không được vượt quá 1000 ký tự")
    String shortDescription;
    
    @NotBlank(message = "Nội dung không được để trống")
    String content;
    
    @NotNull(message = "Trạng thái xuất bản không được để trống")
    Boolean published;
    
    Long categoryId;
}
