package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import vn.payos.PayOS;
import vn.payos.core.ClientOptions;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.webhooks.WebhookData;

/**
 * Chạy SDK payOS thật (không mock) để kiểm được cả tương thích của SDK với phiên bản
 * Jackson do Spring Boot quản lý. API payOS được giả lập bằng một HTTP server cục bộ.
 */
class PayosGatewayTest {

    private static final String CHECKSUM_KEY = "payos-checksum-key-for-tests";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Chuỗi ký của payload webhook mẫu trong tài liệu payOS: các field của {@code data}
     * sắp xếp theo key, dạng {@code key=value} nối bằng {@code &}, giá trị rỗng giữ nguyên "".
     */
    private static final String CANONICAL_WEBHOOK_DATA = "accountNumber=12345678&amount=3000&code=00"
            + "&counterAccountBankId=&counterAccountBankName=&counterAccountName=&counterAccountNumber="
            + "&currency=VND&desc=Thành công&description=VQRIO123&orderCode=123"
            + "&paymentLinkId=124c33293c43417ab7879e14c8d9eb18&reference=TF230204212323"
            + "&transactionDateTime=2023-02-04 18:25:00&virtualAccountName=&virtualAccountNumber=";

    /** Tính độc lập: printf '%s' "$CANONICAL_WEBHOOK_DATA" | openssl dgst -sha256 -hmac "$CHECKSUM_KEY" */
    private static final String CANONICAL_WEBHOOK_SIGNATURE =
            "060019f818c222f94ca99ca0a9bef17b48e5477fcf6200406c9b60c0217ffedf";

    HttpServer server;
    AtomicReference<RecordedRequest> lastRequest = new AtomicReference<>();
    volatile String nextResponseBody;

    PayosGateway gateway;

    record RecordedRequest(String method, String path, String clientId, String apiKey, String body) {
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();

        PayOS payOS = new PayOS(ClientOptions.builder()
                .clientId("client-id")
                .apiKey("api-key")
                .checksumKey(CHECKSUM_KEY)
                .baseURL("http://127.0.0.1:" + server.getAddress().getPort())
                .maxRetries(0)
                .build());
        gateway = new PayosGateway(payOS);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        lastRequest.set(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("x-client-id"),
                exchange.getRequestHeaders().getFirst("x-api-key"),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));

