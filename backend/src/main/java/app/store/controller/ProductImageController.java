package app.store.controller;

import app.store.dto.request.ProductImageRequest;
import app.store.dto.request.ProductImageUpdateRequest;
import app.store.dto.response.ProductImageResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.service.ProductImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static app.store.utils.SortUtils.buildSort;

@RestController
@RequestMapping("/product-images")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Product Image Management", description = "APIs for managing product images including upload, update, and setting primary image")
public class ProductImageController {
    ProductImageService ProductImageService;

    @GetMapping
    @Operation(
        summary = "Get all product images",
        description = "Retrieves all product images with pagination and sorting. Accessible by authenticated users."
    )
    public ApiResponse<Page<ProductImageResponse>> getAllProductImages(
                                                                        @RequestParam(value = "page", defaultValue = "0") int page,
                                                                       @RequestParam(value = "size", defaultValue = "2") int size,
                                                                        @RequestParam(value = "sort", defaultValue = "product.id,DESC") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
                return ApiResponse.<Page<ProductImageResponse>>builder()
                .result(ProductImageService.getAllProductImages(pageable))
                .build();
    }
    @GetMapping("/{imageId}")
    @Operation(
        summary = "Get product image by ID",
        description = "Retrieves detailed information of a product image by ID. Accessible by authenticated users."
    )
    public ApiResponse<ProductImageResponse> getProductImageById(@PathVariable Long imageId) {
        return ApiResponse.<ProductImageResponse>builder()
                .result(ProductImageService.getProductImageById(imageId))
                .build();
    }
    @GetMapping("/products/{productId}")
    @Operation(
        summary = "Get product images by product ID",
        description = "Retrieves all images associated with a specific product. Accessible by authenticated users."
    )
    public ApiResponse<List<ProductImageResponse>> getProductImagesByProductId(@PathVariable Long productId) {
        return ApiResponse.<List<ProductImageResponse>>builder()
                .result(ProductImageService.getProductImagesByProductId(productId))
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload product image",
        description = "Uploads a new product image. Supports setting the image as primary. Only accessible by admin users."
    )
    public ApiResponse<ProductImageResponse> createProductImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productId") Long productId,
            @RequestParam(value = "isPrimary", defaultValue = "false") Boolean isPrimary) {
        ProductImageRequest request = ProductImageRequest.builder()
                .productId(productId)
                .isPrimary(isPrimary)
                .build();
        return ApiResponse.<ProductImageResponse>builder()
                .result(ProductImageService.createProductImage(file, request))
                .build();
    }

    @PutMapping("/{imageId}")
    @Operation(
        summary = "Update product image",
        description = "Updates product image information. Only accessible by admin users."
    )
    public ApiResponse<ProductImageResponse> updateProductImage(@PathVariable Long imageId, @RequestBody ProductImageUpdateRequest request) {
        return ApiResponse.<ProductImageResponse>builder()
                .result(ProductImageService.updateProductImage(imageId, request))
                .build();
    }

    @DeleteMapping("/{imageId}")
    @Operation(
        summary = "Delete product image",
        description = "Permanently deletes a product image by ID. Only accessible by admin users."
    )
    public ApiResponse<Void> deleteProductImage(@PathVariable Long imageId) {
        ProductImageService.deleteProductImage(imageId);
        return ApiResponse.<Void>builder()
                .message("Product image deleted successfully")
                .build();
    }
    @PostMapping("/products/{productId}/images/{imageId}/set-primary")
    @Operation(
        summary = "Set primary image",
        description = "Sets a product image as the primary/main image for display. Only accessible by admin users."
    )
    public ApiResponse<Void> setPrimaryImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) { // Đổi tên biến cho rõ
        ProductImageService.setPrimaryImage(imageId , productId);
        return ApiResponse.<Void>builder()
                .build();
    }
}
