package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.response.ImageUploadResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;

/**
 * Upload ảnh rời — dùng cho ảnh chèn trong nội dung bài viết (RichTextEditor),
 * tức là ảnh không gắn với một entity nào để đính kèm.
 *
 * Trước đây frontend tự ký request rồi gọi thẳng Cloudinary, nên nó cần
 * CLOUDINARY_API_SECRET nằm trong bundle JavaScript — ai mở DevTools cũng đọc
 * được. Endpoint này đưa việc đó về đúng chỗ: secret ở lại server, và quyền
 * upload được kiểm soát bằng IMAGE_UPLOAD như mọi upload khác.
 */
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Image Management", description = "APIs for uploading standalone images to Cloudinary")
public class ImageController {

    // Chặn client tự chọn folder tuỳ ý trên account Cloudinary. Chỉ cho ghi
    // trong "store/", không cho ".." hay ký tự lạ.
    static Pattern ALLOWED_FOLDER = Pattern.compile("^store(/[a-z0-9][a-z0-9-]*)+$");

    CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload an image",
            description = "Uploads an image to Cloudinary and returns its URL. Requires the IMAGE_UPLOAD authority."
    )
    public ApiResponse<ImageUploadResponse> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "store/posts/content") String folder) {

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "File ảnh không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "File tải lên phải là ảnh");
        }

        if (!ALLOWED_FOLDER.matcher(folder).matches()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "Thư mục lưu ảnh không hợp lệ: " + folder);
        }

        String imageUrl = cloudinaryService.uploadImage(file, folder);
        return ApiResponse.<ImageUploadResponse>builder()
                .message(ResponseMessage.UPLOAD_IMAGE_SUCCESS)
                .result(ImageUploadResponse.builder().imageUrl(imageUrl).build())
                .build();
    }
}
