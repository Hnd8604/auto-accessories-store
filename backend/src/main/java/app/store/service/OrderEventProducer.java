package app.store.service;

import app.store.dto.event.OrderCreatedEvent;
import app.store.dto.event.OrderStatusChangedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class OrderEventProducer {
    final KafkaTemplate<String, Object> kafkaTemplate; // param 1: topic, param 2:key, param 3:content

    @Value("${app.kafka.topics.order-created}")
    String orderCreatedTopic;

    @Value("${app.kafka.topics.order-status-changed}")
    String orderStatusChangedTopic;

    public void publishOrderCreated(OrderCreatedEvent event) { // key giúp đảm bảo message được gửi đến đúng partition
        kafkaTemplate.send(orderCreatedTopic, event.getOrderId(), event); // param 1: topic, param 2:key, param 3:content
        log.info("Published order-created event. topic={}, orderId={}, orderCode={}",
                orderCreatedTopic, event.getOrderId(), event.getOrderCode());
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        kafkaTemplate.send(orderStatusChangedTopic, event.getOrderId(), event);
        log.info("Published order-status-changed event. topic={}, orderId={}, orderCode={}, oldStatus={}, newStatus={}",
                orderStatusChangedTopic,
                event.getOrderId(),
                event.getOrderCode(),
                event.getOldStatus(),
                event.getNewStatus());
    }
}
