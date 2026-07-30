package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.store.dto.request.PermissionRequest;
import app.store.dto.response.PermissionResponse;
import app.store.entity.Permission;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.PermissionMapper;
import app.store.repository.PermissionRepository;
import app.store.repository.RolePermissionRepository;

@ExtendWith(MockitoExtension.class)
public class PermissionServiceTest {

    @Mock
    PermissionRepository permissionRepository;
    @Mock
    PermissionMapper permissionMapper;
    @Mock
    RolePermissionRepository rolePermissionRepository;
    @InjectMocks
    PermissionService permissionService;

    @Test
    void createPermission_shouldSaveMappedEntity() {
        PermissionRequest request = PermissionRequest.builder()
                .name("USER_CREATE").description("Tạo user").build();
        Permission mapped = Permission.builder().name("USER_CREATE").build();
        PermissionResponse expected = new PermissionResponse();

        when(permissionMapper.toPermission(request)).thenReturn(mapped);
        when(permissionRepository.save(mapped)).thenReturn(mapped);
        when(permissionMapper.toPermissionResponse(mapped)).thenReturn(expected);

        assertThat(permissionService.createPermission(request)).isSameAs(expected);
    }

    @Test
    void getAllPermissions_shouldMapEveryPermission() {
        Permission p1 = Permission.builder().name("A").build();
        Permission p2 = Permission.builder().name("B").build();

        when(permissionRepository.findAll()).thenReturn(List.of(p1, p2));
        when(permissionMapper.toPermissionResponse(any())).thenReturn(new PermissionResponse());

        assertThat(permissionService.getAllPermissions()).hasSize(2);
    }

    @Test
    void updatePermission_shouldApplyChanges_whenFound() {
        Permission permission = Permission.builder().name("USER_CREATE").build();
        PermissionRequest request = PermissionRequest.builder().description("Mô tả mới").build();

        when(permissionRepository.findById("USER_CREATE")).thenReturn(Optional.of(permission));
        when(permissionRepository.save(permission)).thenReturn(permission);
        when(permissionMapper.toPermissionResponse(permission)).thenReturn(new PermissionResponse());

        permissionService.updatePermission("USER_CREATE", request);

        verify(permissionMapper).updatePermission(permission, request);
        verify(permissionRepository).save(permission);
    }

    @Test
    void updatePermission_shouldThrow_whenNotFound() {
        when(permissionRepository.findById("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.updatePermission("GHOST",
                PermissionRequest.builder().build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PERMISSION_NOT_EXISTED);

        verify(permissionRepository, never()).save(any());
    }

    @Test
    void deletePermission_shouldDeleteById() {
        permissionService.deletePermission("USER_CREATE");

        verify(permissionRepository).deleteById("USER_CREATE");
    }
}
