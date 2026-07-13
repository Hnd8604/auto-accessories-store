package app.store.service;

import app.store.dto.response.NotificationResponse;
import app.store.entity.Notification;
import app.store.entity.User;
import app.store.enums.NotificationType;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.NotificationMapper;
import app.store.repository.NotificationRepository;
import app.store.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationService {

    NotificationRepository notificationRepository;
    NotificationMapper notificationMapper;
    UserRepository userRepository;
    SseEmitterService sseEmitterService;
    public NotificationResponse createNotification(String userId, String title, String message,
                                                   NotificationType type, String referenceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        NotificationResponse response = notificationMapper.toNotificationResponse(notification);

        // Push real-time qua SSE đến tất cả các tab/device đang mở của user
        sseEmitterService.sendToUser(userId, response);
        log.info("Notification created and pushed. userId={}, type={}, referenceId={}", userId, type, referenceId);

        return response;
    }
    @PreAuthorize("hasAuthority('NOTIFICATION_GET_MY')")
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        String userId = getCurrentUserId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toNotificationResponse);
    }
    @PreAuthorize("hasAuthority('NOTIFICATION_GET_MY')")
    public long countUnread() {
        String userId = getCurrentUserId();
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    @PreAuthorize("hasAuthority('NOTIFICATION_GET_MY')")
    @Transactional
    public void markAsRead(String notificationId) {
        String userId = getCurrentUserId();
        int updated = notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
        if (updated == 0) {
            throw new AppException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }
    @PreAuthorize("hasAuthority('NOTIFICATION_GET_MY')")
    @Transactional
    public void markAllAsRead() {
        String userId = getCurrentUserId();
        int count = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked {} notifications as read for userId={}", count, userId);
    }

    private String getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return user.getId();
    }
}
