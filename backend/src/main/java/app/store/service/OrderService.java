package app.store.service;

import app.store.dto.request.OrderCreationRequest;

import app.store.dto.request.OrderDetailRequest;
import app.store.dto.request.OrderUpdateByAdminRequest;
import app.store.dto.request.OrderUpdateByUserRequest;
import app.store.dto.response.OrderResponse;
import app.store.dto.event.OrderCreatedEvent;
import app.store.dto.event.OrderStatusChangedEvent;
import app.store.entity.*;
import app.store.enums.OrderStatus;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.OrderMapper;
import app.store.repository.*;
import app.store.service.OrderEventProducer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderService {
    OrderRepository orderRepository;
    OrderMapper orderMapper;
    UserRepository userRepository;
    CartRepository cartRepository;
    ProductRepository productRepository;
    CartItemRepository cartItemRepository;
    PaymentService paymentService;
    OrderEventProducer orderEventProducer;
    @PreAuthorize("hasAuthority('ORDER_GET_ALL')")
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toOrderResponse);
    }
    @PreAuthorize("hasAuthority('ORDER_GET_BY_ID')")
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_EXISTED));
        return orderMapper.toOrderResponse(order);
    }
    @PreAuthorize("hasAuthority('ORDER_GET_MY_ORDER')")
    public List<OrderResponse> getMyOrder() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        List<Order> orders = orderRepository.getOrderByUserName(name);
        return orders.stream()
                .map(orderMapper::toOrderResponse).toList();
    }
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public OrderResponse createOrderFromCart(OrderCreationRequest orderRequest) {
        Order order = orderMapper.createOrder(orderRequest);
        User user = userRepository.findById(orderRequest.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Generate unique orderCode
        order.setOrderCode(paymentService.generateOrderCode());

        // Set paymentMethod (default COD nếu không chỉ định)
        if (orderRequest.getPaymentMethod() != null) {
            order.setPaymentMethod(orderRequest.getPaymentMethod());
        } else {
            order.setPaymentMethod(app.store.enums.PaymentMethod.COD);
        }

        // lấy cartId theo UserId, lấy cartItem theo cartId
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_EXISTED));
        List<OrderDetail> orderDetails = new ArrayList<>();

        BigDecimal totalPrice = new BigDecimal(0);
        for( OrderDetailRequest orderDetailRequest : orderRequest.getOrderDetails()) { // tao tung order detail tu request
            Product product = productRepository.findById(orderDetailRequest.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

            if(orderDetailRequest.getQuantity() <= 0 || orderDetailRequest.getQuantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException("Quantity is not valid for product: " + product.getName());
            }
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setProduct(product);
            orderDetail.setQuantity(orderDetailRequest.getQuantity());
            orderDetail.setOrder(order);
            orderDetails.add(orderDetail);


            // cong tien
            totalPrice = totalPrice.add(product.getUnitPrice().multiply(BigDecimal.valueOf(orderDetailRequest.getQuantity())));

            // giam so luong trong gio hang
            // tu cart->cartItem-> product -> giam so luong product trong cartItem, neu = 0 thi xoa luon cartItem di
            CartItem cartItem = cart.getCartItems().stream()
                    .filter(item -> item.getProduct().getId().equals(product.getId()))
                    .findFirst()
                    .orElse(null);
            if(cartItem != null) {
                cartItem.setQuantity(cartItem.getQuantity() - orderDetailRequest.getQuantity());

                if (cartItem.getQuantity() == 0) {
                    cart.getCartItems().remove(cartItem);
                    cartItemRepository.delete(cartItem);
                }
            }
            else {
                throw new AppException(ErrorCode.CART_ITEM_NOT_EXISTED);
            }

        // giam so luong product
            product.setStockQuantity(product.getStockQuantity() - orderDetailRequest.getQuantity());
        }
        order.setOrderDetails(orderDetails);
        order.setTotalPrice(totalPrice);
        order.setUser(user);
        Order savedOrder = orderRepository.save(order);

        try {
            orderEventProducer.publishOrderCreated(
                    OrderCreatedEvent.builder()
                            .orderId(savedOrder.getId())
                            .orderCode(savedOrder.getOrderCode())
                            .userId(user.getId())
                            .userEmail(user.getEmail())
                            .recipientName(savedOrder.getNameRecipient())
                            .totalPrice(savedOrder.getTotalPrice())
                            .paymentMethod(savedOrder.getPaymentMethod().name())
                            .createdAt(savedOrder.getCreatedAt())
                            .build()
            );
        } catch (Exception ex) {
            log.error("Failed to publish order-created event for orderCode={}", savedOrder.getOrderCode(), ex);
        }

        return orderMapper.toOrderResponse(savedOrder);
    }
    @PreAuthorize("hasAuthority('ORDER_UPDATE_BY_ADMIN')")
    public OrderResponse updateOrderByAdmin(String orderId, OrderUpdateByAdminRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_EXISTED));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(request.getOrderStatus());
        order.setPaymentStatus(request.getPaymentStatus());
        Order savedOrder = orderRepository.save(order);

        if (oldStatus != savedOrder.getStatus()) {
            publishOrderStatusChangedEvent(savedOrder, oldStatus, savedOrder.getStatus());
        }

        return orderMapper.toOrderResponse(savedOrder);
    }
    @PreAuthorize("hasAuthority('ORDER_UPDATE_BY_USER')")
    public OrderResponse updateOrderByUser(String orderId, OrderUpdateByUserRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_EXISTED));
        order.setNameRecipient(request.getNameRecipient());
        order.setAddressRecipient(request.getAddressRecipient());
        order.setPhoneRecipient(request.getPhoneRecipient());
        order.setNote(request.getNote());
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }
    @PreAuthorize("hasAuthority('ORDER_CANCEL')")
    public OrderResponse cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_EXISTED));
        OrderStatus oldStatus = order.getStatus();
        if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PROCESSING) {
            order.setStatus(OrderStatus.CANCELED);

        }
        else {
            throw new RuntimeException("Only orders with status PENDING or PROCESSING can be canceled");
        }
        List<OrderDetail> orderDetails = order.getOrderDetails();
        for( OrderDetail orderDetail : orderDetails ) {
            Product product = orderDetail.getProduct();
            product.setStockQuantity(product.getStockQuantity() + orderDetail.getQuantity());
        }
        Order savedOrder = orderRepository.save(order);
        publishOrderStatusChangedEvent(savedOrder, oldStatus, savedOrder.getStatus());

        return orderMapper.toOrderResponse(savedOrder);
    }
    @PreAuthorize("hasAuthority('ORDER_DELETE')")
    public void deleteOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_EXISTED));
        orderRepository.delete(order);
    }

    private void publishOrderStatusChangedEvent(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        try {
            orderEventProducer.publishOrderStatusChanged(
                    OrderStatusChangedEvent.builder()
                            .orderId(order.getId())
                            .orderCode(order.getOrderCode())
                            .userId(order.getUser().getId())
                            .userEmail(order.getUser().getEmail())
                            .recipientName(order.getNameRecipient())
                            .oldStatus(oldStatus.name())
                            .newStatus(newStatus.name())
                            .changedAt(LocalDateTime.now())
                            .build()
            );
        } catch (Exception ex) {
            log.error("Failed to publish order-status-changed event for orderCode={}", order.getOrderCode(), ex);
        }
    }
}
