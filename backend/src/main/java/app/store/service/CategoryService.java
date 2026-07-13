package app.store.service;

import app.store.dto.request.CategoryRequest;
import app.store.dto.response.BrandResponse;
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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CategoryService {
    CategoryMapper categoryMapper;
    CategoryRepository categoryRepository;
    BrandRepository brandRepository;
    BrandMapper brandMapper;
    SlugUtil slugUtil;
    @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = categoryMapper.toCategory(request);
        // Tạo slug từ tên danh mục
        String baseSlug = slugUtil.toSlug(request.getName());
        String uniqueSlug = slugUtil.createUniqueSlug(baseSlug, categoryRepository::existsBySlug);

        category.setSlug(uniqueSlug);
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }
    public CategoryResponse getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(categoryMapper::toCategoryResponse)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
    }
    public CategoryResponse getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(categoryMapper::toCategoryResponse)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
    }
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

        public List<BrandResponse> getBrandsByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        return brandRepository.findByCategoriesId(category.getId())
            .stream()
            .map(brandMapper::toBrandResponse)
            .toList();
        }

        @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
        public List<BrandResponse> updateCategoryBrands(Long categoryId, List<Long> brandIds) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        List<Brand> brands = brandIds == null ? List.of() : brandRepository.findAllById(brandIds);
        if (brandIds != null && brands.size() != brandIds.size()) {
            throw new AppException(ErrorCode.BRAND_NOT_EXISTED);
        }

        Set<Brand> brandSet = new HashSet<>(brands);
        category.setBrands(brandSet);
        Category saved = categoryRepository.save(category);

        return saved.getBrands() == null ? List.of() : saved.getBrands().stream()
            .map(brandMapper::toBrandResponse)
            .toList();
        }
    @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        // Cập nhật slug nếu tên thay đổi
        if (!category.getName().equals(request.getName())) {
            String baseSlug = slugUtil.toSlug(request.getName());
            String uniqueSlug = slugUtil.createUniqueSlug(baseSlug, slug ->
                    !slug.equals(category.getSlug()) && categoryRepository.existsBySlug(slug));
            category.setSlug(uniqueSlug);
        }
        categoryMapper.updateCategory(category, request);
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }
    @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
        categoryRepository.deleteById(categoryId);

    }
}
