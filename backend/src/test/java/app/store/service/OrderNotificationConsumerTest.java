package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.store.dto.event.OrderCreatedEvent;
import app.store.dto.event.OrderStatusChangedEvent;
import app.store.enums.NotificationType;

@ExtendWith(MockitoExtension.class)
public class OrderNotificationConsumerTest {

    @Mock
    MailService mailService;
    @Mock
    NotificationService notificationService;
    @InjectMocks
    OrderNotificationConsumer orderNotificationConsumer;

    private OrderCreatedEvent buildOrderCreatedEvent() {
        return OrderCreatedEvent.builder()
                .orderId("o1")
                .orderCode("DH123")
                .userId("u1")
                .userEmail("john@mail.com")
                .recipientName("John")
                .totalPrice(BigDecimal.valueOf(200_000))
                .paymentMethod("COD")
                .build();
    }

    private OrderStatusChangedEvent buildStatusChangedEvent(String newStatus) {
        return OrderStatusChangedEvent.builder()
                .orderId("o1")
                .orderCode("DH123")
                .userId("u1")
                .userEmail("john@mail.com")
                .recipientName("John")
                .oldStatus("PENDING")
                .newStatus(newStatus)
                .build();
    }

    @Test
    void handleOrderCreated_shouldSendMail_andCreateNotification() {
        OrderCreatedEvent event = buildOrderCreatedEvent();

        orderNotificationConsumer.handleOrderCreated(event);

        verify(mailService).sendOrderCreatedEmail(
                "john@mail.com", "John", "DH123", BigDecimal.valueOf(200_000));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq("u1"), eq("Đặt hàng thành công"), messageCaptor.capture(),
                eq(NotificationType.ORDER_CREATED), eq("o1"));
        assertThat(messageCaptor.getValue()).contains("DH123").contains("200.000");
    }

    @Test
    void handleOrderStatusChanged_shouldUseStatusChangedType_forNormalStatus() {
        OrderStatusChangedEvent event = buildStatusChangedEvent("SHIPPING");

        orderNotificationConsumer.handleOrderStatusChanged(event);

        verify(mailService).sendOrderStatusChangedEmail(
                "john@mail.com", "John", "DH123", "PENDING", "SHIPPING");
        verify(notificationService).createNotification(
                eq("u1"), any(), any(), eq(NotificationType.ORDER_STATUS_CHANGED), eq("o1"));
    }

    @Test
    void handleOrderStatusChanged_shouldUseCanceledType_whenOrderCanceled() {
        OrderStatusChangedEvent event = buildStatusChangedEvent("CANCELED");

        orderNotificationConsumer.handleOrderStatusChanged(event);

        verify(notificationService).createNotification(
                eq("u1"), any(), any(), eq(NotificationType.ORDER_CANCELED), eq("o1"));
    }
}
