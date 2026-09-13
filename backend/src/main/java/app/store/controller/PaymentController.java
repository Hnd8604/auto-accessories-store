package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.response.PaymentResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.enums.WebhookOutcome;
import app.store.service.PaymentService;
import app.store.service.PayosGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.WebhookData;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment Management", description = "APIs for payment processing with payOS integration")
public class PaymentController {

    PaymentService paymentService;
    PayosGateway payosGateway;

    @PostMapping("/{orderId}/create")
    @Operation(summary = "Create payment link", description = "Returns a payOS checkout URL for the specified order. Reuses the latest link while it is still payable, otherwise creates a new one.")
    ApiResponse<PaymentResponse> createPayment(@PathVariable String orderId) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.createPayment(orderId))
                .message(ResponseMessage.CREATE_PAYMENT_SUCCESS)
                .build();
    }

    @GetMapping("/{orderId}/status")
    @Operation(summary = "Check payment status", description = "Checks the current payment status of an order, reconciling with payOS while it is unpaid. Frontend can poll this API to detect when payment is completed.")
    ApiResponse<PaymentResponse> checkPaymentStatus(@PathVariable String orderId) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.checkPaymentStatus(orderId))
                .message(ResponseMessage.CHECK_PAYMENT_STATUS_SUCCESS)
                .build();
    }

    @PostMapping("/payos/webhook")
    @Operation(summary = "payOS Webhook", description = "Receives payment notifications from payOS. Authenticated by the HMAC-SHA256 signature in the body, computed with the Checksum Key.")
    ResponseEntity<Map<String, Object>> handlePayosWebhook(@RequestBody byte[] body) {
        WebhookData data = payosGateway.verifyWebhook(body); // xác thực chữ ký tại đây

        WebhookOutcome outcome = paymentService.handlePayosWebhook(data);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "outcome", outcome.name(),
                "message", outcome.getMessage()));
    }
}
