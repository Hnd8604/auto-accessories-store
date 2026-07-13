package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.request.OrderCreationRequest;
import app.store.dto.request.OrderUpdateByAdminRequest;
import app.store.dto.request.OrderUpdateByUserRequest;
import app.store.dto.response.OrderResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static app.store.utils.SortUtils.buildSort;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Order Management", description = "APIs for managing orders including creation, updates, and order tracking")
public class OrderController {
    OrderService OrderService;

    @GetMapping
    @Operation(
        summary = "Get all orders",
        description = "Retrieves all orders with pagination and sorting. Only accessible by admin users."
    )
    ApiResponse<Page<OrderResponse>> getAllOrders( @RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "size", defaultValue = "10") int size,
                                                   @RequestParam(value = "sort", defaultValue = "createdAt,DESC") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        return ApiResponse.<Page<OrderResponse>>builder()
                .result(OrderService.getAllOrders(pageable))
                .message(ResponseMessage.GET_ALL_ORDERS_SUCCESS)
                .build();
    }

    @GetMapping("/{orderId}")
    @Operation(
        summary = "Get order by ID",
        description = "Retrieves detailed information of an order by ID. Only accessible by admin users."
    )
    ApiResponse<OrderResponse> getOrder(@PathVariable String orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(OrderService.getOrderById(orderId))
                .message(ResponseMessage.GET_ORDER_SUCCESS)
                .build();
    }

    @GetMapping("/my-order")
    @Operation(
        summary = "Get my orders",
        description = "Retrieves all orders of the authenticated user. Accessible by any authenticated user."
    )
    ApiResponse<List<OrderResponse>> getMyOrder() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(OrderService.getMyOrder())
                .message(ResponseMessage.GET_MY_ORDER_SUCCESS)
                .build();
    }

    @PostMapping
    @Operation(
        summary = "Create order from cart",
        description = "Creates a new order from the user's shopping cart. Accessible by authenticated users."
    )
    ApiResponse<OrderResponse> createOrderFromCart(@RequestBody OrderCreationRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .result(OrderService.createOrderFromCart(request))
                .message(ResponseMessage.CREATE_ORDER_SUCCESS)
                .build();
    }

    @PutMapping("/{orderId}/update-by-user")
    @Operation(
        summary = "Update order by user",
        description = "Updates order information by the user. Users can update delivery address and contact information."
    )
    ApiResponse<OrderResponse> updateOrderByUser(@PathVariable String orderId, @RequestBody OrderUpdateByUserRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .result(OrderService.updateOrderByUser(orderId, request))
                .message(ResponseMessage.UPDATE_ORDER_BY_USER_SUCCESS)
                .build();
    }

    @PutMapping("/{orderId}/update-by-admin")
    @Operation(
        summary = "Update order by admin",
        description = "Updates order information by admin. Admin can update order status, payment status, and shipping information."
    )
    ApiResponse<OrderResponse> updateOrderByAdmin(@PathVariable String orderId, @RequestBody OrderUpdateByAdminRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .result(OrderService.updateOrderByAdmin(orderId, request))
                .message(ResponseMessage.UPDATE_ORDER_BY_ADMIN_SUCCESS)
                .build();
    }

    @PutMapping("{orderId}/cancel")
    @Operation(
        summary = "Cancel order",
        description = "Cancels an order. Accessible by the order owner or admin users."
    )
    ApiResponse<OrderResponse> cancelOrder(@PathVariable String orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(OrderService.cancelOrder(orderId))
                .message(ResponseMessage.CANCEL_ORDER_SUCCESS)
                .build();
    }

    @DeleteMapping("/{orderId}")
    @Operation(
        summary = "Delete order",
        description = "Permanently deletes an order by ID. Only accessible by admin users."
    )
    ApiResponse<Void> deleteOrder(@PathVariable String orderId) {
        OrderService.deleteOrder(orderId);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.DELETE_ORDER_SUCCESS)
                .build();
    }

}
