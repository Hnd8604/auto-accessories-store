package app.store.service;

import app.store.dto.request.ProductRequest;
import app.store.dto.request.ProductSearchRequest;
import app.store.dto.response.ProductResponse;
import app.store.entity.Brand;
import app.store.entity.Category;
import app.store.entity.Product;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.ProductMapper;
import app.store.repository.BrandRepository;
import app.store.repository.CategoryRepository;
import app.store.repository.ProductRepository;
import app.store.specification.ProductSpecification;
import app.store.utils.SlugUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductService {
    ProductMapper productMapper;
    ProductRepository productRepository;
    CategoryRepository categoryRepository;
    BrandRepository brandRepository;
    SlugUtil slugUtil;
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toProduct(request);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_EXISTED));
            if (!brandRepository.existsByIdAndCategoriesId(brand.getId(), category.getId())) {
                throw new AppException(ErrorCode.BRAND_NOT_IN_CATEGORY);
            }
        }
        String baseSlug = slugUtil.toSlug(request.getName());
        String uniqueSlug = slugUtil.createUniqueSlug(baseSlug, productRepository::existsBySlug);

        product.setSlug(uniqueSlug);
        product.setCategory(category);
        product.setBrand(brand);
        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        Product product = productRepository.findById(productId).
                orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

                productMapper.updateProduct(product, request);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_EXISTED));
            if (!brandRepository.existsByIdAndCategoriesId(brand.getId(), category.getId())) {
                throw new AppException(ErrorCode.BRAND_NOT_IN_CATEGORY);
            }
        }

        // Cập nhật slug nếu tiêu đề thay đổi
        if (!product.getName().equals(request.getName())) {
            String baseSlug = slugUtil.toSlug(request.getName());
            String uniqueSlug = slugUtil.createUniqueSlug(baseSlug, slug ->
                    !slug.equals(product.getSlug()) && productRepository.existsBySlug(slug));
            product.setSlug(uniqueSlug);
        }

        product.setCategory(category);
        product.setBrand(brand);

        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId).
                orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

        productRepository.delete(product);

    }
    @Transactional
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId).
                orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

        return productMapper.toProductResponse(product);
    }

    @Transactional
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug).
                orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

        return productMapper.toProductResponse(product);
    }
    public Page<ProductResponse> getProductsByCategoryId(Long categoryId, Pageable pageable) {
        return productRepository.findProductsByCategoryId(categoryId, pageable)
                .map(productMapper::toProductResponse);
    }
    public Page<ProductResponse> getProductsByBrandId(Long brandId, Pageable pageable) {
        return productRepository.findProductsByBrandId(brandId, pageable)
                .map(productMapper::toProductResponse);
    }
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAllWithImages(pageable)
                .map(product -> {
                    ProductResponse response = productMapper.toProductResponse(product);
                    // Set primaryImageUrl from productImages (since MapStruct @AfterMapping doesn't work with interface default methods)
                    if (product.getProductImages() != null) {
                        product.getProductImages().stream()
                                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                .findFirst()
                                .ifPresent(img -> response.setPrimaryImageUrl(img.getImageUrl()));
                    }
                    return response;
                });
    }
    public Page<ProductResponse> search(ProductSearchRequest req, Pageable pageable) {
        Specification<Product> spec = ProductSpecification.toSpecification(req);
        return productRepository.findAll(spec, pageable)
                .map(productMapper::toProductResponse);
    }
}
