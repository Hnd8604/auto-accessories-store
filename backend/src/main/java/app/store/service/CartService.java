package app.store.service;

import app.store.dto.request.CartItemRequest;
import app.store.dto.request.CartItemUpdateRequest;
import app.store.dto.response.CartItemResponse;
import app.store.dto.response.CartResponse;
import app.store.entity.Cart;
import app.store.entity.CartItem;
import app.store.entity.Product;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.CartItemMapper;
import app.store.mapper.CartMapper;
import app.store.repository.CartItemRepository;
import app.store.repository.CartRepository;
import app.store.repository.ProductRepository;
import app.store.utils.SecurityUtils;
import com.nimbusds.jose.JOSEException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CartService {
    CartMapper cartMapper;
    CartRepository cartRepository;
    ProductRepository productRepository;
    CartItemRepository cartItemRepository;
    CartItemMapper cartItemMapper;

    public CartResponse getMyCart() {
        Cart cart = cartRepository.findByUserId(SecurityUtils.currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_EXISTED));
        return cartMapper.toCartResponse(cart);
    }

    @PreAuthorize("hasAuthority('CART_GET_BY_ID')")
    public CartResponse getCartById(Long cartId) throws ParseException, JOSEException {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_EXISTED));
        return cartMapper.toCartResponse(cart);
    }

    // Luôn thêm vào giỏ của user trong JWT, không nhận cartId từ client
    public CartItemResponse addItemToCart(CartItemRequest request) {
        Cart cart = cartRepository.findByUserId(SecurityUtils.currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_EXISTED));
        return addItem(cart, request.productId(), request.quantity());
    }

    // Dùng khi đã xác định được cart phía server (vd. gộp giỏ session lúc đăng
    // nhập,
    // khi SecurityContext chưa có user)
    public CartItemResponse addItem(Cart cart, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

        // Check if the product is already in the cart
        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int stock = product.getStockQuantity();

        int current = cartItem == null ? 0 : cartItem.getQuantity();
        int newQuantity = current + quantity;
        if (newQuantity <= 0) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }
        if (newQuantity > stock) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
        }
        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);
        return cartItemMapper.toCartItemResponse(cartItem);
    }

    /**
     * Gộp một sản phẩm vào giỏ DB khi đăng nhập: cắt bớt theo tồn kho thay vì ném
     * lỗi,
     * để một item hỏng trong giỏ session không làm hỏng cả request đăng nhập.
     *
     * @return true nếu gộp được ít nhất 1 đơn vị, false nếu bỏ qua item này
     */
    public boolean mergeItem(Cart cart, Long productId, int quantity) {
        if (quantity <= 0) {
            return false;
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return false;
        }

        int stock = product.getStockQuantity();
        if (stock <= 0) {
            return false;
        }

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int current = cartItem == null ? 0 : cartItem.getQuantity();
        int newQuantity = Math.min(current + quantity, stock);
        if (newQuantity <= current) {
            return false; // giỏ DB đã chạm tồn kho, không gộp thêm được gì
        }

        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
        }
        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);
        return true;
    }

    @PreAuthorize("hasAuthority('CART_REMOVE_ITEM')")
    public void removeItemFromCart(Long cartId, Long itemId) {
        CartItem item = findMyCartItem(itemId);

        if (!item.getCart().getId().equals(cartId)) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_IN_CART);
        }
        cartItemRepository.delete(item);
    }

    @PreAuthorize("hasAuthority('CART_UPDATE_ITEM')")
    public CartItemResponse updateItemInCart(Long itemId, CartItemUpdateRequest request) {
        CartItem cartItem = findMyCartItem(itemId);
        if (request.quantity() > cartItem.getProduct().getStockQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
        cartItem.setQuantity(request.quantity());
        cartItemRepository.save(cartItem);
        return cartItemMapper.toCartItemResponse(cartItem);
    }

    // Item của giỏ người khác trả về như không tồn tại để không lộ ID hợp lệ
    private CartItem findMyCartItem(Long itemId) {
        String userId = SecurityUtils.currentUserId();
        return cartItemRepository.findById(itemId)
                .filter(item -> userId.equals(item.getCart().getUser().getId()))
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_EXISTED));
    }
}
