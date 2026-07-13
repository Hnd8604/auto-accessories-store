package app.store.config;

import java.util.*;
import java.util.stream.Collectors;

import app.store.repository.RolePermissionRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Chuyển đổi JWT → Collection<GrantedAuthority> cho Spring Security
 *
 * 1) Lấy role từ claim "roles" (nếu có) hoặc fallback sang "scope"/"scp"
 * 2) Map role thành ROLE_* authority để dùng với @PreAuthorize("hasRole('ADMIN')")
 * 3) Lấy permissions từ Redis theo các role, map thành authority cho @PreAuthorize("hasAuthority('PERMISSION')")
 */
@Component
public class RolePermissionAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final RolePermissionRepository repo;

    public RolePermissionAuthoritiesConverter(RolePermissionRepository repo) {
        this.repo = repo;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        // 1) Lấy từ claim "roles" nếu có
        Object rolesClaim = jwt.getClaims().get("roles");
        if (rolesClaim instanceof Collection<?> c) {
            for (Object o : c) {
                if (o != null) roles.add(o.toString().trim());
            }
        } else {
            // fallback: scope/scp
            String scope = Optional.ofNullable(jwt.getClaimAsString("scope"))
                    .orElse(jwt.getClaimAsString("scp"));
            if (scope != null) {
                for (String s : scope.split(" ")) {
                    String v = s.trim();
                    if (!v.isBlank()) {
                        roles.add(v); // giữ ROLE_ADMIN, ROLE_USER nguyên bản
                    }
                }
            }
        }

        // 2) Map roles sang GrantedAuthority
        Set<GrantedAuthority> authorities = roles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(SimpleGrantedAuthority::new) // giữ nguyên prefix ROLE_ nếu token đã có
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3) Lấy permissions từ Redis, map thành authority
        try {
            Set<String> permissions = new HashSet<>();
            for (String role : roles) {
                if (role == null || role.isBlank()) continue;
                String roleKey = role.startsWith("ROLE_") ? role.substring(5) : role;
                Set<String> perms = repo.findPermissionsByRoles(Set.of(roleKey));
                permissions.addAll(perms);
            }
            for (String p : permissions) {
                if (p != null && !p.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(p));
                }
            }
        } catch (Exception ex) {
            // Fail-soft: nếu Redis down, vẫn giữ role authorities
        }

        return authorities;
    }
}
