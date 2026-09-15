package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import app.store.dto.request.CartItemRequest;
import app.store.dto.request.CartItemUpdateRequest;
import app.store.dto.response.CartItemResponse;
import app.store.dto.response.CartResponse;
import app.store.entity.Cart;
import app.store.entity.CartItem;
import app.store.entity.Product;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.CartItemMapper;
import app.store.mapper.CartMapper;
import app.store.repository.CartItemRepository;
import app.store.repository.CartRepository;
import app.store.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {
    @Mock
    CartMapper cartMapper;
    @Mock
    CartRepository cartRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    CartItemRepository cartItemRepository;
    @Mock
    CartItemMapper cartItemMapper;
    @InjectMocks
    CartService cartService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null));
    }

    private Product buildProduct(int stock) {
        Product product = new Product();
        product.setId(1L);
        product.setStockQuantity(stock);
        return product;
    }

    private Cart buildCartOf(String username, Long cartId) {
        User owner = new User();
        owner.setUsername(username);
        Cart cart = new Cart();
        cart.setId(cartId);
        cart.setUser(owner);
        cart.setCartItems(new ArrayList<>());
        return cart;
    }

    private CartItem buildItem(Cart cart, Product product, int quantity) {
        CartItem item = new CartItem();
        item.setId(1L);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void addItemToCart_shouldUseCartOfCurrentUser() {
        loginAs("john");
        Cart cart = buildCartOf("john", 10L);
        Product product = buildProduct(10);
        CartItemRequest request = CartItemRequest.builder().productId(1L).quantity(3).build();

        when(cartRepository.findByUser_Username("john")).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemMapper.toCartItemResponse(any(CartItem.class))).thenReturn(CartItemResponse.builder().build());

        cartService.addItemToCart(request);

        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRepository, never()).findById(any());
    }

    @Test
    void addItemToCart_shouldThrow_whenCurrentUserHasNoCart() {
        loginAs("john");
        CartItemRequest request = CartItemRequest.builder().productId(1L).quantity(1).build();
        when(cartRepository.findByUser_Username("john")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_NOT_EXISTED);
    }

    @Test
    void addItem_shouldCreateNewItem_whenNotInCart() {
        Cart cart = buildCartOf("john", 10L);
        Product product = buildProduct(10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemMapper.toCartItemResponse(any(CartItem.class))).thenReturn(CartItemResponse.builder().build());

        cartService.addItem(cart, 1L, 3);

        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_shouldAccumulateQuantity_whenAlreadyInCart() {
        Product product = buildProduct(10);
        Cart cart = buildCartOf("john", 10L);
        CartItem existingItem = buildItem(cart, product, 2);
        cart.setCartItems(new ArrayList<>(List.of(existingItem)));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemMapper.toCartItemResponse(any(CartItem.class))).thenReturn(CartItemResponse.builder().build());

        cartService.addItem(cart, 1L, 3);

        assertThat(existingItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void addItem_shouldThrow_whenNewQuantityExceedsStock() {
        Cart cart = buildCartOf("john", 10L);
        Product product = buildProduct(2);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(cart, 1L, 5))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_shouldThrow_whenAccumulatedQuantityExceedsStock() {
        Product product = buildProduct(3);
        Cart cart = buildCartOf("john", 10L);
        cart.setCartItems(new ArrayList<>(List.of(buildItem(cart, product, 2))));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(cart, 1L, 2)) // 2 + 2 = 4 > stock 3
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addItem_shouldThrow_whenProductNotFound() {
        Cart cart = buildCartOf("john", 10L);
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(cart, 99L, 1))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_EXISTED);
    }

    @Test
    void removeItemFromCart_shouldThrow_whenItemNotFound() {
        loginAs("john");
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItemFromCart(10L, 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_ITEM_NOT_EXISTED);
    }

    @Test
    void removeItemFromCart_shouldThrow_whenItemBelongsToAnotherUser() {
        loginAs("john");
        CartItem item = buildItem(buildCartOf("mary", 20L), buildProduct(5), 1);
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.removeItemFromCart(20L, 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_ITEM_NOT_EXISTED);

        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void removeItemFromCart_shouldThrow_whenItemBelongsToDifferentCart() {
        loginAs("john");
        CartItem item = buildItem(buildCartOf("john", 20L), buildProduct(5), 1);
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.removeItemFromCart(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void removeItemFromCart_shouldDelete_whenItemBelongsToCart() {
        loginAs("john");
        CartItem item = buildItem(buildCartOf("john", 10L), buildProduct(5), 1);
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        cartService.removeItemFromCart(10L, 1L);

        verify(cartItemRepository).delete(item);
    }

    @Test
    void updateItemInCart_shouldUpdateQuantity_happyPath() {
        loginAs("john");
        CartItem item = buildItem(buildCartOf("john", 10L), buildProduct(10), 1);
        CartItemUpdateRequest request = CartItemUpdateRequest.builder().quantity(7).build();

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemMapper.toCartItemResponse(item)).thenReturn(CartItemResponse.builder().build());

        cartService.updateItemInCart(1L, request);

        assertThat(item.getQuantity()).isEqualTo(7);
        verify(cartItemRepository).save(item);
    }

    @Test
    void updateItemInCart_shouldThrow_whenQuantityExceedsStock() {
        loginAs("john");
        CartItem item = buildItem(buildCartOf("john", 10L), buildProduct(3), 1);
        CartItemUpdateRequest request = CartItemUpdateRequest.builder().quantity(4).build();

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateItemInCart(1L, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateItemInCart_shouldThrow_whenItemBelongsToAnotherUser() {
        loginAs("john");
        CartItem item = buildItem(buildCartOf("mary", 20L), buildProduct(10), 1);
        CartItemUpdateRequest request = CartItemUpdateRequest.builder().quantity(2).build();

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateItemInCart(1L, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_ITEM_NOT_EXISTED);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateItemInCart_shouldThrow_whenItemNotFound() {
        loginAs("john");
        CartItemUpdateRequest request = CartItemUpdateRequest.builder().quantity(1).build();
        when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemInCart(99L, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_ITEM_NOT_EXISTED);
    }

    @Test
    void getMyCart_shouldThrow_whenCartNotFound() {
        loginAs("john");
        when(cartRepository.findByUser_Username("john")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getMyCart())
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_NOT_EXISTED);
    }

    @Test
    void getMyCart_shouldReturnResponse_happyPath() {
        loginAs("john");
        Cart cart = new Cart();
        CartResponse response = CartResponse.builder().build();

        when(cartRepository.findByUser_Username("john")).thenReturn(Optional.of(cart));
        when(cartMapper.toCartResponse(cart)).thenReturn(response);

        assertThat(cartService.getMyCart()).isSameAs(response);
    }
}
