package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import app.store.dto.request.user.UserCreationRequest;
import app.store.dto.request.user.UserUpdateRequest;
import app.store.dto.response.user.UserResponse;
import app.store.entity.Role;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.UserMapper;
import app.store.repository.RoleRepository;
import app.store.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    UserMapper userMapper;
    @Mock
    UserRepository userRepository;
    @Mock
    RoleRepository roleRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser() {
        User user = new User();
        user.setId("u1");
        user.setUsername("john");
        user.setEmail("john@mail.com");
        user.setPassword("old-hash");
        return user;
    }

    // ==================== createUser ====================

    @Test
    void createUser_shouldEncodePassword_setRoleUser_andCreateCart() {
        UserCreationRequest request = UserCreationRequest.builder()
                .username("john").email("john@mail.com").password("secret123").build();
        User mapped = new User();
        mapped.setUsername("john");
        mapped.setEmail("john@mail.com");
        Role roleUser = Role.builder().name("USER").build();

        when(userMapper.toUser(request)).thenReturn(mapped);
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(roleRepository.findById("USER")).thenReturn(Optional.of(roleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(new UserResponse());

        userService.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getRoles()).containsExactly(roleUser);
        assertThat(captor.getValue().getCart()).isNotNull();
    }

    @Test
    void createUser_shouldThrow_whenUsernameExisted() {
        UserCreationRequest request = UserCreationRequest.builder().username("john").build();
        User mapped = new User();
        mapped.setUsername("john");

        when(userMapper.toUser(request)).thenReturn(mapped);
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_EXISTED);
    }

    @Test
    void createUser_shouldThrow_whenEmailExisted() {
        UserCreationRequest request = UserCreationRequest.builder().email("john@mail.com").build();
        User mapped = new User();
        mapped.setEmail("john@mail.com");

        when(userMapper.toUser(request)).thenReturn(mapped);
        when(userRepository.existsByEmail("john@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_EXISTED);
    }

    // ==================== getMyInfo ====================

    @Test
    void getMyInfo_shouldReadUsernameFromSecurityContext() {
        User user = buildUser();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", null, List.of()));
        UserResponse expected = new UserResponse();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(expected);

        assertThat(userService.getMyInfo()).isSameAs(expected);
    }

    @Test
    void getMyInfo_shouldThrow_whenUserNotFound() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ghost", null, List.of()));

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyInfo())
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);
    }

    // ==================== updateUser ====================

    @Test
    void updateUser_shouldUpdatePasswordAndRoles_whenProvided() {
        User user = buildUser();
        Role admin = Role.builder().name("ADMIN").build();
        UserUpdateRequest request = UserUpdateRequest.builder()
                .password("new-password").roles(List.of("ADMIN")).build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
        when(roleRepository.findAllById(List.of("ADMIN"))).thenReturn(List.of(admin));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse());

        userService.updateUser("u1", request);

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(user.getRoles()).containsExactly(admin);
        verify(userMapper).updateUser(user, request);
    }

    @Test
    void updateUser_shouldKeepPasswordAndRoles_whenNotProvided() {
        User user = buildUser();
        user.setRoles(Set.of(Role.builder().name("USER").build()));
        UserUpdateRequest request = UserUpdateRequest.builder()
                .password("").roles(List.of()).build(); // rỗng = không đổi

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse());

        userService.updateUser("u1", request);

        assertThat(user.getPassword()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(any());
        verify(roleRepository, never()).findAllById(any());
    }

    @Test
    void updateUser_shouldThrow_whenUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser("missing", UserUpdateRequest.builder().build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);
    }

    // ==================== delete / get ====================

    @Test
    void deleteUser_shouldDelete_whenFound() {
        User user = buildUser();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        userService.deleteUser("u1");

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_shouldThrow_whenNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser("missing"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);

        verify(userRepository, never()).delete(any());
    }

    @Test
    void getUserById_shouldThrow_whenNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById("missing"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);
    }

    @Test
    void getAllUsers_shouldMapEachUserInPage() {
        User user = buildUser();
        var pageable = PageRequest.of(0, 10);

        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse());

        var page = userService.getAllUsers(pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(userMapper).toUserResponse(user);
    }
}
