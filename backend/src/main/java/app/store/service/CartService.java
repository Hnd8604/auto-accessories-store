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
import com.nimbusds.jose.JOSEException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Cart cart = cartRepository.findByUser_Username(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_EXISTED));
        return cartMapper.toCartResponse(cart);
    }
    @PreAuthorize("hasAuthority('CART_GET_BY_ID')")
    public CartResponse getCartById(Long cartId) throws ParseException, JOSEException {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_EXISTED));
        return cartMapper.toCartResponse(cart);
    }
//@PreAuthorize("hasAuthority('CART_ADD_ITEM')")
public CartItemResponse addItemToCart(CartItemRequest request) {
    Cart cart = cartRepository.findById(request.getCartId())
            .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_EXISTED));
    Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

    // Check if the product is already in the cart
    CartItem cartItem = cart.getCartItems().stream()
            .filter(item -> item.getProduct().getId().equals(product.getId()))
            .findFirst()
            .orElse(null);

    if (cartItem != null) {
        // If item exists, update the quantity
        int newQuantity = cartItem.getQuantity() + request.getQuantity();
        if (newQuantity <= 0 || newQuantity > product.getStockQuantity()) {
            throw new IllegalArgumentException("Quantity is not valid");
        }
        cartItem.setQuantity(newQuantity);
    } else {
        // If item does not exist, create a new one
        if (request.getQuantity() <= 0 || request.getQuantity() > product.getStockQuantity()) {
            throw new IllegalArgumentException("Quantity is not valid");
        }
        cartItem = new CartItem(); // neu bang null thi tao moi
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(request.getQuantity());
    }
    cartItemRepository.save(cartItem);
    return cartItemMapper.toCartItemResponse(cartItem);
}
    @PreAuthorize("hasAuthority('CART_REMOVE_ITEM')")
    public void removeItemFromCart(Long cartId, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_EXISTED));

        if (!item.getCart().getId().equals(cartId)) {
            throw new IllegalArgumentException("Item does not belong to cart " + cartId);
        }
        cartItemRepository.delete(item);
    }
    @PreAuthorize("hasAuthority('CART_UPDATE_ITEM')")
    public CartItemResponse updateItemInCart(Long itemId, CartItemUpdateRequest request) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_EXISTED));
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        return cartItemMapper.toCartItemResponse(cartItem);
    }
}
