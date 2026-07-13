package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.request.ProductRequest;
import app.store.dto.request.ProductSearchRequest;
import app.store.dto.response.auth.ApiResponse;
import app.store.dto.response.ProductResponse;
import app.store.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import static app.store.utils.SortUtils.buildSort;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Product Management", description = "APIs for managing products including CRUD operations, search, and filtering")
public class ProductController {
    ProductService ProductService;
    
    @PostMapping("/search")
    @Operation(
        summary = "Search products",
        description = "Searches products with advanced filters including name, category, brand, price range, and stock status. Supports pagination and sorting."
    )
    public ApiResponse<Page<ProductResponse>> searchProducts(
            @RequestBody ProductSearchRequest req,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name,ASC") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));

        return ApiResponse.<Page<ProductResponse>>builder()
                .result(ProductService.search(req, pageable))
                .build();
    }
    
    @PostMapping
    @Operation(
        summary = "Create a new product",
        description = "Creates a new product with details including name, description, price, stock, category, and brand. Only accessible by admin users."
    )
    ApiResponse<ProductResponse> createProduct(@RequestBody ProductRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(ProductService.createProduct(request))
                .message(ResponseMessage.CREATE_PRODUCT_SUCCESS)
                .build();
    }

    @PutMapping("/{productId}")
    @Operation(
        summary = "Update product",
        description = "Updates an existing product by ID. Only accessible by admin users."
    )
    ApiResponse<ProductResponse> updateProduct(@PathVariable Long productId, @RequestBody ProductRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(ProductService.updateProduct(productId, request))
                .message(ResponseMessage.UPDATE_PRODUCT_SUCCESS)
                .build();
    }

    @DeleteMapping("/{productId}")
    @Operation(
        summary = "Delete product",
        description = "Permanently deletes a product by ID. Only accessible by admin users."
    )
    ApiResponse<Void> deleteProduct(@PathVariable Long productId) {
        ProductService.deleteProduct(productId);
        return ApiResponse.<Void>builder().message(ResponseMessage.DELETE_PRODUCT_SUCCESS).build();
    }

    @GetMapping
    @Operation(
        summary = "Get all products",
        description = "Retrieves all products with pagination and sorting. Accessible by authenticated users."
    )
    ApiResponse<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        return ApiResponse.<Page<ProductResponse>>builder()
                .result(ProductService.getAllProducts(pageable))
                .message(ResponseMessage.GET_ALL_PRODUCTS_SUCCESS)
                .build();
    }

    @GetMapping("/categories/{categoryId}")
    @Operation(
        summary = "Get products by category",
        description = "Retrieves all products filtered by category ID with pagination and sorting. Accessible by authenticated users."
    )
    ApiResponse<Page<ProductResponse>> getAllProductsByCategoryId(@PathVariable Long categoryId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "2") int size,
                                                                  @RequestParam(defaultValue = "name,asc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        return ApiResponse.<Page<ProductResponse>>builder()
                .result(ProductService.getProductsByCategoryId(categoryId, pageable))
                .message(ResponseMessage.GET_ALL_PRODUCTS_BY_CATEGORY_SUCCESS)
                .build();
    }

    @GetMapping("/brands/{brandId}")
    @Operation(
        summary = "Get products by brand",
        description = "Retrieves all products filtered by brand ID with pagination and sorting. Accessible by authenticated users."
    )
    ApiResponse<Page<ProductResponse>> getAllProductsByBrandId(@PathVariable Long brandId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "2") int size,
                                                               @RequestParam(defaultValue = "name,asc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));

        return ApiResponse.<Page<ProductResponse>>builder()
                .result(ProductService.getProductsByBrandId(brandId, pageable))
                .message(ResponseMessage.GET_ALL_PRODUCTS_BY_BRAND_SUCCESS)
                .build();
    }

    @GetMapping("/id/{productId}")
    @Operation(
        summary = "Get product by ID",
        description = "Retrieves detailed information of a product by ID. Accessible by authenticated users."
    )
    ApiResponse<ProductResponse> getProductById(@PathVariable Long productId) {
        return ApiResponse.<ProductResponse>builder()
                .result(ProductService.getProductById(productId))
                .message(ResponseMessage.GET_PRODUCT_SUCCESS)
                .build();
    }

    @GetMapping("/slug/{slug}")
    @Operation(
            summary = "Get product by Slug",
            description = "Retrieves detailed information of a product by Slug. Accessible by authenticated users."
    )
    ApiResponse<ProductResponse> getProductBySlug(@PathVariable String slug) {
        return ApiResponse.<ProductResponse>builder()
                .result(ProductService.getProductBySlug(slug))
                .message(ResponseMessage.GET_PRODUCT_SUCCESS)
                .build();
    }
}

