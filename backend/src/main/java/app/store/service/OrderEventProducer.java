package app.store.service;

import app.store.dto.event.OrderCreatedEvent;
import app.store.dto.event.OrderStatusChangedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderEventProducer {
    ApplicationEventPublisher eventPublisher;

    public void publishOrderCreated(OrderCreatedEvent event) {
        eventPublisher.publishEvent(event);
        log.info("Published order-created event. orderId={}, orderCode={}",
                event.orderId(), event.orderCode());
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        eventPublisher.publishEvent(event);
        log.info("Published order-status-changed event. orderId={}, orderCode={}, oldStatus={}, newStatus={}",
                event.orderId(),
                event.orderCode(),
                event.oldStatus(),
                event.newStatus());
    }
}
