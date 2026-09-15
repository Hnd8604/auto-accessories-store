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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;

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
        throw new IllegalStateException(
                "Chưa có tài khoản quản trị trong database và biến môi trường ADMIN_PASSWORD chưa được set. "
                        + "Hãy set ADMIN_PASSWORD (và ADMIN_USERNAME nếu muốn đổi tên đăng nhập) rồi khởi động lại.");
    }
}
