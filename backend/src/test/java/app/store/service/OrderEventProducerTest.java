package app.store.service;

import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import app.store.dto.event.OrderCreatedEvent;
import app.store.dto.event.OrderStatusChangedEvent;
import app.store.enums.OrderStatus;

@ExtendWith(MockitoExtension.class)
public class OrderEventProducerTest {

    @Mock
    ApplicationEventPublisher eventPublisher;
    @InjectMocks
    OrderEventProducer orderEventProducer;

    @Test
    void publishOrderCreated_shouldPublishEvent() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("o1").orderCode("DH123").userId("u1")
                .totalPrice(BigDecimal.valueOf(200_000)).build();

        orderEventProducer.publishOrderCreated(event);

        verify(eventPublisher).publishEvent(event);
    }

    @Test
    void publishOrderStatusChanged_shouldPublishEvent() {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .orderId("o1").orderCode("DH123")
                .oldStatus(OrderStatus.PENDING).newStatus(OrderStatus.SHIPPED).build();

        orderEventProducer.publishOrderStatusChanged(event);

        verify(eventPublisher).publishEvent(event);
    }
}
