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

    private Product buildProduct(int stock) {
        Product product = new Product();
        product.setId(1L);
        product.setStockQuantity(stock);
        return product;
    }

    @Test
    void addItemToCart_shouldCreateNewItem_whenNotInCart() {
        Cart cart = new Cart();
        cart.setId(10L);
        cart.setCartItems(new ArrayList<>());
        Product product = buildProduct(10);

        CartItemRequest request = CartItemRequest.builder()
                .cartId(10L).productId(1L).quantity(3).build();

        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemMapper.toCartItemResponse(any(CartItem.class))).thenReturn(new CartItemResponse());

        cartService.addItemToCart(request);

        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItemToCart_shouldAccumulateQuantity_whenAlreadyInCart() {
        Product product = buildProduct(10);
        CartItem existingItem = new CartItem();
        existingItem.setProduct(product);
        existingItem.setQuantity(2);

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setCartItems(new ArrayList<>(List.of(existingItem)));

        CartItemRequest request = CartItemRequest.builder()
                .cartId(10L).productId(1L).quantity(3).build();

        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemMapper.toCartItemResponse(any(CartItem.class))).thenReturn(new CartItemResponse());

        cartService.addItemToCart(request);

        assertThat(existingItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void addItemToCart_shouldThrow_whenNewQuantityExceedsStock() {
        Cart cart = new Cart();
        cart.setCartItems(new ArrayList<>());
        Product product = buildProduct(2);

        CartItemRequest request = CartItemRequest.builder()
                .cartId(10L).productId(1L).quantity(5).build();

        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItemToCart(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItemToCart_shouldThrow_whenAccumulatedQuantityExceedsStock() {
        Product product = buildProduct(3);
        CartItem existingItem = new CartItem();
        existingItem.setProduct(product);
        existingItem.setQuantity(2);

        Cart cart = new Cart();
        cart.setCartItems(new ArrayList<>(List.of(existingItem)));

        CartItemRequest request = CartItemRequest.builder()
                .cartId(10L).productId(1L).quantity(2).build(); // 2 + 2 = 4 > stock 3

        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItemToCart(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addItemToCart_shouldThrow_whenCartNotFound() {
        CartItemRequest request = CartItemRequest.builder()
                .cartId(99L).productId(1L).quantity(1).build();
        when(cartRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_NOT_EXISTED);
    }

    @Test
    void addItemToCart_shouldThrow_whenProductNotFound() {
        Cart cart = new Cart();
        cart.setCartItems(new ArrayList<>());
        CartItemRequest request = CartItemRequest.builder()
                .cartId(10L).productId(99L).quantity(1).build();

        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_EXISTED);
    }

    @Test
    void removeItemFromCart_shouldThrow_whenItemNotFound() {
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItemFromCart(10L, 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_ITEM_NOT_EXISTED);
    }

    @Test
    void removeItemFromCart_shouldThrow_whenItemBelongsToDifferentCart() {
        Cart cart = new Cart();
        cart.setId(20L);
        CartItem item = new CartItem();
        item.setId(1L);
        item.setCart(cart);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.removeItemFromCart(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void removeItemFromCart_shouldDelete_whenItemBelongsToCart() {
        Cart cart = new Cart();
        cart.setId(10L);
        CartItem item = new CartItem();
        item.setId(1L);
        item.setCart(cart);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        cartService.removeItemFromCart(10L, 1L);

        verify(cartItemRepository).delete(item);
    }

    @Test
    void updateItemInCart_shouldUpdateQuantity_happyPath() {
        CartItem item = new CartItem();
        item.setQuantity(1);
        CartItemUpdateRequest request = new CartItemUpdateRequest();
        request.setQuantity(7);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemMapper.toCartItemResponse(item)).thenReturn(new CartItemResponse());

        cartService.updateItemInCart(1L, request);

        assertThat(item.getQuantity()).isEqualTo(7);
        verify(cartItemRepository).save(item);
    }

    @Test
    void updateItemInCart_shouldThrow_whenItemNotFound() {
        CartItemUpdateRequest request = new CartItemUpdateRequest();
        request.setQuantity(1);
        when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemInCart(99L, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_ITEM_NOT_EXISTED);
    }

    @Test
    void getMyCart_shouldThrow_whenCartNotFound() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", null));
        when(cartRepository.findByUser_Username("john")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getMyCart())
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CART_NOT_EXISTED);
    }

    @Test
    void getMyCart_shouldReturnResponse_happyPath() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", null));
        Cart cart = new Cart();
        CartResponse response = new CartResponse();

        when(cartRepository.findByUser_Username("john")).thenReturn(Optional.of(cart));
        when(cartMapper.toCartResponse(cart)).thenReturn(response);

        assertThat(cartService.getMyCart()).isSameAs(response);
    }
}
