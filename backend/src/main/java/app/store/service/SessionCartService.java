package app.store.service;

import jakarta.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SessionCartService {

    HttpSession session; // không cấu hình gì thì mặc định tồn tại 30p

    @SuppressWarnings("unchecked")
    public Map<Long, Integer> getSessionCart() {
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("CART"); // Trong Session key là "CART" value là object Map<Long, Integer>.
                                                                                        // Do value lưu theo map nên trong map lại có key là productId, value là quantity
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("CART", cart);// lưu key value. Key là "CART", value là cart (map rỗng)
        }
        return cart;
    }
    
    public Map<Long, Integer> addToCart(Long productId, int quantity) { // user đăng nhập phát là thêm sp vào giỏ
        Map<Long, Integer> cart = getSessionCart();
        cart.put(productId, cart.getOrDefault(productId, 0) + quantity); //
        return cart;
    }

    public Map<Long, Integer> removeFromCart(Long productId) {
        Map<Long, Integer> cart = getSessionCart();
        cart.remove(productId);
        return cart;
    }

    public void clearCart() {
        session.removeAttribute("CART");
    }
}

