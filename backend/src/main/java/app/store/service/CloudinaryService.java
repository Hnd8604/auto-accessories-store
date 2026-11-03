package app.store.service;

import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CloudinaryService {
    private static final Pattern ALLOWED_FOLDER =
            Pattern.compile("^store(/[a-z0-9][a-z0-9-]*)+$");

    Cloudinary cloudinary;

    @PreAuthorize("hasAuthority('IMAGE_UPLOAD')")
    public String uploadImage(MultipartFile file, String pathFolder) {
        validateImage(file);
        validateFolder(pathFolder);

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", pathFolder));
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new AppException(ErrorCode.IMAGE_STORAGE_ERROR, e);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "File ảnh không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "File tải lên phải là ảnh");
        }
    }

    private void validateFolder(String pathFolder) {
        if (pathFolder == null || !ALLOWED_FOLDER.matcher(pathFolder).matches()) {
            throw new AppException(
                    ErrorCode.INVALID_ARGUMENT,
                    "Thư mục lưu ảnh không hợp lệ: " + pathFolder);
        }
    }

    @PreAuthorize("hasAuthority('IMAGE_DELETE')")
    public void deleteImage(String imageUrl) {
        try {
            String publicId = extractPublicIdFromUrl(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary", e);
            throw new AppException(ErrorCode.IMAGE_STORAGE_ERROR, e);
        }
    }

    private String extractPublicIdFromUrl(String imageUrl) {
        String[] parts = imageUrl.split("/");
        String fileNameWithExtension = parts[parts.length - 1];
        return "products/" + fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf("."));
    }

}
