package app.store.mapper;

import app.store.dto.request.ServiceRequest;
import app.store.dto.response.ServiceResponse;
import app.store.entity.ProfessionalService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    @Mapping(target = "images", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "features", ignore = true)
    ProfessionalService toService(ServiceRequest request);

    @Mapping(target = "primaryImageUrl", ignore = true)
    @Mapping(target = "features", ignore = true)
    ServiceResponse toServiceResponse(ProfessionalService service);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "features", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateService(@MappingTarget ProfessionalService service, ServiceRequest request);
}
