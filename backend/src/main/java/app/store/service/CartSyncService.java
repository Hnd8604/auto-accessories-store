package app.store.service;

import app.store.entity.Cart;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.CartRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartSyncService {
    private final CartService CartService;
    private final CartRepository cartRepository;

    public void syncSessionCart(User user, HttpSession session) {

        Map<Long, Integer> sessionCart =
                (Map<Long, Integer>) session.getAttribute("CART");

        if (sessionCart == null || sessionCart.isEmpty()) return;

        Cart dbCart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_EXISTED));

        for (var entry : sessionCart.entrySet()) {
            CartService.addItem(dbCart, entry.getKey(), entry.getValue());
        }

        // Xóa session cart để tránh sync lại
        session.removeAttribute("CART");
    }
}