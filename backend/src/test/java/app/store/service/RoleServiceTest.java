package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

    @Mock
    RoleRepository roleRepository;
    @Mock
    PermissionRepository permissionRepository;
    @Mock
    RoleMapper roleMapper;
    @Mock
    RolePermissionRepository rolePermissionRepository;
    @InjectMocks
    RoleService roleService;

    private Permission perm(String name) {
        return Permission.builder().name(name).build();
    }

    @Test
    void createRole_shouldAttachPermissions_andSyncRedis() {
        Set<String> permNames = Set.of("USER_CREATE", "USER_DELETE");
        RoleRequest request = RoleRequest.builder().name("ADMIN").permissions(permNames).build();
        Role mapped = Role.builder().name("ADMIN").build();
        Permission p1 = perm("USER_CREATE");
        Permission p2 = perm("USER_DELETE");

        when(roleMapper.toRole(request)).thenReturn(mapped);
        when(permissionRepository.findByNameIn(permNames)).thenReturn(List.of(p1, p2));
        when(roleRepository.save(mapped)).thenReturn(mapped);
        when(roleMapper.toRoleResponse(mapped)).thenReturn(new RoleResponse());

        roleService.createRole(request);

        assertThat(mapped.getPermissions()).containsExactlyInAnyOrder(p1, p2);
        // Phải lưu DB TRƯỚC rồi mới đồng bộ Redis, nếu ngược lại Redis sẽ lệch dữ liệu
        var order = inOrder(roleRepository, rolePermissionRepository);
        order.verify(roleRepository).save(mapped);
        order.verify(rolePermissionRepository).syncRolePermissionsFromDb("ADMIN");
    }

    @Test
    void updateRole_shouldReplacePermissions_andSyncRedis() {
        Role role = Role.builder().name("ADMIN")
                .permissions(new HashSet<>(Set.of(perm("OLD")))).build();
        Set<String> permNames = Set.of("NEW");
        RoleRequest request = RoleRequest.builder().name("ADMIN").permissions(permNames).build();
        Permission newPerm = perm("NEW");

        when(roleRepository.findById("ADMIN")).thenReturn(Optional.of(role));
        when(permissionRepository.findByNameIn(permNames)).thenReturn(List.of(newPerm));
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toRoleResponse(role)).thenReturn(new RoleResponse());

        roleService.updateRole("ADMIN", request);

        assertThat(role.getPermissions()).containsExactly(newPerm);
        verify(roleMapper).updateRole(role, request);
        verify(rolePermissionRepository).syncRolePermissionsFromDb("ADMIN");
    }

    @Test
    void updateRole_shouldThrow_whenRoleNotFound() {
        when(roleRepository.findById("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.updateRole("GHOST", RoleRequest.builder().build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_NOT_EXISTED);

        verify(rolePermissionRepository, never()).syncRolePermissionsFromDb(any());
    }

    @Test
    void getRoleById_shouldThrow_whenNotFound() {
        when(roleRepository.findById("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleById("GHOST"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_NOT_EXISTED);
    }

    @Test
    void getAllRoles_shouldMapEveryRole() {
        Role role = Role.builder().name("USER").build();
        when(roleRepository.findAll()).thenReturn(List.of(role));
        when(roleMapper.toRoleResponse(role)).thenReturn(new RoleResponse());

        assertThat(roleService.getAllRoles()).hasSize(1);
    }

    @Test
    void addPermissionsToRole_shouldKeepOldPermissions() {
        Permission old = perm("OLD");
        Permission added = perm("NEW");
        Role role = Role.builder().name("ADMIN")
                .permissions(new HashSet<>(Set.of(old))).build();

        when(roleRepository.findById("ADMIN")).thenReturn(Optional.of(role));
        when(permissionRepository.findByNameIn(Set.of("NEW"))).thenReturn(List.of(added));
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toRoleResponse(role)).thenReturn(new RoleResponse());

        roleService.addPermissionsToRole("ADMIN", Set.of("NEW"));

        assertThat(role.getPermissions()).containsExactlyInAnyOrder(old, added);
        verify(rolePermissionRepository).syncRolePermissionsFromDb("ADMIN");
    }

    @Test
    void removePermissionsFromRole_shouldRemoveOnlyMatchingNames() {
        Permission keep = perm("KEEP");
        Permission remove = perm("REMOVE");
        Role role = Role.builder().name("ADMIN")
                .permissions(new HashSet<>(Set.of(keep, remove))).build();

        when(roleRepository.findById("ADMIN")).thenReturn(Optional.of(role));
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toRoleResponse(role)).thenReturn(new RoleResponse());

        roleService.removePermissionsFromRole("ADMIN", Set.of("REMOVE"));

        assertThat(role.getPermissions()).containsExactly(keep);
        verify(rolePermissionRepository).syncRolePermissionsFromDb("ADMIN");
    }

    @Test
    void deleteRole_shouldRemoveFromDbAndRedis() {
        roleService.deleteRole("ADMIN");

        verify(roleRepository).deleteById("ADMIN");
        verify(rolePermissionRepository).deleteRoleFromRedis("ADMIN");
    }
}
