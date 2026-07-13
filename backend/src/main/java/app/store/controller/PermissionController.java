package app.store.controller;


import app.store.constant.ResponseMessage;
import app.store.dto.request.PermissionRequest;
import app.store.dto.response.auth.ApiResponse;
import app.store.dto.response.PermissionResponse;
import app.store.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Permission Management", description = "APIs for managing permissions (access control definitions)")
public class PermissionController {
    PermissionService PermissionService;
    
    @PostMapping
    @Operation(
        summary = "Create a new permission",
        description = "Creates a new permission. Only accessible by admin users. Permissions define what actions users can perform."
    )
    ApiResponse<PermissionResponse> createPermission(@RequestBody PermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .message(ResponseMessage.CREATE_PERMISSION_SUCCESS)
                .result(PermissionService.createPermission(request))
                .build();
    }


    @PutMapping("/{permissionId}")
    @Operation(
        summary = "Update permission",
        description = "Updates an existing permission by ID. Only accessible by admin users."
    )
    ApiResponse<PermissionResponse> updatePermission(@PathVariable String permissionId,
                                                      @RequestBody PermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .message(ResponseMessage.UPDATE_PERMISSION_SUCCESS)
                .result(PermissionService.updatePermission(permissionId, request))
                .build();
    }
    @GetMapping
    @Operation(
        summary = "Get all permissions",
        description = "Retrieves all permissions in the system. Only accessible by admin users."
    )
    ApiResponse<List<PermissionResponse>> getPermissions() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .message(ResponseMessage.GET_ALL_PERMISSIONS_SUCCESS)
                .result(PermissionService.getAllPermissions())
                .build();
    }

    @DeleteMapping("/{permissionId}")
    @Operation(
        summary = "Delete permission",
        description = "Permanently deletes a permission by ID. Only accessible by admin users. Cannot delete permissions that are assigned to roles."
    )
    ApiResponse<Void> deletePermission(@PathVariable String permissionId) {
        PermissionService.deletePermission(permissionId);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.DELETE_PERMISSION_SUCCESS)
                .build();
    }

}
