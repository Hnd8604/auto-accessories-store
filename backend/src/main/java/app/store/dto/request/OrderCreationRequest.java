package app.store.dto.request;

import app.store.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record OrderCreationRequest(
        // Không có userId: người đặt hàng luôn là user trong JWT, không lấy từ body.
        @NotBlank(message = "Recipient name is required")
        @Size(max = 255, message = "Recipient name must not exceed {max} characters")
        String nameRecipient,
        @NotBlank(message = "Recipient phone is required")
        @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Recipient phone is invalid")
        String phoneRecipient,
        @NotBlank(message = "Recipient address is required")
        @Size(max = 255, message = "Recipient address must not exceed {max} characters")
        String addressRecipient,
        @Size(max = 255, message = "Note must not exceed {max} characters")
        String note,
        // null thì service mặc định COD
        PaymentMethod paymentMethod,
        @NotEmpty(message = "Order must contain at least one item")
        List<@Valid OrderDetailRequest> orderDetails
) {
}
