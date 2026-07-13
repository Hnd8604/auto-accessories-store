package app.store.service;

import app.store.dto.event.OrderCreatedEvent;
import app.store.dto.event.OrderStatusChangedEvent;
import app.store.enums.NotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderNotificationConsumer {
    ObjectMapper objectMapper;
    MailService mailService;
    NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(String payload) {// payload là json string
        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class); // json -> object

            // 1. Gửi email thông báo
            mailService.sendOrderCreatedEmail(
                    event.getUserEmail(),
                    event.getRecipientName(),
                    event.getOrderCode(),
                    event.getTotalPrice()
            );

            // 2. Lưu thông báo vào DB + push real-time qua SSE
            String formattedPrice = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"))
                    .format(event.getTotalPrice());
            notificationService.createNotification(
                    event.getUserId(),
                    "Đặt hàng thành công",
                    "Đơn hàng #" + event.getOrderCode() + " đã được đặt thành công. Tổng tiền: " + formattedPrice,
                    NotificationType.ORDER_CREATED,
                    event.getOrderId()
            );

            log.info("Processed order-created notification. orderId={}, orderCode={}",
                    event.getOrderId(), event.getOrderCode());
        } catch (Exception ex) {
            log.error("Failed to process order-created event payload={}", payload, ex);
            throw new RuntimeException(ex);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.order-status-changed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderStatusChanged(String payload) {
        try {
            OrderStatusChangedEvent event = objectMapper.readValue(payload, OrderStatusChangedEvent.class);

            // 1. Gửi email thông báo
            mailService.sendOrderStatusChangedEmail(
                    event.getUserEmail(),
                    event.getRecipientName(),
                    event.getOrderCode(),
                    event.getOldStatus(),
                    event.getNewStatus()
            );

            // 2. Lưu thông báo vào DB + push real-time qua SSE
            NotificationType type = "CANCELED".equals(event.getNewStatus())
                    ? NotificationType.ORDER_CANCELED
                    : NotificationType.ORDER_STATUS_CHANGED;

            notificationService.createNotification(
                    event.getUserId(),
                    "Cập nhật đơn hàng #" + event.getOrderCode(),
                    "Đơn hàng #" + event.getOrderCode() + " đã chuyển trạng thái từ "
                            + event.getOldStatus() + " sang " + event.getNewStatus(),
                    type,
                    event.getOrderId()
            );

            log.info("Processed order-status-changed notification. orderId={}, orderCode={}, {} -> {}",
                    event.getOrderId(), event.getOrderCode(), event.getOldStatus(), event.getNewStatus());
        } catch (Exception ex) {
            log.error("Failed to process order-status-changed event payload={}", payload, ex);
            throw new RuntimeException(ex);
        }
    }
}
