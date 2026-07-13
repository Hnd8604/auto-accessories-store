package app.store.service;

import app.store.dto.request.BannerRequest;
import app.store.dto.response.BannerResponse;
import app.store.entity.Banner;
import app.store.mapper.BannerMapper;
import app.store.repository.BannerRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class BannerService {

    BannerRepository bannerRepository;
    BannerMapper bannerMapper;
    CloudinaryService cloudinaryService;

    @PreAuthorize("hasAuthority('BANNER_CREATE')")
    public BannerResponse createBanner(MultipartFile file, BannerRequest request) {
        Banner banner = bannerMapper.toBanner(request);

        String imageUrl = cloudinaryService.uploadImage(file, "store/banners");
        banner.setImageUrl(imageUrl);

        if (banner.getIsActive() == null) {
            banner.setIsActive(true);
        }

        return bannerMapper.toBannerResponse(bannerRepository.save(banner));
    }

    @PreAuthorize("hasAuthority('BANNER_UPDATE')")
    public BannerResponse updateBanner(Long bannerId, MultipartFile file, BannerRequest request) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với ID: " + bannerId));

        // Apply field updates from request
        bannerMapper.updateBannerFromRequest(request, banner);

        // Upload new image if provided
        if (file != null && !file.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(file, "store/banners");
            banner.setImageUrl(imageUrl);
        }

        return bannerMapper.toBannerResponse(bannerRepository.save(banner));
    }

    @PreAuthorize("hasAuthority('BANNER_DELETE')")
    public void deleteBanner(Long bannerId) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với ID: " + bannerId));
        bannerRepository.delete(banner);
    }

    @Transactional(readOnly = true)
    public BannerResponse getBannerById(Long bannerId) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với ID: " + bannerId));
        return bannerMapper.toBannerResponse(banner);
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(bannerMapper::toBannerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(bannerMapper::toBannerResponse)
                .toList();
    }
}
