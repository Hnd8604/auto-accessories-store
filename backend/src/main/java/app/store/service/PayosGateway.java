package app.store.service;

import java.nio.charset.StandardCharsets;

import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import vn.payos.PayOS;
import vn.payos.exception.APIException;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.WebhookData;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayosGateway {

    private final PayOS payOS;

    public CreatePaymentLinkResponse createLink(CreatePaymentLinkRequest request) {
        try {
            return payOS.paymentRequests().create(request);
        } catch (PayOSException e) {
            throw gatewayError("create payment link for payOS orderCode " + request.getOrderCode(), e);
        }
    }

    public PaymentLink getLink(String paymentLinkId) {
        try {
            return payOS.paymentRequests().get(paymentLinkId);
        } catch (PayOSException e) {
            throw gatewayError("get payment link " + paymentLinkId, e);
        }
    }

    public void cancelLink(String paymentLinkId, String reason) {
        try {
            payOS.paymentRequests().cancel(paymentLinkId, reason);
        } catch (PayOSException e) {
            throw gatewayError("cancel payment link " + paymentLinkId, e);
        }
    }

    // Xác thực chữ ký webhook bằng Checksum Key rồi trả về phần data.
    public WebhookData verifyWebhook(byte[] body) {
        try {
            return payOS.webhooks().verify(new String(body, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            log.warn("Rejected payOS webhook: {}", e.getClass().getSimpleName());
            throw new AppException(ErrorCode.WEBHOOK_INVALID_SIGNATURE);
        }
    }

    // Đổi lỗi của SDK sang AppException để không phụ thuộc kiểu exception của
    // SDK.
    private AppException gatewayError(String action, PayOSException e) {
        if (e instanceof APIException apiException) {
            log.error("payOS failed to {}: status={}, code={}, desc={}", action,
                    apiException.getStatusCode().orElse(null),
                    apiException.getErrorCode().orElse(null),
                    apiException.getErrorDesc().orElse(null));
        } else {
            log.error("payOS failed to {}", action, e);
        }
        return new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
    }
}
