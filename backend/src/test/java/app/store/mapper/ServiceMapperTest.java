package app.store.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import app.store.dto.response.ServiceResponse;
import app.store.entity.ProfessionalService;
import app.store.entity.ServiceImage;

// Dùng mapper thật do MapStruct sinh ra, không mock
public class ServiceMapperTest {

    ServiceMapper serviceMapper = Mappers.getMapper(ServiceMapper.class);

    private ServiceImage image(String url, Boolean primary) {
        ServiceImage img = new ServiceImage();
        img.setImageUrl(url);
        img.setIsPrimary(primary);
        return img;
    }

    private ProfessionalService serviceWithImages(List<ServiceImage> images) {
        ProfessionalService service = new ProfessionalService();
        service.setId(1L);
        service.setName("Dán phim cách nhiệt");
        service.setImages(images);
        return service;
    }

    @Test
    void toServiceResponse_shouldPickPrimaryImage() {
        ProfessionalService service = serviceWithImages(
                List.of(image("a.png", false), image("b.png", true)));

        ServiceResponse response = serviceMapper.toServiceResponse(service);

        assertThat(response.primaryImageUrl()).isEqualTo("b.png");
    }

    @Test
    void toServiceResponse_shouldFallbackToFirstImage_whenNoPrimary() {
        ProfessionalService service = serviceWithImages(
                List.of(image("a.png", false), image("b.png", null)));

        ServiceResponse response = serviceMapper.toServiceResponse(service);

        assertThat(response.primaryImageUrl()).isEqualTo("a.png");
    }

    @Test
    void toServiceResponse_shouldReturnNullImage_whenNoImages() {
        ProfessionalService service = serviceWithImages(List.of());

        ServiceResponse response = serviceMapper.toServiceResponse(service);

        assertThat(response.primaryImageUrl()).isNull();
    }
}
