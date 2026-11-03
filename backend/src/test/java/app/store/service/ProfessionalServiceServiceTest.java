package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.store.dto.request.ServiceRequest;
import app.store.dto.response.ServiceResponse;
import app.store.entity.ProfessionalService;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.ServiceMapper;
import app.store.repository.ServiceRepository;
import app.store.utils.SlugUtil;

@ExtendWith(MockitoExtension.class)
public class ProfessionalServiceServiceTest {

    @Mock
    ServiceRepository serviceRepository;
    @Mock
    ServiceMapper serviceMapper;
    @Mock
    SlugUtil slugUtil;
    @InjectMocks
    ProfessionalServiceService professionalServiceService;

    private ProfessionalService buildService(Long id, String name) {
        ProfessionalService service = new ProfessionalService();
        service.setId(id);
        service.setName(name);
        service.setSlug("dan-phim-cach-nhiet");
        return service;
    }

    // ==================== getAllServices ====================

    // Chọn ảnh primary nằm trong ServiceMapper -> test ở ServiceMapperTest
    @Test
    void getAllServices_shouldMapEachService() {
        ProfessionalService first = buildService(1L, "Dán phim cách nhiệt");
        ProfessionalService second = buildService(2L, "Rửa xe");
        ServiceResponse firstResponse = ServiceResponse.builder().build();
        ServiceResponse secondResponse = ServiceResponse.builder().build();

        when(serviceRepository.findAllWithImages()).thenReturn(List.of(first, second));
        when(serviceMapper.toServiceResponse(first)).thenReturn(firstResponse);
        when(serviceMapper.toServiceResponse(second)).thenReturn(secondResponse);

        var result = professionalServiceService.getAllServices();

        assertThat(result).containsExactly(firstResponse, secondResponse);
    }

    // ==================== getServiceById / BySlug ====================

    @Test
    void getServiceById_shouldThrow_whenNotFound() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalServiceService.getServiceById(99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_NOT_EXISTED);

        verify(serviceMapper, never()).toServiceResponse(any());
    }

    @Test
    void getServiceBySlug_shouldThrow_whenNotFound() {
        when(serviceRepository.findBySlug("khong-ton-tai")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalServiceService.getServiceBySlug("khong-ton-tai"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_NOT_EXISTED);
    }

    // ==================== createService ====================

    @Test
    void createService_shouldSetUniqueSlug() {
        ServiceRequest request = ServiceRequest.builder()
                .name("Dán phim cách nhiệt")
                .build();
        ProfessionalService mapped = new ProfessionalService();

        when(serviceMapper.toService(request)).thenReturn(mapped);
        when(slugUtil.toSlug("Dán phim cách nhiệt")).thenReturn("dan-phim-cach-nhiet");
        when(slugUtil.createUniqueSlug(eq("dan-phim-cach-nhiet"), any()))
                .thenReturn("dan-phim-cach-nhiet-1");
        when(serviceRepository.save(mapped)).thenReturn(mapped);
        when(serviceMapper.toServiceResponse(mapped)).thenReturn(ServiceResponse.builder().build());

        professionalServiceService.createService(request);

        assertThat(mapped.getSlug()).isEqualTo("dan-phim-cach-nhiet-1");
    }

    // ==================== updateService ====================

    @Test
    void updateService_shouldRegenerateSlug_whenNameChanged() {
        ProfessionalService service = buildService(1L, "Tên cũ");
        ServiceRequest request = ServiceRequest.builder()
                .name("Tên mới").build();

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(slugUtil.toSlug("Tên mới")).thenReturn("ten-moi");
        when(slugUtil.createUniqueSlug(eq("ten-moi"), any())).thenReturn("ten-moi");
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toServiceResponse(service)).thenReturn(ServiceResponse.builder().build());

        professionalServiceService.updateService(1L, request);

        assertThat(service.getSlug()).isEqualTo("ten-moi");
        verify(serviceMapper).updateService(service, request);
    }

    @Test
    void updateService_shouldKeepSlug_whenNameUnchanged() {
        ProfessionalService service = buildService(1L, "Dán phim cách nhiệt");
        ServiceRequest request = ServiceRequest.builder()
                .name("Dán phim cách nhiệt").build();

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toServiceResponse(service)).thenReturn(ServiceResponse.builder().build());

        professionalServiceService.updateService(1L, request);

        assertThat(service.getSlug()).isEqualTo("dan-phim-cach-nhiet");
        verify(slugUtil, never()).toSlug(any());
    }

    @Test
    void updateService_slugChecker_shouldIgnoreOwnSlug() {
        ProfessionalService service = buildService(1L, "Tên cũ");
        service.setSlug("ten-moi"); // slug hiện tại trùng slug mới sinh ra
        ServiceRequest request = ServiceRequest.builder().name("Tên mới").build();

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(slugUtil.toSlug("Tên mới")).thenReturn("ten-moi");
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toServiceResponse(service)).thenReturn(ServiceResponse.builder().build());
        // Bắt lấy hàm kiểm tra trùng slug mà service truyền vào để gọi thử
        when(slugUtil.createUniqueSlug(eq("ten-moi"), any())).thenAnswer(inv -> {
            Function<String, Boolean> existsChecker = inv.getArgument(1);
            assertThat(existsChecker.apply("ten-moi")).isFalse(); // slug của chính nó -> không tính là trùng
            return "ten-moi";
        });

        professionalServiceService.updateService(1L, request);

        assertThat(service.getSlug()).isEqualTo("ten-moi");
    }

    @Test
    void updateService_shouldThrow_whenNotFound() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalServiceService.updateService(99L,
                ServiceRequest.builder().name("x").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_NOT_EXISTED);
    }

    // ==================== deleteService ====================

    @Test
    void deleteService_shouldDelete_whenFound() {
        ProfessionalService service = buildService(1L, "Rửa xe");
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));

        professionalServiceService.deleteService(1L);

        verify(serviceRepository).delete(service);
    }

    @Test
    void deleteService_shouldThrow_whenNotFound() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalServiceService.deleteService(99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_NOT_EXISTED);

        verify(serviceRepository, never()).delete(any());
    }
}
