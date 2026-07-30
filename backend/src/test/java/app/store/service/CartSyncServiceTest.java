package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.store.dto.request.CartItemRequest;
import app.store.entity.Cart;
import app.store.entity.User;
import app.store.repository.CartRepository;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
public class CartSyncServiceTest {

    @Mock
    CartService cartService;
    @Mock
    CartRepository cartRepository;
    @Mock
    HttpSession session;
    @InjectMocks
    CartSyncService cartSyncService;

    private User buildUser() {
        User user = new User();
        user.setId("u1");
        return user;
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

        cartSyncService.syncSessionCart(user, session);

        ArgumentCaptor<CartItemRequest> captor = ArgumentCaptor.forClass(CartItemRequest.class);
        verify(cartService, times(2)).addItemToCart(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(r -> r.getCartId().equals(10L))
                .extracting(CartItemRequest::getProductId)
                .containsExactlyInAnyOrder(1L, 2L);

        verify(session).removeAttribute("CART"); // tránh sync lặp lại ở lần đăng nhập sau
    }

    @Test
    void syncSessionCart_shouldDoNothing_whenSessionCartNull() {
        when(session.getAttribute("CART")).thenReturn(null);

        cartSyncService.syncSessionCart(buildUser(), session);

        verify(cartService, never()).addItemToCart(any());
        verify(session, never()).removeAttribute(any());
    }

    @Test
    void syncSessionCart_shouldDoNothing_whenSessionCartEmpty() {
        when(session.getAttribute("CART")).thenReturn(new HashMap<Long, Integer>());

        cartSyncService.syncSessionCart(buildUser(), session);

        verify(cartRepository, never()).findByUserId(any());
        verify(cartService, never()).addItemToCart(any());
    }
}
