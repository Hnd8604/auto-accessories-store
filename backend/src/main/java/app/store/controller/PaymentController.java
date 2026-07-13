package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.request.SepayWebhookRequest;
import app.store.dto.response.PaymentResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment Management", description = "APIs for payment processing with SePay integration")
public class PaymentController {

    PaymentService paymentService;

    @PostMapping("/{orderId}/create")
    @Operation(
        summary = "Create payment QR",
        description = "Creates a VietQR payment QR code for the specified order. Returns bank details and QR URL for the user to scan and transfer."
    )
    ApiResponse<PaymentResponse> createPayment(@PathVariable String orderId) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.createPayment(orderId))
                .message(ResponseMessage.CREATE_PAYMENT_SUCCESS)
                .build();
    }

    @GetMapping("/{orderId}/status")
    @Operation(
        summary = "Check payment status",
        description = "Checks the current payment status of an order. Frontend can poll this API to detect when payment is completed."
    )
    ApiResponse<PaymentResponse> checkPaymentStatus(@PathVariable String orderId) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.checkPaymentStatus(orderId))
                .message(ResponseMessage.CHECK_PAYMENT_STATUS_SUCCESS)
                .build();
    }

    /**
     * Webhook endpoint cho SePay gọi khi có giao dịch ngân hàng.
     * Endpoint này KHÔNG yêu cầu authentication (public) nhưng verify API key.
     *
     * SePay gửi API key trong header "Authorization" để xác thực.
     * ⚠️ Nếu không verify → ai cũng có thể fake webhook → đơn hàng được đánh dấu PAID
     * mà không cần chuyển tiền thật!
     */
    @PostMapping("/sepay/webhook")
    @Operation(
        summary = "SePay Webhook (IPN)",
        description = "Receives transaction notifications from SePay. Verifies API key before processing."
    )
    ResponseEntity<Map<String, Object>> handleSepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SepayWebhookRequest request) {

        log.info("Received SePay webhook: transferType={}, amount={}, content={}",
                request.getTransferType(), request.getTransferAmount(), request.getContent());

        try {
            // 🔐 Verify API key
            paymentService.verifyApiKey(authorization);

            // Xử lý webhook
            boolean success = paymentService.handleSepayWebhook(request);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", "Webhook processed successfully"
            ));
        } catch (app.store.exception.AppException e) {
            // API key verification failed → trả 401
            log.error("🚫 Webhook rejected: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized: " + e.getErrorCode().getMessage()
            ));
        } catch (Exception e) {
            log.error("Error processing SePay webhook: ", e);
            // Trả về 200 OK để SePay không retry (tránh spam)
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Error processing webhook: " + e.getMessage()
            ));
        }
    }
}
