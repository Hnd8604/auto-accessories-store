package app.store.service;

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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductImageService {
    ProductImageRepository productImageRepository;
    ProductImageMapper productImageMapper;
    ProductRepository productRepository;
    CloudinaryService CloudinaryService;
    @PreAuthorize("hasAuthority('PRODUCT_IMAGE_CREATE')")
    public ProductImageResponse createProductImage(MultipartFile file, ProductImageRequest request) {

        // Tạo ProductImage entity
        ProductImage productImage = productImageMapper.toProductImage(request);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_EXISTED));
        // Upload ảnh lên Cloudinary
        String imageUrl = CloudinaryService.uploadImage(file, "store/products");

        productImage.setProduct(product);
        productImage.setImageUrl(imageUrl);

        return productImageMapper.toProductImageResponse(
                productImageRepository.save(productImage));
    }

    private final ProductMapper productMapper;
    public Page<ProductImageResponse> getAllProductImages(Pageable pageable) {
        return productImageRepository.findAll(pageable)
                .map(productImageMapper::toProductImageResponse);
    }
    public ProductImageResponse getProductImageById(Long imageId) {
        ProductImage productImage = productImageRepository.findById(imageId).
                orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_EXISTED));  ;
        return productImageMapper.toProductImageResponse(productImage);
    }
    public List<ProductImageResponse> getProductImagesByProductId(Long productId) {
        return productImageRepository.getProductImageByProductId(productId).stream()
                .map(productImageMapper::toProductImageResponse).toList();
    }
    @PreAuthorize("hasAuthority('PRODUCT_IMAGE_UPDATE')")
    public ProductImageResponse updateProductImage(Long imageId, ProductImageUpdateRequest request) {
        // Tìm ProductImage cũ
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_EXISTED));

//        // Tìm Product mới
//        Product product = productRepository.findById(request.getProductId())
//                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));
//
      productImageMapper.updateProductImage(productImage, request);
//      productImage.setProduct(product);
        return productImageMapper.toProductImageResponse(
                productImageRepository.save(productImage));
    }
    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_IMAGE_DELETE')")
    public void deleteProductImage(Long imageId) {
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_EXISTED));

        // Xóa ảnh trên Cloudinary
        CloudinaryService.deleteImage(productImage.getImageUrl());

        // Xóa record trong database
        productImageRepository.delete(productImage);
    }
    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_IMAGE_SET_PRIMARY')")
    public void setPrimaryImage(Long imageId, Long productId) {
        // Bước 1: Kiểm tra sự tồn tại của Product
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_EXISTED);
        }

        // Bước 2: Kiểm tra sự tồn tại của ProductImage
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_EXISTED));

        // Bước 3: Kiểm tra xem image có thuộc về product không
        if (!productImage.getProduct().getId().equals(productId)) {
            throw new RuntimeException("Image with id " + imageId + " does not belong to product with id " + productId);
        }
            productImageRepository.resetAllPrimaryImagesForProduct(productId);
            // Bước 2: "Thiết lập" ảnh mới là true
            productImageRepository.setNewPrimaryImage(imageId, productId);
    }
}
