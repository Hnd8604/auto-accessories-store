package app.store.seed;

import app.store.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Nạp role -> permissions từ Postgres lên Redis khi app khởi động.
 *
 * <p>Bản thân dữ liệu role/permission do Flyway tạo (V2__seed_roles_permissions.sql),
 * nhưng tầng phân quyền lại đọc từ Redis key {@code role:<TÊN>:perms}
 * ({@link RolePermissionRepository#findPermissionsByRoles}), mà Flyway thì chỉ nói
 * chuyện được với Postgres. Vì vậy vẫn cần một bước đồng bộ ở phía ứng dụng.
 *
 * <p>Khác với {@code SeedRolePerms} cũ, lớp này KHÔNG giới hạn {@code @Profile("dev")}:
 * Redis là kho rỗng sau mỗi lần restart/flush, nên production cũng phải nạp lại,
 * nếu không mọi request đều bị 403 do không tra ra permission nào.
 *
 * <p>Chạy sau {@link ApplicationReadyEvent} thay vì {@link ApplicationRunner} để
 * Redis lỗi không làm sập tiến trình khởi động — quyền sẽ thiếu cho tới lần sync
 * kế tiếp, nhưng app vẫn lên và log cảnh báo rõ ràng.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class RolePermissionRedisSync {

    private final RolePermissionRepository rolePermissionRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        try {
            rolePermissionRepository.syncAllRolesFromDb();
            log.info("Đã đồng bộ role -> permissions từ Postgres sang Redis");
        } catch (Exception e) {
            log.error("Không đồng bộ được role -> permissions sang Redis. "
                    + "Phân quyền sẽ không hoạt động cho tới khi Redis sẵn sàng.", e);
        }
    }
}
