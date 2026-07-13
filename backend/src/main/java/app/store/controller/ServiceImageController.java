package app.store.controller;

import app.store.dto.request.ServiceImageRequest;
import app.store.dto.request.ServiceImageUpdateRequest;
import app.store.dto.response.ServiceImageResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.service.ServiceImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/service-images")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Service Image Management", description = "APIs for managing service images")
public class ServiceImageController {

    ServiceImageService serviceImageService;

    @GetMapping("/services/{serviceId}")
    @Operation(summary = "Get images by service ID")
    public ApiResponse<List<ServiceImageResponse>> getImagesByServiceId(@PathVariable Long serviceId) {
        return ApiResponse.<List<ServiceImageResponse>>builder()
                .result(serviceImageService.getImagesByServiceId(serviceId))
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a service image")
    public ApiResponse<ServiceImageResponse> createServiceImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("serviceId") Long serviceId,
            @RequestParam(value = "isPrimary", defaultValue = "false") Boolean isPrimary) {
        ServiceImageRequest request = ServiceImageRequest.builder()
                .serviceId(serviceId)
                .isPrimary(isPrimary)
                .build();
        return ApiResponse.<ServiceImageResponse>builder()
                .result(serviceImageService.createServiceImage(file, request))
                .build();
    }

    @PutMapping("/{imageId}")
    @Operation(summary = "Update service image metadata")
    public ApiResponse<ServiceImageResponse> updateServiceImage(
            @PathVariable Long imageId,
            @RequestBody ServiceImageUpdateRequest request) {
        return ApiResponse.<ServiceImageResponse>builder()
                .result(serviceImageService.updateServiceImage(imageId, request))
                .build();
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Delete a service image")
    public ApiResponse<Void> deleteServiceImage(@PathVariable Long imageId) {
        serviceImageService.deleteServiceImage(imageId);
        return ApiResponse.<Void>builder()
                .message("Service image deleted successfully")
                .build();
    }

    @PostMapping("/services/{serviceId}/images/{imageId}/set-primary")
    @Operation(summary = "Set primary image for a service")
    public ApiResponse<Void> setPrimaryImage(
            @PathVariable Long serviceId,
            @PathVariable Long imageId) {
        serviceImageService.setPrimaryImage(serviceId, imageId);
        return ApiResponse.<Void>builder()
                .build();
    }
}
