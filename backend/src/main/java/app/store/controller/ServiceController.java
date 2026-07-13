package app.store.controller;

import app.store.dto.request.ServiceRequest;
import app.store.dto.response.ServiceResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.service.ProfessionalServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Professional Service Management", description = "APIs for managing professional services")
public class ServiceController {

    ProfessionalServiceService professionalServiceService;

    @GetMapping
    @Operation(summary = "Get all services")
    public ApiResponse<List<ServiceResponse>> getAllServices() {
        return ApiResponse.<List<ServiceResponse>>builder()
                .result(professionalServiceService.getAllServices())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service by ID")
    public ApiResponse<ServiceResponse> getServiceById(@PathVariable Long id) {
        return ApiResponse.<ServiceResponse>builder()
                .result(professionalServiceService.getServiceById(id))
                .build();
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get service by slug")
    public ApiResponse<ServiceResponse> getServiceBySlug(@PathVariable String slug) {
        return ApiResponse.<ServiceResponse>builder()
                .result(professionalServiceService.getServiceBySlug(slug))
                .build();
    }

    @PostMapping
    @Operation(summary = "Create a new service")
    public ApiResponse<ServiceResponse> createService(@RequestBody ServiceRequest request) {
        return ApiResponse.<ServiceResponse>builder()
                .result(professionalServiceService.createService(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a service")
    public ApiResponse<ServiceResponse> updateService(@PathVariable Long id, @RequestBody ServiceRequest request) {
        return ApiResponse.<ServiceResponse>builder()
                .result(professionalServiceService.updateService(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a service")
    public ApiResponse<Void> deleteService(@PathVariable Long id) {
        professionalServiceService.deleteService(id);
        return ApiResponse.<Void>builder()
                .message("Service deleted successfully")
                .build();
    }
}
