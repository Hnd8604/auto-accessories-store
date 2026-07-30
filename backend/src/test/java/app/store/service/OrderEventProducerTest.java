package app.store.service;

import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import app.store.dto.event.OrderCreatedEvent;
import app.store.dto.event.OrderStatusChangedEvent;

@ExtendWith(MockitoExtension.class)
public class OrderEventProducerTest {

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks
    OrderEventProducer orderEventProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderEventProducer, "orderCreatedTopic", "order-created");
        ReflectionTestUtils.setField(orderEventProducer, "orderStatusChangedTopic", "order-status-changed");
    }

    @Test
    void publishOrderCreated_shouldSendToTopic_withOrderIdAsKey() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("o1").orderCode("DH123").userId("u1")
                .totalPrice(BigDecimal.valueOf(200_000)).build();

        orderEventProducer.publishOrderCreated(event);

        // key = orderId để mọi event của cùng 1 đơn luôn vào cùng partition (giữ đúng thứ tự)
        verify(kafkaTemplate).send("order-created", "o1", event);
    }

    @Test
    void publishOrderStatusChanged_shouldSendToTopic_withOrderIdAsKey() {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .orderId("o1").orderCode("DH123")
                .oldStatus("PENDING").newStatus("SHIPPING").build();

        orderEventProducer.publishOrderStatusChanged(event);

        verify(kafkaTemplate).send("order-status-changed", "o1", event);
    }
}
