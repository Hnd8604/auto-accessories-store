package app.store.mapper;

import app.store.dto.request.ServiceRequest;
import app.store.dto.response.ServiceResponse;
import app.store.entity.ProfessionalService;
import app.store.entity.ServiceImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    @Mapping(target = "images", ignore = true)
    @Mapping(target = "slug", ignore = true)
    ProfessionalService toService(ServiceRequest request);

    @Mapping(target = "primaryImageUrl", source = "images", qualifiedByName = "primaryImageUrl")
    ServiceResponse toServiceResponse(ProfessionalService service);

    // Ảnh có isPrimary = true, nếu không có thì lấy ảnh đầu tiên
    @Named("primaryImageUrl")
    default String primaryImageUrl(List<ServiceImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .orElse(images.get(0))
                .getImageUrl();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateService(@MappingTarget ProfessionalService service, ServiceRequest request);
}