        byte[] response = nextResponseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(response);
        }
    }

    private static String hmacHex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(CHECKSUM_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Cài đặt độc lập với SDK cho dữ liệu phẳng hoặc có mảng object (như "transactions"). */
    private static String signData(Map<String, Object> data) throws IOException {
        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<String, Object> entry : new TreeMap<>(data).entrySet()) {
            Object value = entry.getValue();
            String text;
            if (value == null) {
                text = "";
            } else if (value instanceof List<?> list) {
                text = MAPPER.writeValueAsString(list.stream()
                        .map(item -> new TreeMap<>((Map<?, ?>) item))
                        .toList());
            } else {
                text = value.toString();
            }
            if (!canonical.isEmpty()) {
                canonical.append('&');
            }
            canonical.append(entry.getKey()).append('=').append(text);
        }
        return hmacHex(canonical.toString());
    }

    private static Map<String, Object> sampleWebhookData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderCode", 123);
        data.put("amount", 3000);
        data.put("description", "VQRIO123");
        data.put("accountNumber", "12345678");
        data.put("reference", "TF230204212323");
        data.put("transactionDateTime", "2023-02-04 18:25:00");
        data.put("currency", "VND");
        data.put("paymentLinkId", "124c33293c43417ab7879e14c8d9eb18");
        data.put("code", "00");
        data.put("desc", "Thành công");
        data.put("counterAccountBankId", "");
        data.put("counterAccountBankName", "");
        data.put("counterAccountName", "");
        data.put("counterAccountNumber", "");
        data.put("virtualAccountName", "");
        data.put("virtualAccountNumber", "");
        return data;
    }

    private static byte[] webhookBody(Map<String, Object> data, String signature) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "00");
        body.put("desc", "success");
        body.put("success", true);
        body.put("data", data);
        if (signature != null) {
            body.put("signature", signature);
        }
        return MAPPER.writeValueAsBytes(body);
    }

    private static String apiResponse(Map<String, Object> data) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "00");
        body.put("desc", "success");
        body.put("data", data);
        body.put("signature", signData(data));
        return MAPPER.writeValueAsString(body);
    }

    // ==================== verifyWebhook ====================

    @Test
    void independentSigner_shouldMatchOpensslVector() throws IOException {
        // Neo cài đặt ký trong test vào giá trị tính bằng openssl, để test không "tự đúng với chính nó"
        assertThat(hmacHex(CANONICAL_WEBHOOK_DATA)).isEqualTo(CANONICAL_WEBHOOK_SIGNATURE);
        assertThat(signData(sampleWebhookData())).isEqualTo(CANONICAL_WEBHOOK_SIGNATURE);
    }

    @Test
    void verifyWebhook_shouldReturnData_whenSignedWithChecksumKey() throws IOException {
        WebhookData data = gateway.verifyWebhook(webhookBody(sampleWebhookData(), CANONICAL_WEBHOOK_SIGNATURE));

        assertThat(data.getOrderCode()).isEqualTo(123L);
        assertThat(data.getAmount()).isEqualTo(3000L);
        assertThat(data.getReference()).isEqualTo("TF230204212323");
        assertThat(data.getCode()).isEqualTo("00");
        assertThat(data.getDesc()).isEqualTo("Thành công");
    }

    @Test
    void verifyWebhook_shouldReject_whenDataTamperedAfterSigning() throws IOException {
        Map<String, Object> data = sampleWebhookData();
        data.put("amount", 3_000_000);

        assertInvalidSignature(webhookBody(data, CANONICAL_WEBHOOK_SIGNATURE));
    }

    @Test
    void verifyWebhook_shouldReject_whenSignedWithAnotherKey() throws IOException {
        String wrong = HexFormat.of().formatHex(new byte[32]);

        assertInvalidSignature(webhookBody(sampleWebhookData(), wrong));
    }

    @Test
    void verifyWebhook_shouldReject_whenSignatureMissing() throws IOException {
        assertInvalidSignature(webhookBody(sampleWebhookData(), null));
    }

    @Test
    void verifyWebhook_shouldReject_whenBodyIsNotJson() {
        assertInvalidSignature("not-json".getBytes(StandardCharsets.UTF_8));
    }

    private void assertInvalidSignature(byte[] body) {
        assertThatThrownBy(() -> gateway.verifyWebhook(body))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.WEBHOOK_INVALID_SIGNATURE);
    }

    // ==================== payment requests ====================

    @Test
    void createLink_shouldSendSignedRequest_andParseResponse() throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bin", "970422");
        data.put("accountNumber", "113366668888");
        data.put("accountName", "NGUYEN VAN A");
        data.put("amount", 500000);
        data.put("description", "DH42");
        data.put("orderCode", 42);
        data.put("currency", "VND");
        data.put("paymentLinkId", "link-42");
        data.put("status", "PENDING");
        data.put("expiredAt", 1_900_000_000);
        data.put("checkoutUrl", "https://pay.payos.vn/web/link-42");
        data.put("qrCode", "000201010212");
        nextResponseBody = apiResponse(data);

        CreatePaymentLinkResponse response = gateway.createLink(CreatePaymentLinkRequest.builder()
                .orderCode(42L)
                .amount(500_000L)
                .description("DH42")
                .returnUrl("https://shop.test/")
                .cancelUrl("https://shop.test/")
                .expiredAt(1_900_000_000L)
                .build());

        assertThat(response.getCheckoutUrl()).isEqualTo("https://pay.payos.vn/web/link-42");
        assertThat(response.getPaymentLinkId()).isEqualTo("link-42");
        assertThat(response.getStatus()).isEqualTo(PaymentLinkStatus.PENDING);

        RecordedRequest request = lastRequest.get();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/v2/payment-requests");
        assertThat(request.clientId()).isEqualTo("client-id");
        assertThat(request.apiKey()).isEqualTo("api-key");

        // Chữ ký tạo link theo tài liệu payOS: amount, cancelUrl, description, orderCode, returnUrl
        Map<?, ?> sent = MAPPER.readValue(request.body(), Map.class);
        assertThat(sent.get("signature")).isEqualTo(hmacHex(
                "amount=500000&cancelUrl=https://shop.test/&description=DH42&orderCode=42&returnUrl=https://shop.test/"));
        assertThat(sent.get("expiredAt")).isEqualTo(1_900_000_000);
    }

    @Test
    void getLink_shouldParseStatusAndTransactions() throws IOException {
        Map<String, Object> tx = new LinkedHashMap<>();
        tx.put("reference", "FT123");
        tx.put("amount", 500000);
        tx.put("accountNumber", "113366668888");
        tx.put("description", "DH42");
        tx.put("transactionDateTime", "2026-09-13 10:00:00");
        tx.put("virtualAccountName", null);
        tx.put("virtualAccountNumber", null);
        tx.put("counterAccountBankId", null);
        tx.put("counterAccountBankName", null);
        tx.put("counterAccountName", null);
        tx.put("counterAccountNumber", null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "link-42");
        data.put("orderCode", 42);
        data.put("amount", 500000);
        data.put("amountPaid", 500000);
        data.put("amountRemaining", 0);
        data.put("status", "PAID");
        data.put("createdAt", "2026-09-13T09:55:00+07:00");
        data.put("transactions", List.of(tx));
        data.put("cancellationReason", null);
        data.put("canceledAt", null);
        nextResponseBody = apiResponse(data);

        PaymentLink link = gateway.getLink("link-42");

        assertThat(lastRequest.get().method()).isEqualTo("GET");
        assertThat(lastRequest.get().path()).isEqualTo("/v2/payment-requests/link-42");
        assertThat(link.getStatus()).isEqualTo(PaymentLinkStatus.PAID);
        assertThat(link.getTransactions()).singleElement()
                .satisfies(t -> {
                    assertThat(t.getReference()).isEqualTo("FT123");
                    assertThat(t.getAmount()).isEqualTo(500_000L);
                });
    }

    @Test
    void createLink_shouldThrowGatewayError_whenPayosRejects() throws IOException {
        nextResponseBody = MAPPER.writeValueAsString(Map.of("code", "231", "desc", "Đơn thanh toán đã tồn tại"));

        assertThatThrownBy(() -> gateway.createLink(CreatePaymentLinkRequest.builder()
                .orderCode(42L)
                .amount(500_000L)
                .description("DH42")
                .returnUrl("https://shop.test/")
                .cancelUrl("https://shop.test/")
                .build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_GATEWAY_ERROR);
    }
}
