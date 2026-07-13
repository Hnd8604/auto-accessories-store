package app.store.mapper;

import app.store.dto.request.BannerRequest;
import app.store.dto.response.BannerResponse;
import app.store.entity.Banner;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BannerMapper {
    BannerResponse toBannerResponse(Banner banner);

    Banner toBanner(BannerRequest request);

    void updateBannerFromRequest(BannerRequest request, @MappingTarget Banner banner);
}
