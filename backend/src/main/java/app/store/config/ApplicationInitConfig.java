package app.store.config;

import app.store.entity.Cart;
import app.store.entity.Role;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.RoleRepository;
import app.store.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.HashSet;

/**
 * Tạo tài khoản quản trị đầu tiên nếu database chưa có.
 *
 * <p>Thông tin đăng nhập lấy từ biến môi trường {@code ADMIN_USERNAME} /
 * {@code ADMIN_PASSWORD} thay vì hardcode. Trước đây mật khẩu {@code "admin"}
 * nằm thẳng trong code nên MỌI môi trường, kể cả production, đều có sẵn một tài
 * khoản toàn quyền với mật khẩu ai cũng đoán được.
 *
 * <p>Quy tắc:
 * <ul>
 *   <li>Đã có admin trong DB → không làm gì, và không đòi hỏi biến môi trường nào
 *       (các lần deploy sau không cần set lại).</li>
 *   <li>Chưa có admin, profile {@code dev} → dùng mật khẩu mặc định cho tiện,
 *       kèm cảnh báo trong log.</li>
 *   <li>Chưa có admin, profile khác → bắt buộc phải có {@code ADMIN_PASSWORD},
 *       thiếu thì dừng khởi động luôn. Thà fail rõ ràng còn hơn chạy tiếp với
 *       mật khẩu mặc định, hoặc im lặng không tạo admin khiến app vô dụng.</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Order(2) // de chay sau RoleInitConfig
public class ApplicationInitConfig {

    static String DEV_DEFAULT_PASSWORD = "admin";

    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    Environment environment;

    // @NonFinal là bắt buộc: @FieldDefaults(makeFinal = true) ở trên khiến
    // @RequiredArgsConstructor kéo các field này thành tham số constructor, mà
    // Lombok không copy @Value sang tham số -> Spring đi tìm bean kiểu String
    // và fail khi khởi động. Để non-final thì Spring inject thẳng vào field.
    @NonFinal
    @Value("${app.admin.username:admin}")
    String adminUsername;

    @NonFinal
    @Value("${app.admin.password:}")
    String adminPassword;

    @NonFinal
    @Value("${app.admin.email:admin@gmail.com}")
    String adminEmail;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByUsername(adminUsername).isPresent()) {
                log.info("Tài khoản quản trị '{}' đã tồn tại, bỏ qua bước khởi tạo", adminUsername);
                return;
            }

            String password = resolveInitialPassword();

            // Role do Flyway tạo sẵn (V2__seed_roles_permissions.sql)
            Role roleAdmin = roleRepository.findById("ADMIN")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));
            Role roleUser = roleRepository.findById("USER")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

            HashSet<Role> roles = new HashSet<>();
            roles.add(roleAdmin);
            roles.add(roleUser);

            User user = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(password))
                    .roles(roles)
                    .email(adminEmail)
                    .fullName("Administrator")
                    .build();

            Cart cart = new Cart();
            user.setCart(cart);
            cart.setUser(user);

            userRepository.save(user);
            log.info("Đã tạo tài khoản quản trị '{}'", adminUsername);
        };
    }

    private String resolveInitialPassword() {
        if (StringUtils.hasText(adminPassword)) {
            return adminPassword;
        }
        if (environment.matchesProfiles("dev")) {
            log.warn("ADMIN_PASSWORD chưa được set, dùng mật khẩu mặc định '{}' cho profile dev. "
                    + "Đừng dùng cấu hình này ngoài máy local.", DEV_DEFAULT_PASSWORD);
            return DEV_DEFAULT_PASSWORD;
        }
        throw new IllegalStateException(
                "Chưa có tài khoản quản trị trong database và biến môi trường ADMIN_PASSWORD chưa được set. "
                        + "Hãy set ADMIN_PASSWORD (và ADMIN_USERNAME nếu muốn đổi tên đăng nhập) rồi khởi động lại.");
    }
}
