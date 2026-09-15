package app.store.service;

import app.store.dto.event.OrderCreatedEvent;
import app.store.dto.event.OrderStatusChangedEvent;
import app.store.enums.NotificationType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderNotificationConsumer {
    MailService mailService;
    NotificationService notificationService;

    @Async
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            // 1. Gửi email thông báo
            mailService.sendOrderCreatedEmail(
                    event.userEmail(),
                    event.recipientName(),
                    event.orderCode(),
                    event.totalPrice()
            );

            // 2. Lưu thông báo vào DB + push real-time qua SSE
            String formattedPrice = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"))
                    .format(event.totalPrice());
            notificationService.createNotification(
                    event.userId(),
                    "Đặt hàng thành công",
                    "Đơn hàng #" + event.orderCode() + " đã được đặt thành công. Tổng tiền: " + formattedPrice,
                    NotificationType.ORDER_CREATED,
                    event.orderId()
            );

            log.info("Processed order-created notification. orderId={}, orderCode={}",
                    event.orderId(), event.orderCode());
        } catch (Exception ex) {
            log.error("Failed to process order-created event orderId={}", event.orderId(), ex);
        }
    }

    @Async
    @EventListener
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        try {
            // 1. Gửi email thông báo
            mailService.sendOrderStatusChangedEmail(
                    event.userEmail(),
                    event.recipientName(),
                    event.orderCode(),
                    event.oldStatus(),
                    event.newStatus()
            );

            // 2. Lưu thông báo vào DB + push real-time qua SSE
            NotificationType type = "CANCELED".equals(event.newStatus())
                    ? NotificationType.ORDER_CANCELED
                    : NotificationType.ORDER_STATUS_CHANGED;

            notificationService.createNotification(
                    event.userId(),
                    "Cập nhật đơn hàng #" + event.orderCode(),
                    "Đơn hàng #" + event.orderCode() + " đã chuyển trạng thái từ "
                            + event.oldStatus() + " sang " + event.newStatus(),
                    type,
                    event.orderId()
            );

            log.info("Processed order-status-changed notification. orderId={}, orderCode={}, {} -> {}",
                    event.orderId(), event.orderCode(), event.oldStatus(), event.newStatus());
        } catch (Exception ex) {
            log.error("Failed to process order-status-changed event orderId={}", event.orderId(), ex);
        }
    }
}
