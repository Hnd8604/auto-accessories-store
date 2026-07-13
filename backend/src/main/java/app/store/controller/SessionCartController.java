package app.store.controller;

import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.ProductRepository;
import app.store.service.SessionCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("session-carts")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Session Cart Management", description = "APIs for managing session-based shopping carts (for guest users)")
public class SessionCartController {

    SessionCartService sessionCartService;
    ProductRepository productRepository;
    
    @PostMapping("/add")
    @Operation(
        summary = "Add product to session cart",
        description = "Adds a product to the session-based cart. Used for guest users who haven't logged in."
    )
    public Map<Long, Integer> add(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int qty
    ) {
        // Check product exists
        productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

        return sessionCartService.addToCart(productId, qty);
    }

    @GetMapping
    @Operation(
        summary = "View session cart",
        description = "Retrieves the current session cart contents. Returns a map of product IDs to quantities."
    )
    public Map<Long, Integer> view() {
        return sessionCartService.getSessionCart();
    }

    @DeleteMapping("/remove/{productId}")
    @Operation(
        summary = "Remove product from session cart",
        description = "Removes a product from the session-based cart."
    )
    public Map<Long, Integer> remove(@PathVariable Long productId) {
        return sessionCartService.removeFromCart(productId);
    }

    @DeleteMapping("/clear")
    @Operation(
        summary = "Clear session cart",
        description = "Removes all products from the session cart. Typically used after merging to user cart."
    )
    public void clear() {
        sessionCartService.clearCart();
    }
}
