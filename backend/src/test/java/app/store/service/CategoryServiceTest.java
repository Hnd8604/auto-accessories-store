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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.store.dto.request.CategoryRequest;
import app.store.dto.response.CategoryResponse;
import app.store.entity.Brand;
import app.store.entity.Category;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.BrandMapper;
import app.store.mapper.CategoryMapper;
import app.store.repository.BrandRepository;
import app.store.repository.CategoryRepository;
import app.store.utils.SlugUtil;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {
    @Mock
    CategoryMapper categoryMapper;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    BrandRepository brandRepository;
    @Mock
    BrandMapper brandMapper;
    @Mock
    SlugUtil slugUtil;
    @InjectMocks
    CategoryService categoryService;

    @Test
    void createCategory_shouldSetSlug_happyPath() {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setName("Đèn xe");
        Category mappedCategory = new Category();

        when(categoryMapper.toCategory(request)).thenReturn(mappedCategory);
        when(slugUtil.toSlug("Đèn xe")).thenReturn("den-xe");
        when(slugUtil.createUniqueSlug(eq("den-xe"), any())).thenReturn("den-xe");
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toCategoryResponse(any(Category.class))).thenReturn(new CategoryResponse());

        // Act
        categoryService.createCategory(request);

        // Assert
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("den-xe");
    }

    @Test
    void getCategoryById_shouldThrow_whenNotFound() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_EXISTED);
    }

    @Test
    void getCategoryById_shouldReturnResponse_whenFound() {
        // Arrange
        Category category = new Category();
        category.setId(1L);
        CategoryResponse response = new CategoryResponse();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toCategoryResponse(category)).thenReturn(response);

        // Act + Assert
        assertThat(categoryService.getCategoryById(1L)).isSameAs(response);
    }

    @Test
    void deleteCategory_shouldThrow_whenNotFound() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_EXISTED);

        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void updateCategoryBrands_shouldThrow_whenSomeBrandIdsNotFound() {
        // Arrange: yêu cầu gán 2 brand nhưng repository chỉ tìm thấy 1
        Category category = new Category();
        category.setId(1L);
        Brand brand = new Brand();
        brand.setId(10L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(brand));

        // Act + Assert
        assertThatThrownBy(() -> categoryService.updateCategoryBrands(1L, List.of(10L, 20L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BRAND_NOT_EXISTED);

        verify(categoryRepository, never()).save(any());
    }
}
