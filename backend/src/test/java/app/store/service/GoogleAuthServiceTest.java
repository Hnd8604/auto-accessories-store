package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import app.store.dto.request.GoogleAuthRequest;
import app.store.dto.response.auth.AuthenticationResponse;
import app.store.entity.Role;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.UserMapper;
import app.store.repository.RoleRepository;
import app.store.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class GoogleAuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    RoleRepository roleRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    AuthenticationService authenticationService;
    @InjectMocks
    GoogleAuthService googleAuthService;

    private static final String USER_INFO_JSON = """
            {"id":"g-123","email":"john@gmail.com","name":"John Doe","picture":"http://pic/new.png"}
            """;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleAuthService, "clientId", "client-id");
        ReflectionTestUtils.setField(googleAuthService, "clientSecret", "client-secret");
        ReflectionTestUtils.setField(googleAuthService, "redirectUri", "http://localhost/callback");
    }

    /**
     * GoogleAuthService tự tạo `new RestTemplate()` bên trong method nên không thể @Mock được.
     * mockConstruction() chặn mọi lệnh `new RestTemplate()` xảy ra trong khối try-with-resources
     * và thay bằng mock — nhờ đó test không hề gọi mạng thật.
     * Dùng lenient() vì mỗi RestTemplate chỉ dùng 1 trong 2 stub.
     */
    private MockedConstruction<RestTemplate> mockGoogleApi(String userInfoJson) {
        return mockConstruction(RestTemplate.class, (mock, context) -> {
            lenient().when(mock.postForEntity(anyString(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok("{\"access_token\":\"google-token\"}"));
            lenient().when(mock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));
        });
    }

    private GoogleAuthRequest request() {
        return GoogleAuthRequest.builder().code("auth-code").build();
    }

    @Test
    void authenticateWithGoogle_shouldCreateNewUser_whenGoogleIdAndEmailNotFound() {
        try (MockedConstruction<RestTemplate> ignored = mockGoogleApi(USER_INFO_JSON)) {
            when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.empty());
            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(roleRepository.findById("USER"))
                    .thenReturn(Optional.of(Role.builder().name("USER").build()));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authenticationService.generateAuthResponse(any(User.class)))
                    .thenReturn(AuthenticationResponse.builder().accessToken("jwt").authenticated(true).build());

            var response = googleAuthService.authenticateWithGoogle(request());

            assertThat(response.getAccessToken()).isEqualTo("jwt");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User created = captor.getValue();
            assertThat(created.getUsername()).isEqualTo("john");   // lấy phần trước @ của email
            assertThat(created.getGoogleId()).isEqualTo("g-123");
            assertThat(created.getPassword()).isNull();            // user Google không có mật khẩu
            assertThat(created.getCart()).isNotNull();
        }
    }

    @Test
    void authenticateWithGoogle_shouldSuffixUsername_whenUsernameTaken() {
        try (MockedConstruction<RestTemplate> ignored = mockGoogleApi(USER_INFO_JSON)) {
            when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.empty());
            when(userRepository.existsByUsername("john")).thenReturn(true);
            when(roleRepository.findById("USER"))
                    .thenReturn(Optional.of(Role.builder().name("USER").build()));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authenticationService.generateAuthResponse(any(User.class)))
                    .thenReturn(AuthenticationResponse.builder().build());

            googleAuthService.authenticateWithGoogle(request());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getUsername()).startsWith("john_").hasSize(11);
        }
    }

    @Test
    void authenticateWithGoogle_shouldUpdateAvatar_whenGoogleIdExists() {
        User existing = new User();
        existing.setId("u1");
        existing.setUsername("john");
        existing.setGoogleId("g-123");
        existing.setAvatarUrl("http://pic/old.png");

        try (MockedConstruction<RestTemplate> ignored = mockGoogleApi(USER_INFO_JSON)) {
            when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(existing));
            when(authenticationService.generateAuthResponse(existing))
                    .thenReturn(AuthenticationResponse.builder().build());

            googleAuthService.authenticateWithGoogle(request());

            assertThat(existing.getAvatarUrl()).isEqualTo("http://pic/new.png");
            verify(userRepository).save(existing);
            verify(roleRepository, never()).findById(any()); // không tạo user mới
        }
    }

    @Test
    void authenticateWithGoogle_shouldLinkGoogleId_whenEmailAlreadyRegistered() {
        User existing = new User();
        existing.setId("u1");
        existing.setUsername("john");
        existing.setEmail("john@gmail.com");

        try (MockedConstruction<RestTemplate> ignored = mockGoogleApi(USER_INFO_JSON)) {
            when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(existing));
            when(userRepository.save(existing)).thenReturn(existing);
            when(authenticationService.generateAuthResponse(existing))
                    .thenReturn(AuthenticationResponse.builder().build());

            googleAuthService.authenticateWithGoogle(request());

            assertThat(existing.getGoogleId()).isEqualTo("g-123");
            assertThat(existing.getAvatarUrl()).isEqualTo("http://pic/new.png");
            verify(roleRepository, never()).findById(any());
        }
    }

    @Test
    void authenticateWithGoogle_shouldThrowGoogleAuthFailed_whenTokenExchangeFails() {
        try (MockedConstruction<RestTemplate> ignored = mockConstruction(RestTemplate.class,
                (mock, context) -> lenient().when(mock.postForEntity(anyString(), any(), eq(String.class)))
                        .thenThrow(new RestClientException("400 Bad Request")))) {

            assertThatThrownBy(() -> googleAuthService.authenticateWithGoogle(request()))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GOOGLE_AUTH_FAILED);

            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void authenticateWithGoogle_shouldThrowGoogleAuthFailed_whenUserInfoHasNoId() {
        try (MockedConstruction<RestTemplate> ignored = mockGoogleApi("{\"email\":\"john@gmail.com\"}")) {

            assertThatThrownBy(() -> googleAuthService.authenticateWithGoogle(request()))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }
}
