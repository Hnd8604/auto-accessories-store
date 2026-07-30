package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.store.enums.NotificationType;

@ExtendWith(MockitoExtension.class)
public class OrderNotificationConsumerTest {

    @Spy
    ObjectMapper objectMapper = new ObjectMapper(); // parse JSON thật, giống lúc chạy production
    @Mock
    MailService mailService;
    @Mock
    NotificationService notificationService;
    @InjectMocks
    OrderNotificationConsumer orderNotificationConsumer;

    private static final String ORDER_CREATED_PAYLOAD = """
            {
              "orderId": "o1",
              "orderCode": "DH123",
              "userId": "u1",
              "userEmail": "john@mail.com",
              "recipientName": "John",
              "totalPrice": 200000,
              "paymentMethod": "COD"
            }
            """;

    private String statusChangedPayload(String newStatus) {
        return """
                {
                  "orderId": "o1",
                  "orderCode": "DH123",
                  "userId": "u1",
                  "userEmail": "john@mail.com",
                  "recipientName": "John",
                  "oldStatus": "PENDING",
                  "newStatus": "%s"
                }
                """.formatted(newStatus);
    }

    @Test
    void handleOrderCreated_shouldSendMail_andCreateNotification() {
        orderNotificationConsumer.handleOrderCreated(ORDER_CREATED_PAYLOAD);

        verify(mailService).sendOrderCreatedEmail(
                "john@mail.com", "John", "DH123", BigDecimal.valueOf(200000));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq("u1"), eq("Đặt hàng thành công"), messageCaptor.capture(),
                eq(NotificationType.ORDER_CREATED), eq("o1"));
        assertThat(messageCaptor.getValue()).contains("DH123").contains("200.000");
    }

    @Test
    void handleOrderStatusChanged_shouldUseStatusChangedType_forNormalStatus() {
        orderNotificationConsumer.handleOrderStatusChanged(statusChangedPayload("SHIPPING"));

        verify(mailService).sendOrderStatusChangedEmail(
                "john@mail.com", "John", "DH123", "PENDING", "SHIPPING");
        verify(notificationService).createNotification(
                eq("u1"), any(), any(), eq(NotificationType.ORDER_STATUS_CHANGED), eq("o1"));
    }

    @Test
    void handleOrderStatusChanged_shouldUseCanceledType_whenOrderCanceled() {
        orderNotificationConsumer.handleOrderStatusChanged(statusChangedPayload("CANCELED"));

        verify(notificationService).createNotification(
                eq("u1"), any(), any(), eq(NotificationType.ORDER_CANCELED), eq("o1"));
    }

    @Test
    void handleOrderCreated_shouldRethrow_whenPayloadInvalid() {
        // Ném lỗi để Kafka retry / đẩy vào dead-letter thay vì nuốt lặng
        assertThatThrownBy(() -> orderNotificationConsumer.handleOrderCreated("{json hỏng}"))
                .isInstanceOf(RuntimeException.class);

        verify(mailService, never()).sendOrderCreatedEmail(any(), any(), any(), any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }
}
