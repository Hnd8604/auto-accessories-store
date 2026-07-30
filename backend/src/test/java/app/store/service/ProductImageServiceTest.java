package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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

import app.store.dto.request.ProductImageRequest;
import app.store.dto.request.ProductImageUpdateRequest;
import app.store.dto.response.ProductImageResponse;
import app.store.entity.Product;
import app.store.entity.ProductImage;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.ProductImageMapper;
import app.store.mapper.ProductMapper;
import app.store.repository.ProductImageRepository;
import app.store.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class ProductImageServiceTest {

    @Mock
    ProductImageRepository productImageRepository;
    @Mock
    ProductImageMapper productImageMapper;
    @Mock
    ProductRepository productRepository;
    @Mock
    CloudinaryService cloudinaryService;
    @Mock
    ProductMapper productMapper;
    @InjectMocks
    ProductImageService productImageService;

    private final MultipartFile file =
            new MockMultipartFile("file", "anh.png", "image/png", "fake-bytes".getBytes());

    private Product buildProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    private ProductImage buildImage(Long id, Product product) {
        ProductImage image = new ProductImage();
        image.setId(id);
        image.setProduct(product);
        image.setImageUrl("http://cloud/old.png");
        return image;
    }

    // ==================== create ====================

    @Test
    void createProductImage_shouldUploadToCloudinary_andLinkProduct() {
        ProductImageRequest request = ProductImageRequest.builder().productId(1L).build();
        ProductImage mapped = new ProductImage();
        Product product = buildProduct(1L);

        when(productImageMapper.toProductImage(request)).thenReturn(mapped);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cloudinaryService.uploadImage(file, "store/products")).thenReturn("http://cloud/new.png");
        when(productImageRepository.save(mapped)).thenReturn(mapped);
        when(productImageMapper.toProductImageResponse(mapped)).thenReturn(new ProductImageResponse());

        productImageService.createProductImage(file, request);

        assertThat(mapped.getProduct()).isSameAs(product);
        assertThat(mapped.getImageUrl()).isEqualTo("http://cloud/new.png");
    }

    @Test
    void createProductImage_shouldThrow_whenProductNotFound() {
        ProductImageRequest request = ProductImageRequest.builder().productId(99L).build();

        when(productImageMapper.toProductImage(request)).thenReturn(new ProductImage());
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.createProductImage(file, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_IMAGE_NOT_EXISTED);

        // Không được upload ảnh khi sản phẩm không tồn tại (tránh rác trên Cloudinary)
        verify(cloudinaryService, never()).uploadImage(any(), any());
    }

    // ==================== read ====================

    @Test
    void getProductImageById_shouldThrow_whenNotFound() {
        when(productImageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.getProductImageById(99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_IMAGE_NOT_EXISTED);
    }

    @Test
    void getProductImagesByProductId_shouldMapEveryImage() {
        ProductImage image = buildImage(1L, buildProduct(1L));

        when(productImageRepository.getProductImageByProductId(1L)).thenReturn(List.of(image));
        when(productImageMapper.toProductImageResponse(image)).thenReturn(new ProductImageResponse());

        assertThat(productImageService.getProductImagesByProductId(1L)).hasSize(1);
    }

    // ==================== update / delete ====================

    @Test
    void updateProductImage_shouldApplyMapper_andSave() {
        ProductImage image = buildImage(1L, buildProduct(1L));
        ProductImageUpdateRequest request = ProductImageUpdateRequest.builder().altText("mới").build();

        when(productImageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(productImageRepository.save(image)).thenReturn(image);
        when(productImageMapper.toProductImageResponse(image)).thenReturn(new ProductImageResponse());

        productImageService.updateProductImage(1L, request);

        verify(productImageMapper).updateProductImage(image, request);
        verify(productImageRepository).save(image);
    }

    @Test
    void deleteProductImage_shouldRemoveFromCloudinaryThenDb() {
        ProductImage image = buildImage(1L, buildProduct(1L));
        when(productImageRepository.findById(1L)).thenReturn(Optional.of(image));

        productImageService.deleteProductImage(1L);

        var order = inOrder(cloudinaryService, productImageRepository);
        order.verify(cloudinaryService).deleteImage("http://cloud/old.png");
        order.verify(productImageRepository).delete(image);
    }

    // ==================== setPrimaryImage ====================

    @Test
    void setPrimaryImage_shouldResetOthersThenSetNew() {
        ProductImage image = buildImage(1L, buildProduct(10L));

        when(productRepository.existsById(10L)).thenReturn(true);
        when(productImageRepository.findById(1L)).thenReturn(Optional.of(image));

        productImageService.setPrimaryImage(1L, 10L);

        var order = inOrder(productImageRepository);
        order.verify(productImageRepository).resetAllPrimaryImagesForProduct(10L);
        order.verify(productImageRepository).setNewPrimaryImage(1L, 10L);
    }

    @Test
    void setPrimaryImage_shouldThrow_whenProductNotExisted() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productImageService.setPrimaryImage(1L, 99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_EXISTED);
    }

    @Test
    void setPrimaryImage_shouldThrow_whenImageBelongsToAnotherProduct() {
        ProductImage image = buildImage(1L, buildProduct(999L)); // ảnh của sản phẩm khác

        when(productRepository.existsById(10L)).thenReturn(true);
        when(productImageRepository.findById(1L)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> productImageService.setPrimaryImage(1L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong");

        verify(productImageRepository, never()).setNewPrimaryImage(any(), any());
    }
}
