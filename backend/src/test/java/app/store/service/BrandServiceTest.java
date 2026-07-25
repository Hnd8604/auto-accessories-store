package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.store.dto.request.BrandRequest;
import app.store.dto.response.BrandResponse;
import app.store.entity.Brand;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.BrandMapper;
import app.store.repository.BrandRepository;
import app.store.utils.SlugUtil;

@ExtendWith(MockitoExtension.class)
public class BrandServiceTest {
    @Mock
    BrandRepository brandRepository;
    @Mock
    BrandMapper brandMapper;
    @Mock
    SlugUtil slugUtil;
    @InjectMocks
    BrandService brandService;

    @Test
    void createBrand_shouldSetSlug_happyPath() {
        // Arrange
        BrandRequest request = new BrandRequest();
        request.setName("Honda");
        Brand mappedBrand = new Brand();

        when(brandMapper.toBrand(request)).thenReturn(mappedBrand);
        when(slugUtil.toSlug("Honda")).thenReturn("honda");
        when(slugUtil.createUniqueSlug(eq("honda"), any())).thenReturn("honda");
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(brandMapper.toBrandResponse(any(Brand.class))).thenReturn(new BrandResponse());

        // Act
        brandService.createBrand(request);

        // Assert
        ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("honda");
    }

    @Test
    void getBrandById_shouldThrow_whenNotFound() {
        // Arrange
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> brandService.getBrandById(99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BRAND_NOT_EXISTED);

        verify(brandMapper, never()).toBrandResponse(any());
    }

    @Test
    void getBrandById_shouldReturnResponse_whenFound() {
        // Arrange
        Brand brand = new Brand();
        brand.setId(1L);
        BrandResponse response = new BrandResponse();

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandMapper.toBrandResponse(brand)).thenReturn(response);

        // Act + Assert
        assertThat(brandService.getBrandById(1L)).isSameAs(response);
    }

    @Test
    void updateBrand_shouldRegenerateSlug_onlyWhenNameChanges() {
        // Arrange
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Honda");
        brand.setSlug("honda");

        BrandRequest request = new BrandRequest();
        request.setName("Honda Việt Nam");

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(slugUtil.toSlug("Honda Việt Nam")).thenReturn("honda-viet-nam");
        when(slugUtil.createUniqueSlug(eq("honda-viet-nam"), any())).thenReturn("honda-viet-nam");
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(brandMapper.toBrandResponse(any(Brand.class))).thenReturn(new BrandResponse());

        // Act
        brandService.updateBrand(1L, request);

        // Assert
        ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("honda-viet-nam");
    }

    @Test
    void deleteBrand_shouldThrow_whenNotFound() {
        // Arrange
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> brandService.deleteBrand(99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BRAND_NOT_EXISTED);

        verify(brandRepository, never()).delete(any());
    }
}
