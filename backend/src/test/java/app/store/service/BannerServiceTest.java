package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import app.store.dto.request.BannerRequest;
import app.store.dto.response.BannerResponse;
import app.store.entity.Banner;
import app.store.mapper.BannerMapper;
import app.store.repository.BannerRepository;

@ExtendWith(MockitoExtension.class)
public class BannerServiceTest {

    @Mock
    BannerRepository bannerRepository;
    @Mock
    BannerMapper bannerMapper;
    @Mock
    CloudinaryService cloudinaryService;
    @InjectMocks
    BannerService bannerService;

    private final MultipartFile file =
            new MockMultipartFile("file", "banner.png", "image/png", "bytes".getBytes());

    @Test
    void createBanner_shouldUploadImage_andDefaultIsActiveToTrue() {
        BannerRequest request = BannerRequest.builder().title("Khuyến mãi").build();
        Banner mapped = new Banner();
        mapped.setIsActive(null); // mapper không set -> service phải tự bật

        when(bannerMapper.toBanner(request)).thenReturn(mapped);
        when(cloudinaryService.uploadImage(file, "store/banners")).thenReturn("http://cloud/banner.png");
        when(bannerRepository.save(mapped)).thenReturn(mapped);
        when(bannerMapper.toBannerResponse(mapped)).thenReturn(new BannerResponse());

        bannerService.createBanner(file, request);

        assertThat(mapped.getImageUrl()).isEqualTo("http://cloud/banner.png");
        assertThat(mapped.getIsActive()).isTrue();
    }

    @Test
    void createBanner_shouldRespectExplicitIsActiveFalse() {
        BannerRequest request = BannerRequest.builder().title("Khuyến mãi").isActive(false).build();
        Banner mapped = new Banner();
        mapped.setIsActive(false);

        when(bannerMapper.toBanner(request)).thenReturn(mapped);
        when(cloudinaryService.uploadImage(file, "store/banners")).thenReturn("http://cloud/banner.png");
        when(bannerRepository.save(mapped)).thenReturn(mapped);
        when(bannerMapper.toBannerResponse(mapped)).thenReturn(new BannerResponse());

        bannerService.createBanner(file, request);

        assertThat(mapped.getIsActive()).isFalse();
    }

    @Test
    void updateBanner_shouldUploadNewImage_whenFileProvided() {
        Banner banner = new Banner();
        banner.setImageUrl("http://cloud/old.png");
        BannerRequest request = BannerRequest.builder().title("Tiêu đề mới").build();

        when(bannerRepository.findById(1L)).thenReturn(Optional.of(banner));
        when(cloudinaryService.uploadImage(file, "store/banners")).thenReturn("http://cloud/new.png");
        when(bannerRepository.save(banner)).thenReturn(banner);
        when(bannerMapper.toBannerResponse(banner)).thenReturn(new BannerResponse());

        bannerService.updateBanner(1L, file, request);

        assertThat(banner.getImageUrl()).isEqualTo("http://cloud/new.png");
        verify(bannerMapper).updateBannerFromRequest(request, banner);
    }

    @Test
    void updateBanner_shouldKeepOldImage_whenNoFile() {
        Banner banner = new Banner();
        banner.setImageUrl("http://cloud/old.png");
        BannerRequest request = BannerRequest.builder().title("Tiêu đề mới").build();

        when(bannerRepository.findById(1L)).thenReturn(Optional.of(banner));
        when(bannerRepository.save(banner)).thenReturn(banner);
        when(bannerMapper.toBannerResponse(banner)).thenReturn(new BannerResponse());

        bannerService.updateBanner(1L, null, request);

        assertThat(banner.getImageUrl()).isEqualTo("http://cloud/old.png");
        verify(cloudinaryService, never()).uploadImage(any(), any());
    }

    @Test
    void updateBanner_shouldKeepOldImage_whenFileEmpty() {
        Banner banner = new Banner();
        banner.setImageUrl("http://cloud/old.png");
        MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        when(bannerRepository.findById(1L)).thenReturn(Optional.of(banner));
        when(bannerRepository.save(banner)).thenReturn(banner);
        when(bannerMapper.toBannerResponse(banner)).thenReturn(new BannerResponse());

        bannerService.updateBanner(1L, emptyFile, BannerRequest.builder().build());

        verify(cloudinaryService, never()).uploadImage(any(), any());
    }

    @Test
    void updateBanner_shouldThrow_whenNotFound() {
        when(bannerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bannerService.updateBanner(99L, file, BannerRequest.builder().build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy banner");
    }

    @Test
    void deleteBanner_shouldDelete_whenFound() {
        Banner banner = new Banner();
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(banner));

        bannerService.deleteBanner(1L);

        verify(bannerRepository).delete(banner);
    }

    @Test
    void deleteBanner_shouldThrow_whenNotFound() {
        when(bannerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bannerService.deleteBanner(99L))
                .isInstanceOf(RuntimeException.class);

        verify(bannerRepository, never()).delete(any());
    }

    @Test
    void getBannerById_shouldThrow_whenNotFound() {
        when(bannerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bannerService.getBannerById(99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAllBanners_shouldUseDisplayOrderQuery() {
        Banner banner = new Banner();

        when(bannerRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(banner));
        when(bannerMapper.toBannerResponse(banner)).thenReturn(new BannerResponse());

        assertThat(bannerService.getAllBanners()).hasSize(1);
    }

    @Test
    void getActiveBanners_shouldUseActiveOnlyQuery() {
        Banner banner = new Banner();

        when(bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(banner));
        when(bannerMapper.toBannerResponse(banner)).thenReturn(new BannerResponse());

        assertThat(bannerService.getActiveBanners()).hasSize(1);
    }
}
