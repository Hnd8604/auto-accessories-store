package app.store.service;

import app.store.dto.request.PermissionRequest;
import app.store.dto.response.PermissionResponse;
import app.store.entity.Permission;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.PermissionMapper;
import app.store.repository.PermissionRepository;
import app.store.repository.RolePermissionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PermissionService {
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;
    RolePermissionRepository rolePermissionRepository;
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    public PermissionResponse createPermission(PermissionRequest request) {
        Permission permission = permissionMapper.toPermission(request);
        return permissionMapper.toPermissionResponse(permissionRepository.save(permission));
    }
    @PreAuthorize("hasAuthority('PERMISSION_GET_ALL')")
    public List<PermissionResponse> getAllPermissions() {
        var permissions = permissionRepository.findAll();
        return permissions.stream().map(permissionMapper::toPermissionResponse).toList();
    }
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    public PermissionResponse updatePermission(String permissionId, PermissionRequest request) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_EXISTED));

        permissionMapper.updatePermission(permission, request);
        return permissionMapper.toPermissionResponse(permissionRepository.save(permission));
    }
    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    public void deletePermission(String permissionId) {
        permissionRepository.deleteById(permissionId);
//        rolePermissionRepository.syncAllRolesFromDb(); không cần thiết vì chỉ có thể xóa khi permission không thuộc role nào;
    }

}
