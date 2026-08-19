package app.store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * Danh sách origin được phép, khai báo trong application-{profile}.yaml
     * dưới khoá {@code app.cors.allowed-origins} (phân tách bằng dấu phẩy).
     *
     * <p>Trước đây giá trị này hardcode "http://localhost:3000" ngay trong
     * source, nghĩa là muốn đổi domain phải sửa code rồi build lại image —
     * và bản prod vẫn mang theo origin localhost. Đây là config, không phải
     * code, nên nó thuộc về file profile.
     *
     * <p>Default chỉ là lưới an toàn cho unit test và trường hợp chạy không
     * profile nào; dev/prod đều khai báo tường minh giá trị của mình.
     */
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Use allowedOriginPatterns instead of allowedOrigins when using credentials
        // This allows wildcard headers with credentials
        configuration.setAllowedOriginPatterns(allowedOrigins);

        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Expose headers
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
