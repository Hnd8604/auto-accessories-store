package app.store.dto.response;

import app.store.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    String orderId;
    String orderCode;
    BigDecimal amount;
    String qrCodeUrl;          // URL ảnh QR VietQR
    String bankName;
    String bankAccountNumber;
    String bankAccountName;
    String paymentContent;     // Nội dung chuyển khoản
    PaymentStatus paymentStatus;
}
