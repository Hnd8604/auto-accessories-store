package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.request.BannerRequest;
import app.store.dto.response.BannerResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Banner Management", description = "APIs for managing banners including CRUD operations")
public class BannerController {
    BannerService bannerService;

    @GetMapping
    @Operation(
            summary = "Get all banners",
            description = "Retrieves all banners sorted by display order. Accessible by all users."
    )
    ApiResponse<List<BannerResponse>> getAllBanners() {
        return ApiResponse.<List<BannerResponse>>builder()
                .result(bannerService.getAllBanners())
                .message(ResponseMessage.GET_ALL_BANNERS_SUCCESS)
                .build();
    }

    @GetMapping("/active")
    @Operation(
            summary = "Get active banners",
            description = "Retrieves only active banners sorted by display order. Used for public display."
    )
    ApiResponse<List<BannerResponse>> getActiveBanners() {
        return ApiResponse.<List<BannerResponse>>builder()
                .result(bannerService.getActiveBanners())
                .message(ResponseMessage.GET_ALL_BANNERS_SUCCESS)
                .build();
    }

    @GetMapping("/{bannerId}")
    @Operation(
            summary = "Get banner by ID",
            description = "Retrieves detailed information of a banner by ID. Accessible by all users."
    )
    ApiResponse<BannerResponse> getBannerById(@PathVariable Long bannerId) {
        return ApiResponse.<BannerResponse>builder()
                .result(bannerService.getBannerById(bannerId))
                .message(ResponseMessage.GET_BANNER_SUCCESS)
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a new banner",
            description = "Creates a new banner with image. Only accessible by admin users."
    )
    ApiResponse<BannerResponse> createBanner(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("banner") BannerRequest bannerRequest) {
        return ApiResponse.<BannerResponse>builder()
                .result(bannerService.createBanner(file, bannerRequest))
                .message(ResponseMessage.CREATE_BANNER_SUCCESS)
                .build();
    }

    @PutMapping(value = "/{bannerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update banner",
            description = "Updates an existing banner by ID. Optionally upload a new image. Only accessible by admin users."
    )
    ApiResponse<BannerResponse> updateBanner(
            @PathVariable Long bannerId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Valid @RequestPart("banner") BannerRequest bannerRequest) {
        return ApiResponse.<BannerResponse>builder()
                .result(bannerService.updateBanner(bannerId, file, bannerRequest))
                .message(ResponseMessage.UPDATE_BANNER_SUCCESS)
                .build();
    }

    @DeleteMapping("/{bannerId}")
    @Operation(
            summary = "Delete banner",
            description = "Permanently deletes a banner by ID. Only accessible by admin users."
    )
    ApiResponse<Void> deleteBanner(@PathVariable Long bannerId) {
        bannerService.deleteBanner(bannerId);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.DELETE_BANNER_SUCCESS)
                .build();
    }
}
