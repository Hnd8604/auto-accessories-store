package app.store.service;

import app.store.dto.request.CartItemRequest;
import app.store.entity.Cart;
import app.store.entity.User;
import app.store.repository.CartRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartSyncService {
    private final CartService CartService;
    private final CartRepository cartRepository;

    public void syncSessionCart(User user, HttpSession session) {

        Map<Long, Integer> sessionCart =
                (Map<Long, Integer>) session.getAttribute("CART");

        if (sessionCart == null || sessionCart.isEmpty()) return;

        Optional<Cart> dbCart = cartRepository.findByUserId(user.getId());

        for (var entry : sessionCart.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            CartService.addItemToCart(
                    new CartItemRequest(dbCart.get().getId(), productId, quantity)
            );
        }

        // Xóa session cart để tránh sync lại
        session.removeAttribute("CART");
    }
}