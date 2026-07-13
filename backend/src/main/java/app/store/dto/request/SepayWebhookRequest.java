package app.store.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO đại diện cho payload webhook mà SePay gửi khi có giao dịch ngân hàng.
 * 
 * Ví dụ payload:
 * {
 *   "id": 93,
 *   "gateway": "MBBank",
 *   "transactionDate": "2024-01-15 10:30:00",
 *   "accountNumber": "0123456789",
 *   "subAccount": null,
 *   "transferType": "in",
 *   "transferAmount": 500000,
 *   "accumulated": 1500000,
 *   "code": "TF123456",
 *   "content": "DH20240115ABC123",
 *   "referenceCode": "FT24015ABCDE",
 *   "description": "MBBank-0123456789-DH20240115ABC123"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SepayWebhookRequest {
    Long id;
    String gateway;
    String transactionDate;
    String accountNumber;
    String subAccount;
    String transferType;      // "in" = tiền vào, "out" = tiền ra
    Long transferAmount;
    Long accumulated;
    String code;
    String content;           // Nội dung chuyển khoản - chứa orderCode
    String referenceCode;
    String description;
}
