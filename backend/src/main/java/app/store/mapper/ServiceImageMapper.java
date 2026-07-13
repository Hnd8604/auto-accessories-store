package app.store.mapper;

import app.store.dto.request.ServiceImageRequest;
import app.store.dto.request.ServiceImageUpdateRequest;
import app.store.dto.response.ServiceImageResponse;
import app.store.entity.ServiceImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ServiceImageMapper {

    @Mapping(target = "serviceId", source = "service.id")
    ServiceImageResponse toServiceImageResponse(ServiceImage serviceImage);

    @Mapping(target = "service", ignore = true)
    ServiceImage toServiceImage(ServiceImageRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateServiceImage(@MappingTarget ServiceImage serviceImage, ServiceImageUpdateRequest request);
}
