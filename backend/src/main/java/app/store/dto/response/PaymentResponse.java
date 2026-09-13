package app.store.dto.response;

import app.store.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    PaymentStatus paymentStatus;
    String checkoutUrl;          // Trang thanh toán payOS, frontend nhúng vào dialog
    String paymentLinkId;
    LocalDateTime expiredAt;     // Hết hạn thì gọi lại /create để lấy link mới
}
