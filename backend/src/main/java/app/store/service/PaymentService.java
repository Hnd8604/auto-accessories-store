package app.store.service;

import app.store.dto.request.SepayWebhookRequest;
import app.store.dto.response.PaymentResponse;
import app.store.entity.Order;
import app.store.entity.Payment;
import app.store.enums.PaymentMethod;
import app.store.enums.PaymentStatus;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.OrderRepository;
import app.store.repository.PaymentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentService {

    OrderRepository orderRepository;
    PaymentRepository paymentRepository;

    @NonFinal     
    @Value("${sepay.bank-account-number}")
    String bankAccountNumber;
    
    @NonFinal
    @Value("${sepay.bank-name}")
    String bankName;
    
    @NonFinal
    @Value("${sepay.bank-account-name}")
    String bankAccountName;
    
    @NonFinal
    @Value("${sepay.bank-code}")
    String bankCode;
    
    @NonFinal
    @Value("${sepay.api-key}")
    String sepayApiKey;

    /**
     * Tạo mã đơn hàng duy nhất: DH + yyyyMMdd + 8 ký tự UUID viết hoa
     * VD: DH20240115A1B2C3D4
     */
    public String generateOrderCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "DH" + dateStr + randomPart;
    }

    /**
     * Tạo thông tin thanh toán cho đơn hàng (QR code VietQR).
     * FE gọi API này sau khi tạo đơn hàng với paymentMethod = BANK_TRANSFER.
     */
    public PaymentResponse createPayment(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_EXISTED));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Đơn hàng đã được thanh toán");
        }

        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new RuntimeException("Đơn hàng không sử dụng phương thức chuyển khoản");
        }

        String paymentContent = order.getOrderCode();

        // Tạo URL QR VietQR
        // Format: https://qr.sepay.vn/img?bank={bankCode}&acc={accountNumber}&template=compact&amount={amount}&des={content}
        String qrCodeUrl = buildQrCodeUrl(order.getTotalPrice(), paymentContent);

        return PaymentResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .amount(order.getTotalPrice())
                .qrCodeUrl(qrCodeUrl)
                .bankName(bankName)
                .bankAccountNumber(bankAccountNumber)
                .bankAccountName(bankAccountName)
                .paymentContent(paymentContent)
                .paymentStatus(order.getPaymentStatus())
                .build();
    }

    /**
     * Kiểm tra trạng thái thanh toán của đơn hàng.
     * FE có thể poll API này để biết đơn hàng đã thanh toán hay chưa.
     * Poll là liên tục gửi request tới BE để kiểm tra xem có dữ liệu mới hoặc thay đổi mới
     */
    public PaymentResponse checkPaymentStatus(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_EXISTED));

        PaymentResponse response = PaymentResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .amount(order.getTotalPrice())
                .paymentStatus(order.getPaymentStatus())
                .bankName(bankName)
                .bankAccountNumber(bankAccountNumber)
                .bankAccountName(bankAccountName)
                .paymentContent(order.getOrderCode())
                .build();

        // Nếu chưa thanh toán, trả thêm QR code
        if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
            response.setQrCodeUrl(buildQrCodeUrl(order.getTotalPrice(), order.getOrderCode()));
        }

        return response;
    }

    /**
     * Xử lý webhook từ SePay khi có giao dịch ngân hàng.
     * SePay gửi POST request tới store-backend với thông tin giao dịch.
     * Ta parse nội dung chuyển khoản (content) để tìm orderCode và cập nhật trạng thái.
     */
    @Transactional
    public boolean handleSepayWebhook(SepayWebhookRequest request) {
        log.info("=== SePay Webhook received ===");
        log.info("Gateway: {}, Amount: {}, Content: {}, TransferType: {}",
                request.getGateway(), request.getTransferAmount(),
                request.getContent(), request.getTransferType());

        // Chỉ xử lý giao dịch tiền VÀO
        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            log.info("Ignoring outgoing transaction");
            return true;
        }

        // Tìm orderCode từ content chuyển khoản
        String content = request.getContent();
        if (content == null || content.isBlank()) {
            log.warn("Webhook content is empty, skipping...");
            return true;
        }

        // Normalize content: uppercase, bỏ dấu cách
        String normalizedContent = content.toUpperCase().replaceAll("\\s+", "");

        // Tìm orderCode trong nội dung chuyển khoản (pattern: DH + 8 số + 8 ký tự)
        String orderCode = extractOrderCode(normalizedContent);
        if (orderCode == null) {
            log.warn("Could not extract orderCode from content: {}", content);
            return true;
        }

        log.info("Extracted orderCode: {}", orderCode);

        // Tìm đơn hàng theo orderCode
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            log.warn("Order not found for orderCode: {}", orderCode);
            return true;
        }

        // Kiểm tra đơn hàng đã thanh toán chưa
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Order {} already paid, skipping...", orderCode);
            return true;
        }

        // Kiểm tra số tiền
        BigDecimal transferAmount = BigDecimal.valueOf(request.getTransferAmount());
        if (transferAmount.compareTo(order.getTotalPrice()) < 0) {
            log.warn("Transfer amount {} is less than order total {}, orderCode: {}",
                    transferAmount, order.getTotalPrice(), orderCode);
            // Vẫn lưu giao dịch nhưng không update trạng thái
            savePaymentRecord(order, request, PaymentStatus.UNPAID);
            return true;
        }

        // Cập nhật trạng thái thanh toán
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        // Lưu bản ghi thanh toán
        savePaymentRecord(order, request, PaymentStatus.PAID);

        log.info("✅ Order {} has been marked as PAID! Amount: {}", orderCode, transferAmount);
        return true;
    }

    /**
     * Trích xuất mã đơn hàng từ nội dung chuyển khoản.
     * Pattern: DH + 8 chữ số (yyyyMMdd) + 8 ký tự alphanumeric
     * VD: DH20240115A1B2C3D4
     */
    private String extractOrderCode(String normalizedContent) {
        // Tìm pattern DH + 16 ký tự (8 số + 8 alpha)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(DH\\d{8}[A-Z0-9]{8})");
        java.util.regex.Matcher matcher = pattern.matcher(normalizedContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Lưu bản ghi giao dịch thanh toán
     */
    private void savePaymentRecord(Order order, SepayWebhookRequest request, PaymentStatus status) {
        Payment payment = Payment.builder()
                .order(order)
                .amount(BigDecimal.valueOf(request.getTransferAmount()))
                .gateway(request.getGateway())
                .transactionCode(request.getCode())
                .referenceCode(request.getReferenceCode())
                .transferContent(request.getContent())
                .accountNumber(request.getAccountNumber())
                .transactionDate(request.getTransactionDate())
                .status(status)
                .build();
        paymentRepository.save(payment);
    }

    /**
     * Tạo URL QR Code VietQR thông qua SePay.
     * URL format: https://qr.sepay.vn/img?bank={bankCode}&acc={accountNumber}&template=compact&amount={amount}&des={content}
     */
    private String buildQrCodeUrl(BigDecimal amount, String content) {
        return String.format(
                "https://qr.sepay.vn/img?bank=%s&acc=%s&template=compact&amount=%s&des=%s",
                URLEncoder.encode(bankCode, StandardCharsets.UTF_8),
                URLEncoder.encode(bankAccountNumber, StandardCharsets.UTF_8),
                amount.toBigInteger().toString(),
                URLEncoder.encode(content, StandardCharsets.UTF_8)
        );
    }

    // ==================== Webhook Verification ====================

    /**
     * Xác thực webhook request bằng API key.
     * So sánh constant-time để chống timing attack.
     */
    public void verifyApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("❌ Missing API key in webhook request");
            throw new AppException(ErrorCode.WEBHOOK_INVALID_SIGNATURE);
        }

        // SePay có thể gửi header dạng "Bearer <key>" hoặc "Apikey <key>", cắt bỏ prefix nếu có
        String cleanApiKey = apiKey.replace("Bearer ", "").replace("Apikey ", "").trim();

        if (!constantTimeEquals(cleanApiKey, sepayApiKey)) {
            log.error("❌ Invalid API key in webhook request. Expected: {}, Got: {}", sepayApiKey, cleanApiKey);
            throw new AppException(ErrorCode.WEBHOOK_INVALID_SIGNATURE);
        }

        log.info("✅ Webhook API key verified successfully");
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
