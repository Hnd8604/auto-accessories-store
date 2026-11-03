package app.store.service;

import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    Cloudinary cloudinary;
    @Mock
    MultipartFile file;
    @InjectMocks
    CloudinaryService cloudinaryService;

    @Test
    void uploadImage_wrapsIOExceptionWithoutLeakingProviderMessage() throws Exception {
        IOException cause = new IOException("provider-secret-detail");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenThrow(cause);

        assertThatThrownBy(() -> cloudinaryService.uploadImage(file, "store/products"))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IMAGE_STORAGE_ERROR);
                    assertThat(exception.getMessage()).isEqualTo(ErrorCode.IMAGE_STORAGE_ERROR.getMessage());
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }

    @Test
    void uploadImage_rejectsEmptyFile() {
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> cloudinaryService.uploadImage(file, "store/products"))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
                    assertThat(exception.getMessage()).isEqualTo("File ảnh không được để trống");
                });

        verifyNoInteractions(cloudinary);
    }

    @Test
    void uploadImage_rejectsNonImageFile() {
        when(file.getContentType()).thenReturn("application/pdf");

        assertThatThrownBy(() -> cloudinaryService.uploadImage(file, "store/products"))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
                    assertThat(exception.getMessage()).isEqualTo("File tải lên phải là ảnh");
                });

        verifyNoInteractions(cloudinary);
    }

    @Test
    void uploadImage_rejectsFolderOutsideStore() {
        when(file.getContentType()).thenReturn("image/png");

        assertThatThrownBy(() -> cloudinaryService.uploadImage(file, "../products"))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
                    assertThat(exception.getMessage())
                            .isEqualTo("Thư mục lưu ảnh không hợp lệ: ../products");
                });

        verifyNoInteractions(cloudinary);
    }
}
