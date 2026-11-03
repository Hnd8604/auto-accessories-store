package app.store.service;

import app.store.dto.request.RoleRequest;
import app.store.dto.response.RoleResponse;
import app.store.entity.Permission;
import app.store.entity.Role;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.RoleMapper;
import app.store.repository.PermissionRepository;
import app.store.repository.RolePermissionRepository;
import app.store.repository.RoleRepository;
import app.store.utils.TransactionUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;
    RolePermissionRepository rolePermissionRepository;
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public RoleResponse createRole(RoleRequest request) {
        var role = roleMapper.toRole(request); // we have to use var keyword because we had ignored permission when mapping

        var permissions = permissionRepository.findByNameIn(request.permissions());
        role.setPermissions(new HashSet<>(permissions));

        role = roleRepository.save(role);
        // Đồng bộ sang Redis sau khi commit, để Redis không giữ quyền của một thay đổi đã rollback
        syncRedisAfterCommit(request.name());
        return roleMapper.toRoleResponse(role);
    }


    @Transactional
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public RoleResponse updateRole(String roleId, RoleRequest request) {
        var role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        roleMapper.updateRole(role, request);
        var permissions = permissionRepository.findByNameIn(request.permissions());
        role.setPermissions(new HashSet<>(permissions));
        RoleResponse response = roleMapper.toRoleResponse(roleRepository.save(role));
        syncRedisAfterCommit(roleId);
        return response;
    }
    @PreAuthorize("hasAuthority('ROLE_GET_BY_ID')")
    public RoleResponse getRoleById(String roleId) {
        var role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));
        return roleMapper.toRoleResponse(role);
    }
    @PreAuthorize("hasAuthority('ROLE_GET_ALL')")
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toRoleResponse)
                .toList();
    }
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADD_PERMISSIONS')")
    public RoleResponse addPermissionsToRole(String roleId, Set<String> permNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        Set<Permission> perms = new HashSet<>(permissionRepository.findByNameIn(permNames));
        role.getPermissions().addAll(perms);

        RoleResponse response = roleMapper.toRoleResponse(roleRepository.save(role));
        syncRedisAfterCommit(roleId);
        return response;
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_REMOVE_PERMISSIONS')")
    public RoleResponse removePermissionsFromRole(String roleId, Set<String> permNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        role.getPermissions().removeIf(p -> permNames.contains(p.getName()));

        RoleResponse response = roleMapper.toRoleResponse(roleRepository.save(role));
        syncRedisAfterCommit(roleId);
        return response;
    }
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public void deleteRole(String roleId) {
        roleRepository.deleteById(roleId);
        // Xóa role permissions khỏi Redis sau khi commit
        TransactionUtils.runAfterCommit(() -> rolePermissionRepository.deleteRoleFromRedis(roleId));
    }

    // Redis đọc lại permissions từ DB nên phải chờ commit, nếu không có thể đọc dữ liệu chưa ghi
    // hoặc giữ thay đổi đã bị rollback
    private void syncRedisAfterCommit(String roleName) {
        TransactionUtils.runAfterCommit(() -> rolePermissionRepository.syncRolePermissionsFromDb(roleName));
    }
}
