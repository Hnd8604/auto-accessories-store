// Java
package app.store.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // NOTE: made static final
    private static final String[] PUBLIC_ENDPOINTS = {
            "/users",
            "/session-carts/**",
            "/auth/login", "/auth/google", "/auth/introspect", "/auth/logout", "/auth/refresh", "/auth/password/reset/**",
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
            "/ws/**"
    };

    @Autowired
    private CustomJwtDecoder customJwtDecoder;

    @Autowired
    private RolePermissionAuthoritiesConverter authoritiesConverter; // NOTE: inject custom Redis-backed converter
    @Autowired
    private CorsConfig corsConfig;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .authorizeHttpRequests(req -> req
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.GET, "/products/**", "/categories/**", "/brands/**", "/posts/**", "/post-categories/**", "/product-images/**", "/banners/**", "/services/**", "/service-images/**", "/conversations/*/messages").permitAll()
                        .requestMatchers(HttpMethod.POST,"/products/search**", "/auth/forgot-password", "/auth/reset-password", "/payments/sepay/webhook", "/conversations").permitAll()
                        .anyRequest().authenticated())


                .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .decoder(customJwtDecoder)
                        // NOTE: use bean-configured JwtAuthenticationConverter pulling roles + permissions
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                )
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        // NOTE: custom converter already adds ROLE_ prefix and permissions from Redis
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}