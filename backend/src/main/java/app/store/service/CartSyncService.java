package app.store.service;

import app.store.entity.Cart;
import app.store.entity.Product;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.CartRepository;
import app.store.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartSyncService {
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    /**
     * Gộp giỏ tạm trong session vào giỏ DB khi đăng nhập.
     * <p>
     * Việc gộp không bao giờ được làm hỏng request đăng nhập: item nào không gộp
     * được (sản phẩm đã xoá, hết hàng) thì bỏ qua, và giỏ tạm luôn được xoá ở cuối
     * để lỗi không lặp lại ở mỗi lần đăng nhập sau.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void syncSessionCart(User user, HttpSession session) {

        Map<Long, Integer> sessionCart =
                (Map<Long, Integer>) session.getAttribute(SessionCartService.CART_ATTRIBUTE);

        if (sessionCart == null || sessionCart.isEmpty()) return;

     
        Cart dbCart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_EXISTED));

        // Lọc trước các sản phẩm đã bị xoá để không truy vấn từng id trong vòng lặp
        Set<Long> existingProductIds = StreamSupport
                .stream(productRepository.findAllById(sessionCart.keySet()).spliterator(), false)
                .map(Product::getId)
                .collect(Collectors.toSet());

        for (var entry : sessionCart.entrySet()) {
            Long productId = entry.getKey();
            if (!existingProductIds.contains(productId)) {
                log.warn("Bỏ qua sản phẩm {} khi gộp giỏ: sản phẩm không còn tồn tại", productId);
                continue;
            }
            try {
                cartService.mergeItem(dbCart, productId, entry.getValue());
            } catch (Exception e) {
                // Một item hỏng không được làm hỏng cả lần đăng nhập
                log.warn("Bỏ qua sản phẩm {} khi gộp giỏ: {}", productId, e.getMessage());
            }
        }

        // Xóa session cart để tránh sync lại
        session.removeAttribute(SessionCartService.CART_ATTRIBUTE);
    }
}
