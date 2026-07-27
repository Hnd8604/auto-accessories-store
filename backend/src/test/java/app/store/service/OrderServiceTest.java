package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.store.dto.request.OrderCreationRequest;
import app.store.dto.request.OrderDetailRequest;
import app.store.dto.response.OrderResponse;
import app.store.entity.Cart;
import app.store.entity.CartItem;
import app.store.entity.Order;
import app.store.entity.OrderDetail;
import app.store.entity.Product;
import app.store.entity.User;
import app.store.enums.OrderStatus;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.OrderMapper;
import app.store.repository.CartItemRepository;
import app.store.repository.CartRepository;
import app.store.repository.OrderRepository;
import app.store.repository.ProductRepository;
import app.store.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderMapper orderMapper;
    @Mock
    UserRepository userRepository;
    @Mock
    CartRepository cartRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    CartItemRepository cartItemRepository;
    @Mock
    PaymentService paymentService;
    @Mock
    OrderEventProducer orderEventProducer;
    @InjectMocks
    OrderService orderService;

    private User buildUser() {
        User user = new User();
        user.setId("u1");
        user.setEmail("a@b.com");
        return user;
    }

    @Test
    void createOrderFromCart_happyPath_calculatesTotalAndReducesStock() {
        // Arrange
        User user = buildUser();

        Product product = new Product();
        product.setId(1L);
        product.setName("Lốp xe");
        product.setUnitPrice(BigDecimal.valueOf(100_000));
        product.setStockQuantity(10);

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        Cart cart = new Cart();
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        OrderDetailRequest detailRequest = OrderDetailRequest.builder()
                .productId(1L).quantity(2).build();
        OrderCreationRequest request = OrderCreationRequest.builder()
                .userId("u1").orderDetails(List.of(detailRequest)).build();

        when(orderMapper.createOrder(request)).thenReturn(new Order());
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(paymentService.generateOrderCode()).thenReturn("DH123");
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(new OrderResponse());

        // Act
        orderService.createOrderFromCart(request);

        // Assert
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo("200000");
        assertThat(product.getStockQuantity()).isEqualTo(8);
        assertThat(cart.getCartItems()).isEmpty(); // quantity trong giỏ về 0 -> bị xoá
        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void createOrderFromCart_shouldThrow_whenQuantityExceedsStock() {
        User user = buildUser();
        Product product = new Product();
        product.setId(1L);
        product.setStockQuantity(1);
        Cart cart = new Cart();
        cart.setCartItems(new ArrayList<>());

        OrderCreationRequest request = OrderCreationRequest.builder()
                .userId("u1")
                .orderDetails(List.of(OrderDetailRequest.builder().productId(1L).quantity(5).build()))
                .build();

        when(orderMapper.createOrder(request)).thenReturn(new Order());
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrderFromCart(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderFromCart_shouldThrow_whenCartItemNotFound() {
        User user = buildUser();
        Product product = new Product();
        product.setId(1L);
        product.setUnitPrice(BigDecimal.TEN);
        product.setStockQuantity(10);
        Cart cart = new Cart();
        cart.setCartItems(new ArrayList<>()); // giỏ rỗng -> không tìm thấy cart item

        OrderCreationRequest request = OrderCreationRequest.builder()
                .userId("u1")
                .orderDetails(List.of(OrderDetailRequest.builder().productId(1L).quantity(1).build()))
                .build();

        when(orderMapper.createOrder(request)).thenReturn(new Order());
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrderFromCart(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_ITEM_NOT_EXISTED);
    }

    @Test
    void createOrderFromCart_shouldThrow_whenUserNotFound() {
        OrderCreationRequest request = OrderCreationRequest.builder()
                .userId("missing")
                .orderDetails(List.of())
                .build();

        when(orderMapper.createOrder(request)).thenReturn(new Order());
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrderFromCart(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);
    }

    @Test
    void createOrderFromCart_shouldNotFail_whenKafkaPublishThrows() {
        User user = buildUser();
        Product product = new Product();
        product.setId(1L);
        product.setUnitPrice(BigDecimal.TEN);
        product.setStockQuantity(10);
        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        Cart cart = new Cart();
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        OrderCreationRequest request = OrderCreationRequest.builder()
                .userId("u1")
                .orderDetails(List.of(OrderDetailRequest.builder().productId(1L).quantity(1).build()))
                .build();

        when(orderMapper.createOrder(request)).thenReturn(new Order());
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toOrderResponse(any())).thenReturn(new OrderResponse());
        doThrow(new RuntimeException("Kafka down")).when(orderEventProducer).publishOrderCreated(any());

        // Kafka lỗi nhưng đơn hàng vẫn phải tạo thành công (try/catch nuốt lỗi)
        assertThatCode(() -> orderService.createOrderFromCart(request))
                .doesNotThrowAnyException();

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void cancelOrder_shouldThrow_whenStatusNotCancelable() {
        Order order = new Order();
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("o1"))
                .isInstanceOf(RuntimeException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_shouldRestoreStock_whenPending() {
        Product product = new Product();
        product.setStockQuantity(5);
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(3);

        User user = buildUser();
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDetails(List.of(detail));
        order.setUser(user);
        order.setOrderCode("DH123");

        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toOrderResponse(any())).thenReturn(new OrderResponse());

        orderService.cancelOrder("o1");

        assertThat(product.getStockQuantity()).isEqualTo(8);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(orderEventProducer).publishOrderStatusChanged(any());
    }

    @Test
    void getOrderById_shouldThrow_whenNotFound() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("missing"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_EXISTED);
    }

    @Test
    void deleteOrder_shouldThrow_whenNotFound() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder("missing"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_EXISTED);
    }
}
