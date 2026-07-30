package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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

import app.store.dto.response.NotificationResponse;
import app.store.entity.Notification;
import app.store.entity.User;
import app.store.enums.NotificationType;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.NotificationMapper;
import app.store.repository.NotificationRepository;
import app.store.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    NotificationRepository notificationRepository;
    @Mock
    NotificationMapper notificationMapper;
    @Mock
    UserRepository userRepository;
    @Mock
    SseEmitterService sseEmitterService;
    @InjectMocks
    NotificationService notificationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser() {
        User user = new User();
        user.setId("u1");
        user.setUsername("john");
        return user;
    }

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    // ==================== createNotification ====================

    @Test
    void createNotification_shouldSaveUnread_andPushViaSse() {
        User user = buildUser();
        NotificationResponse response = new NotificationResponse();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationMapper.toNotificationResponse(any())).thenReturn(response);

        var result = notificationService.createNotification(
                "u1", "Đặt hàng thành công", "Đơn #DH1", NotificationType.ORDER_CREATED, "o1");

        assertThat(result).isSameAs(response);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(saved.getReferenceId()).isEqualTo("o1");
        assertThat(saved.isRead()).isFalse();

        verify(sseEmitterService).sendToUser("u1", response);
    }

    @Test
    void createNotification_shouldThrow_whenUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.createNotification(
                "missing", "t", "m", NotificationType.SYSTEM, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);

        verify(notificationRepository, never()).save(any());
        verify(sseEmitterService, never()).sendToUser(any(), any());
    }

    // ==================== getMyNotifications / countUnread ====================

    @Test
    void getMyNotifications_shouldQueryByCurrentUserId() {
        loginAs("john");
        var pageable = PageRequest.of(0, 10);
        Notification notification = Notification.builder().title("t").message("m").build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(buildUser()));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("u1", pageable))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationMapper.toNotificationResponse(notification))
                .thenReturn(new NotificationResponse());

        assertThat(notificationService.getMyNotifications(pageable).getContent()).hasSize(1);
    }

    @Test
    void countUnread_shouldDelegateToRepository() {
        loginAs("john");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(buildUser()));
        when(notificationRepository.countByUserIdAndIsReadFalse("u1")).thenReturn(3L);

        assertThat(notificationService.countUnread()).isEqualTo(3L);
    }

    @Test
    void countUnread_shouldThrow_whenCurrentUserNotFound() {
        loginAs("ghost");

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.countUnread())
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);
    }

    // ==================== markAsRead ====================

    @Test
    void markAsRead_shouldPass_whenOneRowUpdated() {
        loginAs("john");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(buildUser()));
        when(notificationRepository.markAsReadByIdAndUserId("n1", "u1")).thenReturn(1);

        notificationService.markAsRead("n1");

        verify(notificationRepository).markAsReadByIdAndUserId("n1", "u1");
    }

    @Test
    void markAsRead_shouldThrow_whenNotificationNotOwnedByUser() {
        loginAs("john");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(buildUser()));
        when(notificationRepository.markAsReadByIdAndUserId("n-cua-nguoi-khac", "u1")).thenReturn(0);

        assertThatThrownBy(() -> notificationService.markAsRead("n-cua-nguoi-khac"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void markAllAsRead_shouldDelegateToRepository() {
        loginAs("john");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(buildUser()));
        when(notificationRepository.markAllAsReadByUserId("u1")).thenReturn(5);

        notificationService.markAllAsRead();

        verify(notificationRepository).markAllAsReadByUserId("u1");
    }
}
