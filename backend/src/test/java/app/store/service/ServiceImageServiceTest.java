package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import app.store.dto.request.ServiceImageRequest;
import app.store.dto.request.ServiceImageUpdateRequest;
import app.store.dto.response.ServiceImageResponse;
import app.store.entity.ProfessionalService;
import app.store.entity.ServiceImage;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.ServiceImageMapper;
import app.store.repository.ServiceImageRepository;
import app.store.repository.ServiceRepository;

@ExtendWith(MockitoExtension.class)
public class ServiceImageServiceTest {

    @Mock
    ServiceImageRepository serviceImageRepository;
    @Mock
    ServiceImageMapper serviceImageMapper;
    @Mock
    ServiceRepository serviceRepository;
    @Mock
    CloudinaryService cloudinaryService;
    @InjectMocks
    ServiceImageService serviceImageService;

    private final MultipartFile file =
            new MockMultipartFile("file", "anh.png", "image/png", "fake-bytes".getBytes());

    private ProfessionalService buildService(Long id) {
        ProfessionalService service = new ProfessionalService();
        service.setId(id);
        return service;
    }

    private ServiceImage buildImage(Long id, ProfessionalService service) {
        ServiceImage image = new ServiceImage();
        image.setId(id);
        image.setService(service);
        image.setImageUrl("http://cloud/old.png");
        return image;
    }

    @Test
    void createServiceImage_shouldUploadAndLinkService() {
        ServiceImageRequest request = ServiceImageRequest.builder().serviceId(1L).build();
        ProfessionalService service = buildService(1L);
        ServiceImage mapped = new ServiceImage();

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(cloudinaryService.uploadImage(file, "store/services")).thenReturn("http://cloud/new.png");
        when(serviceImageMapper.toServiceImage(request)).thenReturn(mapped);
        when(serviceImageRepository.save(mapped)).thenReturn(mapped);
        when(serviceImageMapper.toServiceImageResponse(mapped)).thenReturn(new ServiceImageResponse());

        serviceImageService.createServiceImage(file, request);

        assertThat(mapped.getService()).isSameAs(service);
        assertThat(mapped.getImageUrl()).isEqualTo("http://cloud/new.png");
    }

    @Test
    void createServiceImage_shouldThrow_whenServiceNotFound() {
        ServiceImageRequest request = ServiceImageRequest.builder().serviceId(99L).build();

        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceImageService.createServiceImage(file, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_NOT_EXISTED);

        verify(cloudinaryService, never()).uploadImage(any(), any());
    }

    @Test
    void getImagesByServiceId_shouldMapEveryImage() {
        ServiceImage image = buildImage(1L, buildService(1L));

        when(serviceImageRepository.findByServiceId(1L)).thenReturn(List.of(image));
        when(serviceImageMapper.toServiceImageResponse(image)).thenReturn(new ServiceImageResponse());

        assertThat(serviceImageService.getImagesByServiceId(1L)).hasSize(1);
    }

    @Test
    void updateServiceImage_shouldThrow_whenNotFound() {
        when(serviceImageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceImageService.updateServiceImage(99L,
                ServiceImageUpdateRequest.builder().build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_IMAGE_NOT_EXISTED);
    }

    @Test
    void deleteServiceImage_shouldRemoveFromCloudinaryThenDb() {
        ServiceImage image = buildImage(1L, buildService(1L));
        when(serviceImageRepository.findById(1L)).thenReturn(Optional.of(image));

        serviceImageService.deleteServiceImage(1L);

        var order = inOrder(cloudinaryService, serviceImageRepository);
        order.verify(cloudinaryService).deleteImage("http://cloud/old.png");
        order.verify(serviceImageRepository).delete(image);
    }

    @Test
    void setPrimaryImage_shouldResetOthersThenSetNew() {
        ServiceImage image = buildImage(1L, buildService(10L));

        when(serviceRepository.existsById(10L)).thenReturn(true);
        when(serviceImageRepository.findById(1L)).thenReturn(Optional.of(image));

        serviceImageService.setPrimaryImage(10L, 1L);

        var order = inOrder(serviceImageRepository);
        order.verify(serviceImageRepository).resetAllPrimaryImagesForService(10L);
        order.verify(serviceImageRepository).setNewPrimaryImage(1L, 10L);
    }

    @Test
    void setPrimaryImage_shouldThrow_whenServiceNotExisted() {
        when(serviceRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> serviceImageService.setPrimaryImage(99L, 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_NOT_EXISTED);
    }

    @Test
    void setPrimaryImage_shouldThrow_whenImageBelongsToAnotherService() {
        ServiceImage image = buildImage(1L, buildService(999L));

        when(serviceRepository.existsById(10L)).thenReturn(true);
        when(serviceImageRepository.findById(1L)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> serviceImageService.setPrimaryImage(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong");

        verify(serviceImageRepository, never()).setNewPrimaryImage(any(), any());
    }
}
