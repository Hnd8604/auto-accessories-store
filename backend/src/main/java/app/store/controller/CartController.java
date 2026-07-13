package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.request.CartItemRequest;
import app.store.dto.request.CartItemUpdateRequest;
import app.store.dto.response.CartResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.dto.response.CartItemResponse;
import app.store.service.CartService;

import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Cart Management", description = "APIs for managing shopping carts and cart items")
public class CartController {
    CartService CartService;

    @GetMapping("/{cartId}")
    @Operation(
        summary = "Get cart by ID",
        description = "Retrieves cart details including all cart items. Accessible by cart owner or admin users."
    )
    ApiResponse<CartResponse> getCartById(@PathVariable Long cartId) throws ParseException, JOSEException {
        return ApiResponse.<CartResponse>builder()
                .result(CartService.getCartById(cartId))
                .message(ResponseMessage.GET_CART_SUCCESS)
                .build();
    }
    @GetMapping("/my-cart")
    @Operation(
            summary = "Get my cart",
            description = "Retrieves cart details including all cart items. Accessible by cart owner or admin users."
    )
    ApiResponse<CartResponse> getMyCart() throws ParseException, JOSEException {
        return ApiResponse.<CartResponse>builder()
                .result(CartService.getMyCart())
                .message(ResponseMessage.GET_CART_SUCCESS)
                .build();
    }
    @PostMapping("/items")
    @Operation(
        summary = "Add item to cart",
        description = "Adds a product item to the shopping cart. Accessible by authenticated users."
    )
    ApiResponse<CartItemResponse> addItemToCart(@RequestBody CartItemRequest cartItemRequest) {
        return ApiResponse.<CartItemResponse>builder()
                .result(CartService.addItemToCart(cartItemRequest))
                .message(ResponseMessage.ADD_ITEM_SUCCESS)
                .build();
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    @Operation(
        summary = "Remove item from cart",
        description = "Removes a specific item from the shopping cart. Accessible by cart owner."
    )
    ApiResponse<Void> removeItemFromCart(@PathVariable  Long cartId, @PathVariable Long itemId) {
        CartService.removeItemFromCart(cartId, itemId);
        return ApiResponse.<Void>builder().
                message(ResponseMessage.REMOVE_ITEM_SUCCESS).build();

    }
    @PutMapping("/items/{itemId}")
    @Operation(
        summary = "Update cart item",
        description = "Updates quantity of a cart item. Accessible by cart owner."
    )
    ApiResponse<CartItemResponse> updateItemInCart(@PathVariable Long itemId, @RequestBody CartItemUpdateRequest request) {
        return ApiResponse.<CartItemResponse>builder()
                .result(CartService.updateItemInCart(itemId, request))
                .message(ResponseMessage.UPDATE_ITEM_SUCCESS)
                .build();
    }
}
