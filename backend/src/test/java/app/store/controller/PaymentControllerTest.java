package app.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import app.store.enums.WebhookOutcome;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.service.PaymentService;
import app.store.service.PayosGateway;
import vn.payos.model.webhooks.WebhookData;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private static final byte[] BODY = "{\"data\":{},\"signature\":\"abc\"}".getBytes(StandardCharsets.UTF_8);

    @Mock
    PaymentService paymentService;
    @Mock
    PayosGateway payosGateway;

    PaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(paymentService, payosGateway);
    }

    @Test
    void shouldVerifyThenPassVerifiedDataToService() {
        WebhookData data = new WebhookData();
        when(payosGateway.verifyWebhook(BODY)).thenReturn(data);
        when(paymentService.handlePayosWebhook(data)).thenReturn(WebhookOutcome.PAID);

        ResponseEntity<Map<String, Object>> response = controller.handlePayosWebhook(BODY);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("outcome", "PAID");
    }

    @Test
    void shouldNotTouchService_whenSignatureInvalid() {
        when(payosGateway.verifyWebhook(BODY)).thenThrow(new AppException(ErrorCode.WEBHOOK_INVALID_SIGNATURE));

        assertThatThrownBy(() -> controller.handlePayosWebhook(BODY))
                .isInstanceOf(AppException.class);

        verify(paymentService, never()).handlePayosWebhook(any());
    }
}
