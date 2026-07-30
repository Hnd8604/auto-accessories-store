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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.store.dto.request.ServiceRequest;
import app.store.dto.response.ServiceResponse;
import app.store.entity.ProfessionalService;
import app.store.entity.ServiceImage;
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
    @Spy
    ObjectMapper objectMapper = new ObjectMapper(); // @Spy = object THẬT, không phải mock rỗng
    @InjectMocks
    ProfessionalServiceService professionalServiceService;

    private ServiceImage image(String url, Boolean primary) {
        ServiceImage img = new ServiceImage();
        img.setImageUrl(url);
        img.setIsPrimary(primary);
        return img;
    }

    private ProfessionalService buildService(Long id, String name) {
        ProfessionalService service = new ProfessionalService();
        service.setId(id);
        service.setName(name);
        service.setSlug("dan-phim-cach-nhiet");
        service.setFeatures("[\"Bảo hành 5 năm\",\"Chống UV\"]");
        return service;
    }

    // ==================== getAllServices ====================

    @Test
    void getAllServices_shouldParseFeatures_andPickPrimaryImage() {
        ProfessionalService service = buildService(1L, "Dán phim cách nhiệt");
        service.setImages(List.of(image("a.png", false), image("b.png", true)));
        ServiceResponse response = new ServiceResponse();

        when(serviceRepository.findAllWithImages()).thenReturn(List.of(service));
        when(serviceMapper.toServiceResponse(service)).thenReturn(response);

        var result = professionalServiceService.getAllServices();

        assertThat(result).hasSize(1);
        assertThat(response.getFeatures()).containsExactly("Bảo hành 5 năm", "Chống UV");
        assertThat(response.getPrimaryImageUrl()).isEqualTo("b.png");
    }

    @Test
    void getAllServices_shouldFallbackToFirstImage_whenNoPrimary() {
        ProfessionalService service = buildService(1L, "Dán phim cách nhiệt");
        service.setImages(List.of(image("a.png", false), image("b.png", false)));
        ServiceResponse response = new ServiceResponse();

        when(serviceRepository.findAllWithImages()).thenReturn(List.of(service));
        when(serviceMapper.toServiceResponse(service)).thenReturn(response);

        professionalServiceService.getAllServices();

        assertThat(response.getPrimaryImageUrl()).isEqualTo("a.png");
    }

    @Test
    void getAllServices_shouldReturnEmptyFeatures_whenJsonInvalid() {
        ProfessionalService service = buildService(1L, "Dán phim cách nhiệt");
        service.setFeatures("{json hỏng}");
        ServiceResponse response = new ServiceResponse();

        when(serviceRepository.findAllWithImages()).thenReturn(List.of(service));
        when(serviceMapper.toServiceResponse(service)).thenReturn(response);

        professionalServiceService.getAllServices();

        // JSON hỏng không được làm sập API, chỉ trả list rỗng
        assertThat(response.getFeatures()).isEmpty();
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
    void createService_shouldSerializeFeatures_andSetUniqueSlug() {
        ServiceRequest request = ServiceRequest.builder()
                .name("Dán phim cách nhiệt")
                .features(List.of("Bảo hành 5 năm", "Chống UV"))
                .build();
        ProfessionalService mapped = new ProfessionalService();

        when(serviceMapper.toService(request)).thenReturn(mapped);
        when(slugUtil.toSlug("Dán phim cách nhiệt")).thenReturn("dan-phim-cach-nhiet");
        when(slugUtil.createUniqueSlug(eq("dan-phim-cach-nhiet"), any()))
                .thenReturn("dan-phim-cach-nhiet-1");
        when(serviceRepository.save(mapped)).thenReturn(mapped);
        when(serviceMapper.toServiceResponse(mapped)).thenReturn(new ServiceResponse());

        professionalServiceService.createService(request);

        assertThat(mapped.getSlug()).isEqualTo("dan-phim-cach-nhiet-1");
        assertThat(mapped.getFeatures()).contains("Bảo hành 5 năm").contains("Chống UV");
    }

    @Test
    void createService_shouldStoreEmptyJsonArray_whenNoFeatures() {
        ServiceRequest request = ServiceRequest.builder().name("Rửa xe").features(null).build();
        ProfessionalService mapped = new ProfessionalService();

        when(serviceMapper.toService(request)).thenReturn(mapped);
        when(slugUtil.toSlug("Rửa xe")).thenReturn("rua-xe");
        when(slugUtil.createUniqueSlug(eq("rua-xe"), any())).thenReturn("rua-xe");
        when(serviceRepository.save(mapped)).thenReturn(mapped);
        when(serviceMapper.toServiceResponse(mapped)).thenReturn(new ServiceResponse());

        professionalServiceService.createService(request);

        assertThat(mapped.getFeatures()).isEqualTo("[]");
    }

    // ==================== updateService ====================

    @Test
    void updateService_shouldRegenerateSlug_whenNameChanged() {
        ProfessionalService service = buildService(1L, "Tên cũ");
        ServiceRequest request = ServiceRequest.builder()
                .name("Tên mới").features(List.of("A")).build();

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(slugUtil.toSlug("Tên mới")).thenReturn("ten-moi");
        when(slugUtil.createUniqueSlug(eq("ten-moi"), any())).thenReturn("ten-moi");
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toServiceResponse(service)).thenReturn(new ServiceResponse());

        professionalServiceService.updateService(1L, request);

        assertThat(service.getSlug()).isEqualTo("ten-moi");
        verify(serviceMapper).updateService(service, request);
    }

    @Test
    void updateService_shouldKeepSlug_whenNameUnchanged() {
        ProfessionalService service = buildService(1L, "Dán phim cách nhiệt");
        ServiceRequest request = ServiceRequest.builder()
                .name("Dán phim cách nhiệt").features(List.of("A")).build();

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toServiceResponse(service)).thenReturn(new ServiceResponse());

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
        when(serviceMapper.toServiceResponse(service)).thenReturn(new ServiceResponse());
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
