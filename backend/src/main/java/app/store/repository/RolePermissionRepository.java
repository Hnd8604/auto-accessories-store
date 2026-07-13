package app.store.repository;

import app.store.entity.Permission;
import app.store.entity.Role;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RolePermissionRepository {

    StringRedisTemplate redis;
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;

    /**
     * Lấy permissions của nhiều role từ Redis
     * @param roles collection role (có thể là "ROLE_ADMIN" hoặc "ADMIN")
     * @return tập hợp permissions
     */
    public Set<String> findPermissionsByRoles(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) return Set.of();

        Set<String> result = new HashSet<>();
        for (String role : roles) {
            if (role == null || role.isBlank()) continue;

            // Loại bỏ prefix ROLE_ nếu có
            String roleKey = role.startsWith("ROLE_") ? role.substring(5) : role;
            String redisKey = "role:" + roleKey + ":perms";

            Set<String> perms = redis.opsForSet().members(redisKey);
            if (perms != null) result.addAll(perms);
        }
        return result;
    }

    /**
     * Đồng bộ permissions của 1 role từ DB sang Redis
     * @param roleName tên role (không cần prefix ROLE_)
     */
    public void syncRolePermissionsFromDb(String roleName) {
        if (roleName == null || roleName.isBlank()) return;

        roleRepository.findById(roleName).ifPresent(role -> {
            // Lấy permissions từ PermissionRepository
            List<Permission> permissions = permissionRepository.getAllByRole(role);
            Set<String> permNames = permissions.stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet());

            String redisKey = "role:" + roleName + ":perms";

            // Xóa dữ liệu cũ
            redis.delete(redisKey);

            // Thêm permissions mới vào Redis
            if (!permNames.isEmpty()) { // nếu không có permission thì không tạo role mới trên redis.
                redis.opsForSet().add(redisKey, permNames.toArray(String[]::new));
            }
        });
    }

    /**
     * Đồng bộ tất cả role → permissions từ DB sang Redis
     */
    public void syncAllRolesFromDb() {
        roleRepository.findAll().forEach(role -> syncRolePermissionsFromDb(role.getName()));
    }

    public void deleteRoleFromRedis(String roleName) {
        if (roleName == null || roleName.isBlank()) return;
        String redisKey = "role:" + roleName + ":perms";
        redis.delete(redisKey);
    }
}
