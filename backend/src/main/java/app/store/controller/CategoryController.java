package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.request.CategoryBrandsRequest;
import app.store.dto.request.CategoryRequest;
import app.store.dto.response.BrandResponse;
import app.store.dto.response.CategoryResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Category Management", description = "APIs for managing product categories including CRUD operations")
public class CategoryController {
    CategoryService CategoryService;

    @PostMapping
    @Operation(
        summary = "Create a new category",
        description = "Creates a new product category. Only accessible by admin users."
    )
    ApiResponse<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {

        return ApiResponse.<CategoryResponse>builder()
                .result(CategoryService.createCategory(request))
                .message(ResponseMessage.CREATE_CATEGORY_SUCCESS)
                .build();
    }

    @GetMapping
    @Operation(
        summary = "Get all categories",
        description = "Retrieves all product categories with pagination and sorting. Accessible by authenticated users."
    )
    ApiResponse<List<CategoryResponse>> getAllCategories() {

        return ApiResponse.<List<CategoryResponse>>builder()
                .result(CategoryService.getAllCategories())
                .message(ResponseMessage.GET_ALL_CATEGORIES_SUCCESS)
                .build();
    }

    @GetMapping("/id/{categoryId}")
    @Operation(
        summary = "Get category by ID",
        description = "Retrieves detailed information of a category by ID. Accessible by authenticated users."
    )
    ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long categoryId) {
       return ApiResponse.<CategoryResponse>builder()
                .result(CategoryService.getCategoryById(categoryId))
               .message(ResponseMessage.GET_CATEGORY_SUCCESS)
                .build();
    }

    @GetMapping("/slug/{slug}")
    @Operation(
            summary = "Get category by slug",
            description = "Retrieves detailed information of a category by slug. Accessible by authenticated users."
    )
    ApiResponse<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        return ApiResponse.<CategoryResponse>builder()
                .result(CategoryService.getCategoryBySlug(slug))
                .message(ResponseMessage.GET_CATEGORY_SUCCESS)
                .build();
    }
    @PutMapping("/{categoryId}")
    @Operation(
        summary = "Update category",
        description = "Updates an existing product category by ID. Only accessible by admin users."
    )
    ApiResponse<CategoryResponse> updateCategory(@PathVariable Long categoryId, @RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .result(CategoryService.updateCategory(categoryId, request))
                .message(ResponseMessage.UPDATE_CATEGORY_SUCCESS)
                .build();
    }

    @GetMapping("/{categoryId}/brands")
    @Operation(
        summary = "Get brands by category",
        description = "Retrieves all brands belonging to a specific category. Accessible by authenticated users."
    )
    ApiResponse<List<BrandResponse>> getBrandsByCategory(@PathVariable Long categoryId) {
        return ApiResponse.<List<BrandResponse>>builder()
                .result(CategoryService.getBrandsByCategory(categoryId))
                .message(ResponseMessage.GET_CATEGORY_BRANDS_SUCCESS)
                .build();
    }

    @PutMapping("/{categoryId}/brands")
    @Operation(
        summary = "Update brands of category",
        description = "Updates brand assignments for a category. Only accessible by admin users."
    )
    ApiResponse<List<BrandResponse>> updateCategoryBrands(
            @PathVariable Long categoryId,
            @RequestBody CategoryBrandsRequest request
    ) {
        return ApiResponse.<List<BrandResponse>>builder()
                .result(CategoryService.updateCategoryBrands(categoryId, request.getBrandIds()))
                .message(ResponseMessage.UPDATE_CATEGORY_BRANDS_SUCCESS)
                .build();
    }

    @DeleteMapping("/{categoryId}")
    @Operation(
        summary = "Delete category",
        description = "Permanently deletes a product category by ID. Only accessible by admin users. Cannot delete categories with products."
    )
    ApiResponse<Void> deleteCategory(@PathVariable Long categoryId) {
        CategoryService.deleteCategory(categoryId);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.DELETE_CATEGORY_SUCCESS)
                .build();
    }

}
