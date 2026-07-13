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
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;
    RolePermissionRepository rolePermissionRepository;
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public RoleResponse createRole(RoleRequest request) {
        var role = roleMapper.toRole(request); // we have to use var keyword because we had ignored permission when mapping

        var permissions = permissionRepository.findByNameIn(request.getPermissions());
        role.setPermissions(new HashSet<>(permissions));

        role = roleRepository.save(role);
        // Đồng bộ sang Redis
        rolePermissionRepository.syncRolePermissionsFromDb(request.getName());
        return roleMapper.toRoleResponse(role);
    }


    @Transactional
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public RoleResponse updateRole(String roleId, RoleRequest request) {
        var role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        roleMapper.updateRole(role, request);
        var permissions = permissionRepository.findByNameIn(request.getPermissions());
        role.setPermissions(new HashSet<>(permissions));
        // Đồng bộ sang Redis
        rolePermissionRepository.syncRolePermissionsFromDb(roleId);
        return roleMapper.toRoleResponse(roleRepository.save(role));
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


        // Đồng bộ sang Redis
        rolePermissionRepository.syncRolePermissionsFromDb(roleId);
        return roleMapper.toRoleResponse(roleRepository.save(role));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_REMOVE_PERMISSIONS')")
    public RoleResponse removePermissionsFromRole(String roleId, Set<String> permNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        role.getPermissions().removeIf(p -> permNames.contains(p.getName()));

        // Đồng bộ sang Redis
        rolePermissionRepository.syncRolePermissionsFromDb(roleId);
        return roleMapper.toRoleResponse(roleRepository.save(role));
    }
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public void deleteRole(String roleId) {
        roleRepository.deleteById(roleId);
        // Xóa role permissions khỏi Redis
        rolePermissionRepository.deleteRoleFromRedis(roleId);
    }
}
