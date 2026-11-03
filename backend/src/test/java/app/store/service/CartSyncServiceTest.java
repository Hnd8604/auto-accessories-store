package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.store.entity.Cart;
import app.store.entity.Product;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.CartRepository;
import app.store.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
public class CartSyncServiceTest {

    @Mock
    CartService cartService;
    @Mock
    CartRepository cartRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    HttpSession session;
    @InjectMocks
    CartSyncService cartSyncService;

    private User buildUser() {
        User user = new User();
        user.setId("u1");
        return user;
    }

    private Product buildProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    @Test
    void syncSessionCart_shouldMoveEveryItemToDbCart_andClearSession() {
        User user = buildUser();
        Cart dbCart = new Cart();
        dbCart.setId(10L);
        Map<Long, Integer> sessionCart = new HashMap<>();
        sessionCart.put(1L, 2);
        sessionCart.put(2L, 3);

        when(session.getAttribute("CART")).thenReturn(sessionCart);
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.of(dbCart));
        when(productRepository.findAllById(sessionCart.keySet()))
                .thenReturn(List.of(buildProduct(1L), buildProduct(2L)));

        cartSyncService.syncSessionCart(user, session);

        ArgumentCaptor<Long> productIds = ArgumentCaptor.forClass(Long.class);
        verify(cartService, times(2)).mergeItem(eq(dbCart), productIds.capture(), anyInt());
        assertThat(productIds.getAllValues()).containsExactlyInAnyOrder(1L, 2L);
        verify(cartService).mergeItem(dbCart, 1L, 2);
        verify(cartService).mergeItem(dbCart, 2L, 3);

        verify(session).removeAttribute("CART"); // tránh sync lặp lại ở lần đăng nhập sau
    }

    @Test
    void syncSessionCart_shouldSkipFailingItem_andStillSyncTheRest() {
        User user = buildUser();
        Cart dbCart = new Cart();
        dbCart.setId(10L);
        Map<Long, Integer> sessionCart = new HashMap<>();
        sessionCart.put(1L, 2);
        sessionCart.put(2L, 3);

        when(session.getAttribute("CART")).thenReturn(sessionCart);
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.of(dbCart));
        when(productRepository.findAllById(sessionCart.keySet()))
                .thenReturn(List.of(buildProduct(1L), buildProduct(2L)));
        when(cartService.mergeItem(dbCart, 1L, 2))
                .thenThrow(new AppException(ErrorCode.INSUFFICIENT_STOCK));

        // Đây là lỗi H4: trước đây exception thoát ra làm hỏng cả request đăng nhập
        assertThatCode(() -> cartSyncService.syncSessionCart(user, session))
                .doesNotThrowAnyException();

        verify(cartService).mergeItem(dbCart, 2L, 3); // item còn lại vẫn được gộp
        verify(session).removeAttribute("CART"); // vẫn xoá, nếu không lỗi lặp lại mỗi lần đăng nhập
    }

    @Test
    void syncSessionCart_shouldSkipProductsThatNoLongerExist() {
        User user = buildUser();
        Cart dbCart = new Cart();
        dbCart.setId(10L);
        Map<Long, Integer> sessionCart = new HashMap<>();
        sessionCart.put(1L, 2);
        sessionCart.put(2L, 3);

        when(session.getAttribute("CART")).thenReturn(sessionCart);
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.of(dbCart));
        when(productRepository.findAllById(sessionCart.keySet()))
                .thenReturn(List.of(buildProduct(2L))); // sản phẩm 1 đã bị xoá

        cartSyncService.syncSessionCart(user, session);

        verify(cartService, never()).mergeItem(any(), eq(1L), anyInt());
        verify(cartService).mergeItem(dbCart, 2L, 3);
        verify(session).removeAttribute("CART");
    }

    @Test
    void syncSessionCart_shouldThrow_whenDbCartMissing() {
        User user = buildUser();
        when(session.getAttribute("CART")).thenReturn(new HashMap<>(Map.of(1L, 2)));
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.empty());

        // Bất biến: mọi đường tạo user đều tạo cart, nên vào được đây là dữ liệu đã hỏng
        assertThatThrownBy(() -> cartSyncService.syncSessionCart(user, session))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_NOT_EXISTED);

        verify(cartService, never()).mergeItem(any(), any(), anyInt());
    }

    @Test
    void syncSessionCart_shouldDoNothing_whenSessionCartNull() {
        when(session.getAttribute("CART")).thenReturn(null);

        cartSyncService.syncSessionCart(buildUser(), session);

        verify(cartService, never()).mergeItem(any(), any(), anyInt());
        verify(session, never()).removeAttribute(any());
    }

    @Test
    void syncSessionCart_shouldDoNothing_whenSessionCartEmpty() {
        when(session.getAttribute("CART")).thenReturn(new HashMap<Long, Integer>());

        cartSyncService.syncSessionCart(buildUser(), session);

        verify(cartRepository, never()).findByUserId(any());
        verify(cartService, never()).mergeItem(any(), any(), anyInt());
    }
}
