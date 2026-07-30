package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
public class SessionCartServiceTest {

    @Mock
    HttpSession session;
    @InjectMocks
    SessionCartService sessionCartService;

    @Test
    void getSessionCart_shouldCreateEmptyCart_whenSessionHasNone() {
        when(session.getAttribute("CART")).thenReturn(null);

        Map<Long, Integer> cart = sessionCartService.getSessionCart();

        assertThat(cart).isEmpty();
        verify(session).setAttribute(eq("CART"), any()); // giỏ mới được lưu ngược vào session
    }

    @Test
    void getSessionCart_shouldReuseExistingCart() {
        Map<Long, Integer> existing = new HashMap<>(Map.of(1L, 2));
        when(session.getAttribute("CART")).thenReturn(existing);

        assertThat(sessionCartService.getSessionCart()).isSameAs(existing);
        verify(session, never()).setAttribute(any(), any());
    }

    @Test
    void addToCart_shouldAccumulateQuantityForSameProduct() {
        Map<Long, Integer> existing = new HashMap<>(Map.of(1L, 2));
        when(session.getAttribute("CART")).thenReturn(existing);

        sessionCartService.addToCart(1L, 3);
        Map<Long, Integer> cart = sessionCartService.addToCart(2L, 1);

        assertThat(cart).containsEntry(1L, 5).containsEntry(2L, 1);
    }

    @Test
    void removeFromCart_shouldDropProduct() {
        Map<Long, Integer> existing = new HashMap<>(Map.of(1L, 2, 2L, 1));
        when(session.getAttribute("CART")).thenReturn(existing);

        Map<Long, Integer> cart = sessionCartService.removeFromCart(1L);

        assertThat(cart).doesNotContainKey(1L).containsEntry(2L, 1);
    }

    @Test
    void clearCart_shouldRemoveSessionAttribute() {
        sessionCartService.clearCart();

        verify(session).removeAttribute("CART");
    }
}
