package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.store.entity.Product;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
public class SessionCartServiceTest {

    @Mock
    HttpSession session;
    @Mock
    ProductRepository productRepository;
    @InjectMocks
    SessionCartService sessionCartService;

    private Product buildProduct(Long id, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setStockQuantity(stock);
        return product;
    }

    private void stubProduct(Long id, int stock) {
        when(productRepository.findById(id)).thenReturn(Optional.of(buildProduct(id, stock)));
    }

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
        stubProduct(1L, 10);
        stubProduct(2L, 10);

        sessionCartService.addToCart(1L, 3);
        Map<Long, Integer> cart = sessionCartService.addToCart(2L, 1);

        assertThat(cart).containsEntry(1L, 5).containsEntry(2L, 1);
    }

    @Test
    void addToCart_shouldRemoveProduct_whenNegativeDeltaDropsToZero() {
        Map<Long, Integer> existing = new HashMap<>(Map.of(1L, 2, 2L, 1));
        when(session.getAttribute("CART")).thenReturn(existing);
        stubProduct(1L, 10);

        // frontend giảm số lượng bằng delta âm; giảm về 0 nghĩa là bỏ khỏi giỏ
        Map<Long, Integer> cart = sessionCartService.addToCart(1L, -2);

        assertThat(cart).doesNotContainKey(1L).containsEntry(2L, 1);
    }

    @Test
    void addToCart_shouldThrowInsufficientStock_whenResultExceedsStock() {
        Map<Long, Integer> existing = new HashMap<>(Map.of(1L, 2));
        when(session.getAttribute("CART")).thenReturn(existing);
        stubProduct(1L, 3);

        assertThatThrownBy(() -> sessionCartService.addToCart(1L, 2)) // 2 + 2 = 4 > stock 3
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);

        assertThat(existing).containsEntry(1L, 2); // giỏ giữ nguyên
    }

    @Test
    void addToCart_shouldThrowInsufficientStock_whenProductOutOfStock() {
        when(session.getAttribute("CART")).thenReturn(new HashMap<Long, Integer>());
        stubProduct(1L, 0);

        assertThatThrownBy(() -> sessionCartService.addToCart(1L, 1))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    void addToCart_shouldThrow_whenProductNotExists() {
        when(session.getAttribute("CART")).thenReturn(new HashMap<Long, Integer>());
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionCartService.addToCart(99L, 1))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_EXISTED);
    }

    @Test
    void addToCart_shouldThrowInvalidQuantity_whenDeltaTooLarge() {
        when(session.getAttribute("CART")).thenReturn(new HashMap<Long, Integer>());

        assertThatThrownBy(() -> sessionCartService.addToCart(1L, 1_000))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_QUANTITY);

        verify(productRepository, never()).findById(any()); // chặn trước khi chạm DB
    }

    @Test
    void addToCart_shouldDoNothing_whenDeltaIsZero() {
        Map<Long, Integer> existing = new HashMap<>(Map.of(1L, 2));
        when(session.getAttribute("CART")).thenReturn(existing);

        assertThat(sessionCartService.addToCart(1L, 0)).containsEntry(1L, 2);

        verify(productRepository, never()).findById(any());
    }

    @Test
    void addToCart_shouldRejectNewProduct_whenCartHasTooManyDistinctItems() {
        Map<Long, Integer> full = new HashMap<>();
        for (long id = 1; id <= 50; id++) {
            full.put(id, 1);
        }
        when(session.getAttribute("CART")).thenReturn(full);
        stubProduct(999L, 10);

        assertThatThrownBy(() -> sessionCartService.addToCart(999L, 1))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_QUANTITY);

        assertThat(full).hasSize(50);
    }

    @Test
    void addToCart_shouldStillAllowIncreasing_whenCartIsFull() {
        Map<Long, Integer> full = new HashMap<>();
        for (long id = 1; id <= 50; id++) {
            full.put(id, 1);
        }
        when(session.getAttribute("CART")).thenReturn(full);
        stubProduct(1L, 10);

        // giỏ đầy chỗ nhưng tăng số lượng sản phẩm đã có thì vẫn được
        assertThat(sessionCartService.addToCart(1L, 1)).containsEntry(1L, 2);
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
