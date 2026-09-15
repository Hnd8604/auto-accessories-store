package app.store.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    CustomJwtDecoder customJwtDecoder;
    @Mock
    JwtAuthenticationConverter jwtAuthenticationConverter;
    @Mock
    MessageChannel channel;
    @InjectMocks
    StompAuthChannelInterceptor interceptor;

    private static StompHeaderAccessor accessor(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private static Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static JwtAuthenticationToken authentication(String... authorities) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS512").claim("sub", "u1").build();
        return new JwtAuthenticationToken(jwt, List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }

    private static Principal guest() {
        return new StompAuthChannelInterceptor.GuestPrincipal("guest-1");
    }

    private StompHeaderAccessor subscribe(String destination, Principal user) {
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(user);
        return accessor;
    }

    private StompHeaderAccessor send(String destination, Principal user) {
        StompHeaderAccessor accessor = accessor(StompCommand.SEND);
        accessor.setDestination(destination);
        accessor.setUser(user);
        return accessor;
    }

    @Test
    void connect_withoutToken_shouldSetGuestPrincipal() {
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);

        interceptor.preSend(message(accessor), channel);

        assertThat(accessor.getUser()).isInstanceOf(StompAuthChannelInterceptor.GuestPrincipal.class);
        assertThat(accessor.getUser().getName()).startsWith("guest-");
        assertThat(StompAuthChannelInterceptor.isAdmin(accessor.getUser())).isFalse();
        verifyNoInteractions(customJwtDecoder);
    }

    @Test
    void connect_withValidAdminToken_shouldSetAuthentication() {
        Jwt jwt = Jwt.withTokenValue("admin-token").header("alg", "HS512").claim("sub", "admin").build();
        JwtAuthenticationToken adminAuth = authentication("ROLE_ADMIN");
        when(customJwtDecoder.decode("admin-token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn(adminAuth);

        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer admin-token");

        interceptor.preSend(message(accessor), channel);

        assertThat(accessor.getUser()).isSameAs(adminAuth);
        assertThat(StompAuthChannelInterceptor.isAdmin(accessor.getUser())).isTrue();
    }

    @Test
    void connect_withInvalidToken_shouldReject() {
        when(customJwtDecoder.decode("bad")).thenThrow(new JwtException("Token invalid"));

        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer bad");

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(MessagingException.class);
        assertThat(accessor.getUser()).isNull();
    }

    @Test
    void connect_withNonBearerHeader_shouldReject() {
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Basic abc");

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void subscribeAdminTopic_shouldRejectGuest() {
        assertThatThrownBy(() -> interceptor.preSend(message(subscribe("/topic/admin/new-message", guest())), channel))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void subscribeAdminTopic_shouldRejectNonAdminUser() {
        var userAuth = authentication("ROLE_USER");

        assertThatThrownBy(() -> interceptor.preSend(message(subscribe("/topic/admin/new-message", userAuth)), channel))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void subscribeAdminTopic_shouldAllowAdmin() {
        var adminAuth = authentication("ROLE_ADMIN");

        assertThatCode(() -> interceptor.preSend(message(subscribe("/topic/admin/new-message", adminAuth)), channel))
                .doesNotThrowAnyException();
    }

    @Test
    void subscribeConversationTopicAndErrorQueue_shouldAllowGuest() {
        assertThatCode(() -> {
            interceptor.preSend(message(subscribe("/topic/conversation/c1", guest())), channel);
            interceptor.preSend(message(subscribe("/user/queue/errors", guest())), channel);
        }).doesNotThrowAnyException();
    }

    @Test
    void subscribeUnknownDestination_shouldReject() {
        assertThatThrownBy(() -> interceptor.preSend(message(subscribe("/topic/other", guest())), channel))
                .isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> interceptor.preSend(message(subscribe("/topic/conversation/c1/extra", guest())), channel))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void sendDirectlyToBroker_shouldReject() {
        var adminAuth = authentication("ROLE_ADMIN");

        assertThatThrownBy(() -> interceptor.preSend(message(send("/topic/conversation/c1", guest())), channel))
                .isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> interceptor.preSend(message(send("/topic/admin/new-message", adminAuth)), channel))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void sendToApplicationDestination_shouldAllow() {
        assertThatCode(() -> interceptor.preSend(message(send("/app/chat.send", guest())), channel))
                .doesNotThrowAnyException();
    }
}
