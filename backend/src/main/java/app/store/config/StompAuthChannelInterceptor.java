package app.store.config;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Xác thực và phân quyền ở tầng STOMP (handshake /ws/** vẫn public vì SockJS không gửi được header Authorization).
 *
 * - CONNECT có "Authorization: Bearer <token>" → user là JwtAuthenticationToken; token sai thì từ chối kết nối
 * - CONNECT không có header → guest (khách chat không đăng nhập)
 * - SUBSCRIBE /topic/admin/** chỉ cho ROLE_ADMIN; destination ngoài danh sách bị chặn
 * - SEND chỉ vào /app/**, không cho client gửi thẳng tới broker (/topic, /queue)
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    static final String BEARER_PREFIX = "Bearer ";
    static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    CustomJwtDecoder customJwtDecoder;
    JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeSubscribe(accessor);
            case SEND -> authorizeSend(accessor);
            default -> {
            }
        }
        return message;
    }

    public static boolean isAdmin(Principal principal) {
        return principal instanceof Authentication authentication
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> ADMIN_AUTHORITY.equals(a.getAuthority()));
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || header.isBlank()) {
            accessor.setUser(new GuestPrincipal("guest-" + UUID.randomUUID()));
            return;
        }
        if (!header.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("Invalid Authorization header");
        }

        try {
            Jwt jwt = customJwtDecoder.decode(header.substring(BEARER_PREFIX.length()));
            AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
            accessor.setUser(authentication);
        } catch (JwtException e) {
            throw new MessagingException("Invalid token");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new MessagingException("Subscription destination is required");
        }

        if (PATH_MATCHER.match("/topic/admin/**", destination)) {
            if (!isAdmin(accessor.getUser())) {
                throw new MessagingException("Access denied to " + destination);
            }
            return;
        }
        if (PATH_MATCHER.match("/topic/conversation/*", destination)
                || "/user/queue/errors".equals(destination)) {
            return;
        }
        throw new MessagingException("Access denied to " + destination);
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !PATH_MATCHER.match("/app/**", destination)) {
            throw new MessagingException("Access denied to " + destination);
        }
    }

    record GuestPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
