package app.store.service;

import app.store.entity.Product;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.ProductRepository;
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

    // Key của giỏ tạm trong session, dùng chung với CartSyncService
    public static final String CART_ATTRIBUTE = "CART";

    // Giới hạn số loại sản phẩm để khách chưa đăng nhập không bơm được session
    // phình vô hạn
    private static final int MAX_DISTINCT_ITEMS = 50;

    // Chặn delta vô nghĩa trước khi tính toán
    private static final int MAX_DELTA = 999;

    HttpSession session; // không cấu hình gì thì mặc định tồn tại 30p
    ProductRepository productRepository;

    @SuppressWarnings("unchecked")
    public Map<Long, Integer> getSessionCart() {
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute(CART_ATTRIBUTE); // Trong Session key là
                                                                                             // "CART" value là object
                                                                                             // Map<Long, Integer>.
        // Do value lưu theo map nên trong map lại có key là productId, value là
        // quantity
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute(CART_ATTRIBUTE, cart);// lưu key value. Key là "CART", value là cart (map rỗng)
        }
        return cart;
    }

    /**
     * Cộng dồn {@code delta} vào số lượng hiện có của sản phẩm trong giỏ tạm.
     * {@code delta} âm để giảm; giảm xuống <= 0 thì xoá hẳn sản phẩm khỏi giỏ.
     * Số lượng sau khi cộng không được vượt tồn kho.
     */
    public Map<Long, Integer> addToCart(Long productId, int delta) { // user đăng nhập phát là thêm sp vào giỏ
        Map<Long, Integer> cart = getSessionCart();

        if (delta == 0) {
            return cart;
        }
        if (Math.abs(delta) > MAX_DELTA) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

        int current = cart.getOrDefault(productId, 0);
        int newQuantity = current + delta;

        if (newQuantity <= 0) {
            // Giảm về 0 nghĩa là bỏ sản phẩm khỏi giỏ (frontend giảm số lượng bằng delta
            // âm)
            cart.remove(productId);
            session.setAttribute(CART_ATTRIBUTE, cart);
            return cart;
        }

        if (current == 0 && cart.size() >= MAX_DISTINCT_ITEMS) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }

        if (newQuantity > product.getStockQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        cart.put(productId, newQuantity);
        session.setAttribute(CART_ATTRIBUTE, cart); // ghi lại để an toàn với session store phân tán
        return cart;
    }

    public Map<Long, Integer> removeFromCart(Long productId) {
        Map<Long, Integer> cart = getSessionCart();
        cart.remove(productId);
        session.setAttribute(CART_ATTRIBUTE, cart);
        return cart;
    }

    public void clearCart() {
        session.removeAttribute(CART_ATTRIBUTE);
    }
}
