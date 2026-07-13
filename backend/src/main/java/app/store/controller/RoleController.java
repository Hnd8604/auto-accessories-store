package app.store.controller;


import app.store.constant.ResponseMessage;
import app.store.dto.request.RoleRequest;
import app.store.dto.response.auth.ApiResponse;
import app.store.dto.response.RoleResponse;
import app.store.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Role Management", description = "APIs for managing roles and role-permission associations")
public class RoleController {
    RoleService RoleService;

    @PostMapping
    @Operation(
        summary = "Create a new role",
        description = "Creates a new role. Only accessible by admin users."
    )
    public ApiResponse<RoleResponse> createRole(@RequestBody RoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .message(ResponseMessage.CREATE_ROLE_SUCCESS)
                .result(RoleService.createRole(request))
                .build();
    }

    @GetMapping
    @Operation(
        summary = "Get all roles",
        description = "Retrieves all roles with their associated permissions. Only accessible by admin users."
    )
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        return ApiResponse.<List<RoleResponse>>builder()
                .message(ResponseMessage.GET_ALL_ROLES_SUCCESS)
                .result(RoleService.getAllRoles())
                .build();
    }
    @GetMapping("/{roleId}")
    @Operation(
        summary = "Get role by ID",
        description = "Retrieves detailed information of a role including its permissions. Only accessible by admin users."
    )
    public ApiResponse<RoleResponse> getRoleById(@PathVariable String roleId) {
        return ApiResponse.<RoleResponse>builder()
                .message(ResponseMessage.GET_ROLE_SUCCESS)
                .result(RoleService.getRoleById(roleId))
                .build();
    }

    @PutMapping("/{roleId}")
    @Operation(
        summary = "Update role",
        description = "Updates an existing role by ID. Only accessible by admin users."
    )
    public ApiResponse<RoleResponse> updateRole(@PathVariable String roleId, @RequestBody RoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .message(ResponseMessage.UPDATE_ROLE_SUCCESS)
                .result(RoleService.updateRole(roleId, request))
                .build();
    }

    @PostMapping("/{roleId}/permissions")
    @Operation(
        summary = "Add permissions to role",
        description = "Adds a set of permissions to a role. Only accessible by admin users. Automatically syncs with Redis cache."
    )
    public ApiResponse<RoleResponse> addPermissionsToRole(
            @PathVariable String roleId,
            @RequestBody Set<String> permissions) {

        RoleService.addPermissionsToRole(roleId, permissions);
        return ApiResponse.<RoleResponse>builder()
                .result(RoleService.addPermissionsToRole(roleId, permissions))
                .build();
    }

    @DeleteMapping("/{roleId}/permissions")
    @Operation(
        summary = "Remove permissions from role",
        description = "Removes a set of permissions from a role. Only accessible by admin users. Automatically syncs with Redis cache."
    )
    public ApiResponse<RoleResponse> removePermissionsFromRole(
            @PathVariable String roleId,
            @RequestBody Set<String> permissions) {

        RoleService.removePermissionsFromRole(roleId, permissions);
        return ApiResponse.<RoleResponse>builder()
                .result(RoleService.removePermissionsFromRole(roleId, permissions))
                .build();
    }
    @DeleteMapping("/{roleId}")
    @Operation(
        summary = "Delete role",
        description = "Permanently deletes a role by ID. Only accessible by admin users. Cannot delete roles that are assigned to users."
    )
    public ApiResponse<Void> deleteRole(@PathVariable String roleId) {
        RoleService.deleteRole(roleId);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.DELETE_ROLE_SUCCESS)
                .build();
    }
}
