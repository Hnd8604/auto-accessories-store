package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.nimbusds.jwt.SignedJWT;

import app.store.dto.request.ChangePasswordRequest;
import app.store.dto.request.auth.AuthenticationRequest;
import app.store.dto.request.auth.IntrospectRequest;
import app.store.dto.request.auth.LogoutRequest;
import app.store.dto.request.auth.RefreshRequest;
import app.store.dto.request.user.UserCreationRequest;
import app.store.entity.Role;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.UserMapper;
import app.store.repository.InvalidatedRepository;
import app.store.repository.RoleRepository;
import app.store.repository.UserRepository;
import app.store.dto.response.user.UserResponse;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    InvalidatedRepository invalidatedRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    CartSyncService cartSyncService;
    @Mock
    RoleRepository roleRepository;
    @Mock
    HttpSession session;
    @InjectMocks
    AuthenticationService authenticationService;

    // HS512 yêu cầu khoá bí mật tối thiểu 512 bit = 64 ký tự
    private static final String SIGNER_KEY =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @BeforeEach
    void setUp() {
        // Các field @Value không được Mockito tiêm -> phải set thủ công bằng ReflectionTestUtils
        ReflectionTestUtils.setField(authenticationService, "SIGNER_KEY", SIGNER_KEY);
        ReflectionTestUtils.setField(authenticationService, "ACCESS_DURATION", 3600L);
        ReflectionTestUtils.setField(authenticationService, "REFRESH_DURATION", 86400L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser(String rawPassword) {
        User user = new User();
        user.setId("u1");
        user.setUsername("john");
        user.setEmail("john@mail.com");
        user.setPassword(encoder.encode(rawPassword));
        user.setRoles(Set.of(Role.builder().name("USER").build()));
        return user;
    }

    // ==================== register ====================

    @Test
    void register_shouldEncodePassword_setDefaultRole_andCreateCart() {
        UserCreationRequest request = UserCreationRequest.builder()
                .username("john").email("john@mail.com").password("secret123")
                .build();
        User mapped = new User();
        mapped.setUsername("john");
        mapped.setEmail("john@mail.com");
        Role roleUser = Role.builder().name("USER").build();

        when(userMapper.toUser(request)).thenReturn(mapped);
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@mail.com")).thenReturn(false);
        when(roleRepository.findById("USER")).thenReturn(Optional.of(roleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(new UserResponse());

        authenticationService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isNotEqualTo("secret123"); // đã được hash
        assertThat(encoder.matches("secret123", saved.getPassword())).isTrue();
        assertThat(saved.getRoles()).containsExactly(roleUser);
        assertThat(saved.getCart()).isNotNull();
        assertThat(saved.getCart().getUser()).isSameAs(saved); // quan hệ 2 chiều
    }

    @Test
    void register_shouldThrow_whenUsernameExisted() {
        UserCreationRequest request = UserCreationRequest.builder().username("john").build();
        User mapped = new User();
        mapped.setUsername("john");

        when(userMapper.toUser(request)).thenReturn(mapped);
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_EXISTED);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldThrow_whenEmailExisted() {
        UserCreationRequest request = UserCreationRequest.builder().email("john@mail.com").build();
        User mapped = new User();
        mapped.setEmail("john@mail.com");

        when(userMapper.toUser(request)).thenReturn(mapped);
        when(userRepository.existsByEmail("john@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_EXISTED);
    }

    @Test
    void register_shouldThrow_whenDefaultRoleMissing() {
        UserCreationRequest request = UserCreationRequest.builder().password("secret123").build();

        when(userMapper.toUser(request)).thenReturn(new User());
        when(roleRepository.findById("USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_NOT_EXISTED);
    }

    // ==================== authenticate ====================

    @Test
    void authenticate_shouldReturnTokens_andSyncSessionCart() throws Exception {
        User user = buildUser("secret123");
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john@mail.com").password("secret123").build();

        when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse());

        var response = authenticationService.authenticate(request, session);

        assertThat(response.isAuthenticated()).isTrue();
        // Token thật -> parse ra để kiểm claim
        var claims = SignedJWT.parse(response.getAccessToken()).getJWTClaimsSet();
        assertThat(claims.getSubject()).isEqualTo("john");
        assertThat(claims.getClaim("type")).isEqualTo("accessToken");
        assertThat(claims.getStringClaim("scope")).contains("ROLE_USER");
        assertThat(SignedJWT.parse(response.getRefreshToken()).getJWTClaimsSet().getClaim("type"))
                .isEqualTo("refreshToken");
        verify(cartSyncService).syncSessionCart(user, session);
    }

    @Test
    void authenticate_shouldThrow_whenPasswordWrong() {
        User user = buildUser("secret123");
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john@mail.com").password("sai-mat-khau").build();

        when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.authenticate(request, session))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);

        verify(cartSyncService, never()).syncSessionCart(any(), any());
    }

    @Test
    void authenticate_shouldThrow_whenEmailNotFound() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("missing@mail.com").password("x").build();

        when(userRepository.findByEmail("missing@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate(request, session))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);
    }

    // ==================== refreshToken ====================

    @Test
    void refreshToken_shouldIssueNewAccessToken() throws Exception {
        User user = buildUser("secret123");
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse());
        String refreshToken = authenticationService.generateAuthResponse(user).getRefreshToken();

        when(invalidatedRepository.existsById(any())).thenReturn(false);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        var response = authenticationService.refreshToken(
                RefreshRequest.builder().refreshToken(refreshToken).build());

        assertThat(response.isAuthenticated()).isTrue();
        assertThat(SignedJWT.parse(response.getAccessToken()).getJWTClaimsSet().getClaim("type"))
                .isEqualTo("accessToken");
    }

    @Test
    void refreshToken_shouldThrow_whenAccessTokenUsedInstead() {
        User user = buildUser("secret123");
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse());
        String accessToken = authenticationService.generateAuthResponse(user).getAccessToken();

        when(invalidatedRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.refreshToken(
                RefreshRequest.builder().refreshToken(accessToken).build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void refreshToken_shouldThrow_whenTokenAlreadyInvalidated() {
        User user = buildUser("secret123");
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse());
        String refreshToken = authenticationService.generateAuthResponse(user).getRefreshToken();

        when(invalidatedRepository.existsById(any())).thenReturn(true); // đã logout

        assertThatThrownBy(() -> authenticationService.refreshToken(
                RefreshRequest.builder().refreshToken(refreshToken).build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    // ==================== introspect / logout ====================

    @Test
    void introspect_shouldReturnValidTrue_forFreshToken() throws Exception {
        User user = buildUser("secret123");
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse());
        String accessToken = authenticationService.generateAuthResponse(user).getAccessToken();

        when(invalidatedRepository.existsById(any())).thenReturn(false);

        var response = authenticationService.introspect(
                IntrospectRequest.builder().token(accessToken).build());

        assertThat(response.isValid()).isTrue();
    }

    @Test
    void introspect_shouldReturnValidFalse_whenTokenInvalidated() throws Exception {
        User user = buildUser("secret123");
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse());
        String accessToken = authenticationService.generateAuthResponse(user).getAccessToken();

        when(invalidatedRepository.existsById(any())).thenReturn(true);

        var response = authenticationService.introspect(
                IntrospectRequest.builder().token(accessToken).build());

        assertThat(response.isValid()).isFalse();
    }

    @Test
    void logout_shouldSaveBothTokensToBlacklist() throws Exception {
        User user = buildUser("secret123");
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse());
        var tokens = authenticationService.generateAuthResponse(user);

        when(invalidatedRepository.existsById(any())).thenReturn(false);

        authenticationService.logout(LogoutRequest.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .build());

        verify(invalidatedRepository, times(2)).save(any());
    }

    // ==================== changePassword ====================

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, java.util.List.of()));
    }

    @Test
    void changePassword_shouldSaveNewHashedPassword() {
        loginAs("john");
        User user = buildUser("old-password");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        authenticationService.changePassword(ChangePasswordRequest.builder()
                .currentPassword("old-password")
                .newPassword("new-password")
                .confirmPassword("new-password")
                .build());

        verify(userRepository).save(user);
        assertThat(encoder.matches("new-password", user.getPassword())).isTrue();
    }

    @Test
    void changePassword_shouldThrow_whenCurrentPasswordWrong() {
        loginAs("john");
        User user = buildUser("old-password");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.changePassword(ChangePasswordRequest.builder()
                .currentPassword("sai-mat-khau")
                .newPassword("new-password")
                .confirmPassword("new-password")
                .build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.WRONG_CURRENT_PASSWORD);

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_shouldThrow_whenNewPasswordSameAsCurrent() {
        loginAs("john");
        User user = buildUser("old-password");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.changePassword(ChangePasswordRequest.builder()
                .currentPassword("old-password")
                .newPassword("old-password")
                .confirmPassword("old-password")
                .build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NEW_PASSWORD_SAME_AS_CURRENT);
    }

    @Test
    void changePassword_shouldThrow_whenConfirmationMismatch() {
        loginAs("john");
        User user = buildUser("old-password");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.changePassword(ChangePasswordRequest.builder()
                .currentPassword("old-password")
                .newPassword("new-password")
                .confirmPassword("khac-nhau")
                .build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_shouldThrow_whenUserNotFound() {
        loginAs("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.changePassword(ChangePasswordRequest.builder()
                .currentPassword("a").newPassword("b").confirmPassword("b").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);
    }
}
