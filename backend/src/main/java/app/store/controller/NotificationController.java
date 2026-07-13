package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.response.NotificationResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.UserRepository;
import app.store.service.NotificationService;
import app.store.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Notification Management", description = "APIs for managing user notifications and real-time SSE stream")
public class NotificationController {

    NotificationService notificationService;
    SseEmitterService sseEmitterService;
    UserRepository userRepository;

    /**
     * SSE endpoint: Frontend kết nối để nhận thông báo real-time
     * Ví dụ JS: const eventSource = new EventSource('/api/v1/notifications/stream', { headers: { Authorization: 'Bearer ...' } });
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE) // định nghĩa content type trả về fe 
    @Operation(
            summary = "Subscribe to real-time notifications",
            description = "Opens an SSE (Server-Sent Events) connection to receive real-time notifications. " +
                    "The connection stays open and pushes new notifications automatically."
    )
    public SseEmitter streamNotifications() {
        String userId = getCurrentUserId();
        return sseEmitterService.createEmitter(userId);
    }

    @GetMapping
    @Operation(
            summary = "Get my notifications",
            description = "Retrieves all notifications for the authenticated user with pagination, ordered by most recent first."
    )
    ApiResponse<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<NotificationResponse>>builder()
                .result(notificationService.getMyNotifications(pageable))
                .message(ResponseMessage.GET_NOTIFICATIONS_SUCCESS)
                .build();
    }

    @GetMapping("/unread-count")
    @Operation(
            summary = "Get unread notification count",
            description = "Returns the count of unread notifications for the authenticated user."
    )
    ApiResponse<Long> getUnreadCount() {
        return ApiResponse.<Long>builder()
                .result(notificationService.countUnread())
                .message(ResponseMessage.GET_UNREAD_COUNT_SUCCESS)
                .build();
    }

    @PutMapping("/{notificationId}/read")
    @Operation(
            summary = "Mark notification as read",
            description = "Marks a specific notification as read for the authenticated user."
    )
    ApiResponse<Void> markAsRead(@PathVariable String notificationId) {
        notificationService.markAsRead(notificationId);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.MARK_NOTIFICATION_READ_SUCCESS)
                .build();
    }

    @PutMapping("/read-all")
    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks all notifications as read for the authenticated user."
    )
    ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.MARK_ALL_NOTIFICATIONS_READ_SUCCESS)
                .build();
    }

    private String getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return user.getId();
    }
}
