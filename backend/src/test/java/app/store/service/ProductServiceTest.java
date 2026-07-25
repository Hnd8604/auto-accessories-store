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

import app.store.dto.request.ProductRequest;
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
import app.store.utils.SlugUtil;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    ProductMapper productMapper;
    @Mock
    ProductRepository productRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    BrandRepository brandRepository;
    @Mock
    SlugUtil slugUtil;
    @InjectMocks
    ProductService productService;

    @Test
    void createProduct_shouldSetSlugAndCategory_happyPath() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setName("Đèn LED");
        request.setCategoryId(1L);
        // brandId để NULL -> nhánh brand bị bỏ qua

        Category category = new Category();
        category.setId(1L);
        category.setName("Đèn");
        Product mappedProduct = new Product();

        when(productMapper.toProduct(request)).thenReturn(mappedProduct);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(slugUtil.toSlug("Đèn LED")).thenReturn("den-led");
        when(slugUtil.createUniqueSlug(eq("den-led"), any())).thenReturn("den-led");
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toProductResponse(any(Product.class))).thenReturn(new ProductResponse());

        // Act
        productService.createProduct(request);

        // Assert
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("den-led");
        assertThat(captor.getValue().getCategory()).isEqualTo(category);
    }

    @Test
    void createProduct_shouldThrow_whenCategoryNotFound() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setCategoryId(99L);
        when(productMapper.toProduct(request)).thenReturn(new Product());
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_EXISTED);

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_shouldThrow_whenBrandNotInCategory() {
        ProductRequest request = new ProductRequest();
        request.setCategoryId(1L);
        request.setBrandId(5L);

        Category category = new Category();
        category.setId(1L);
        Brand brand = new Brand();
        brand.setId(5L);

        when(productMapper.toProduct(request)).thenReturn(new Product());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(5L)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByIdAndCategoriesId(5L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BRAND_NOT_IN_CATEGORY);
    }

    @Test
    void getProductById_shouldThrow_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_EXISTED);

        verify(productMapper, never()).toProductResponse(any());
    }

    @Test
    void getProductById_shouldReturnResponse_whenFound() {
        Product product = new Product();
        product.setId(1L);
        ProductResponse response = new ProductResponse();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toProductResponse(product)).thenReturn(response);

        assertThat(productService.getProductById(1L)).isSameAs(response); // isSameAs là kiểm tra xem đúng địa chỉ
                                                                          // bộ nhớ chứ không quan tâm đến giá trị
    }
}
