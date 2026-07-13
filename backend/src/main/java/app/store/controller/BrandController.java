package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.request.BrandRequest;
import app.store.dto.response.BrandResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static app.store.utils.SortUtils.buildSort;
@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Brand Management", description = "APIs for managing product brands including CRUD operations")
public class BrandController {
    BrandService BrandService;

    @GetMapping
    @Operation(
        summary = "Get all brands",
        description = "Retrieves all brands with pagination and sorting. Accessible by authenticated users."
    )
    ApiResponse<List<BrandResponse>> getAllBrands() {
        return ApiResponse.<List<BrandResponse>>builder()
                .result(BrandService.getAllBrands())
                .message(ResponseMessage.GET_ALL_BRANDS_SUCCESS)
                .build();
    }
    @GetMapping("/id/{brandId}")
    @Operation(
        summary = "Get brand by ID",
        description = "Retrieves detailed information of a brand by ID. Accessible by authenticated users."
    )
    ApiResponse<BrandResponse> getBrandById(@PathVariable Long brandId) {
        return ApiResponse.<BrandResponse>builder()
                .result(BrandService.getBrandById(brandId))
                .message(ResponseMessage.GET_BRAND_SUCCESS)
                .build();
    }

    @GetMapping("/slug/{slug}")
    @Operation(
            summary = "Get brand by slug",
            description = "Retrieves detailed information of a brand by slug. Accessible by authenticated users."
    )
    ApiResponse<BrandResponse> getBrandBySlug(@PathVariable String slug) {
        return ApiResponse.<BrandResponse>builder()
                .result(BrandService.getBrandBySlug(slug))
                .message(ResponseMessage.GET_BRAND_SUCCESS)
                .build();
    }
    @PostMapping
    @Operation(
        summary = "Create a new brand",
        description = "Creates a new product brand. Only accessible by admin users."
    )
    ApiResponse<BrandResponse> createBrand(@RequestBody BrandRequest BrandRequest) {
        return ApiResponse.<BrandResponse>builder()
                .result(BrandService.createBrand(BrandRequest))
                .message(ResponseMessage.CREATE_BRAND_SUCCESS)
                .build();
    }

    @PutMapping("/{brandId}")
    @Operation(
        summary = "Update brand",
        description = "Updates an existing brand by ID. Only accessible by admin users."
    )
    ApiResponse<BrandResponse> updateBrand(@PathVariable Long brandId,@RequestBody BrandRequest BrandRequest) {
        return ApiResponse.<BrandResponse>builder()
                .result(BrandService.updateBrand(brandId, BrandRequest))
                .message(ResponseMessage.UPDATE_BRAND_SUCCESS)
                .build();
    }
    @DeleteMapping("/{brandId}")
    @Operation(
        summary = "Delete brand",
        description = "Permanently deletes a brand by ID. Only accessible by admin users. Cannot delete brands with products."
    )
    ApiResponse<Void> deleteBrand(@PathVariable Long brandId) {
        BrandService.deleteBrand(brandId);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.DELETE_BRAND_SUCCESS)
                .build();
        }

}
