package app.store.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MailService {
    JavaMailSender mailSender;

    private static final String FROM_NAME = "Store App";

    /**
     * Gửi email thông báo đặt hàng thành công
     */
    public void sendOrderCreatedEmail(String toEmail, String recipientName, String orderCode, BigDecimal totalPrice) {
        String subject = "Xác nhận đơn hàng #" + orderCode;
        String formattedPrice = formatCurrency(totalPrice);
        String body = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2 style="color: #2E86C1;">Đơn hàng của bạn đã được đặt thành công!</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Cảm ơn bạn đã đặt hàng tại Store App. Dưới đây là thông tin đơn hàng:</p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 400px;">
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Mã đơn hàng</td>
                            <td style="padding: 8px; border: 1px solid #ddd;">#%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Tổng tiền</td>
                            <td style="padding: 8px; border: 1px solid #ddd; color: #E74C3C; font-weight: bold;">%s</td>
                        </tr>
                    </table>
                    <p>Chúng tôi sẽ thông báo cho bạn khi đơn hàng được xử lý.</p>
                    <p style="color: #888; font-size: 12px;">Đây là email tự động, vui lòng không trả lời.</p>
                </body>
                </html>
                """.formatted(recipientName, orderCode, formattedPrice);

        sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Gửi email thông báo thay đổi trạng thái đơn hàng
     */
    public void sendOrderStatusChangedEmail(String toEmail, String recipientName, String orderCode,
                                            String oldStatus, String newStatus) {
        String subject = "Cập nhật đơn hàng #" + orderCode;
        String statusColor = getStatusColor(newStatus);
        String body = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2 style="color: #2E86C1;">Đơn hàng của bạn đã được cập nhật!</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Đơn hàng <strong>#%s</strong> đã được cập nhật trạng thái:</p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 400px;">
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Trạng thái cũ</td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Trạng thái mới</td>
                            <td style="padding: 8px; border: 1px solid #ddd; color: %s; font-weight: bold;">%s</td>
                        </tr>
                    </table>
                    <p style="color: #888; font-size: 12px;">Đây là email tự động, vui lòng không trả lời.</p>
                </body>
                </html>
                """.formatted(recipientName, orderCode, oldStatus, statusColor, newStatus);

        sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Gửi email OTP quên mật khẩu
     */
    public void sendForgotPasswordEmail(String toEmail, String otp) {
        String subject = "Mã OTP đặt lại mật khẩu";
        String body = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2 style="color: #2E86C1;">Đặt lại mật khẩu</h2>
                    <p>Mã OTP của bạn là:</p>
                    <div style="background: #f4f4f4; padding: 15px; text-align: center; font-size: 28px;
                                font-weight: bold; letter-spacing: 8px; border-radius: 8px; margin: 20px 0;">
                        %s
                    </div>
                    <p>Mã này có hiệu lực trong <strong>5 phút</strong>.</p>
                    <p style="color: #E74C3C;">Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>
                    <p style="color: #888; font-size: 12px;">Đây là email tự động, vui lòng không trả lời.</p>
                </body>
                </html>
                """.formatted(otp);

        sendHtmlEmail(toEmail, subject, body);
    }

    // ==================== Helper Methods ====================

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    private String getStatusColor(String status) {
        return switch (status.toUpperCase()) {
            case "DELIVERED" -> "#27AE60";
            case "CANCELED" -> "#E74C3C";
            case "SHIPPING" -> "#F39C12";
            case "PROCESSING" -> "#3498DB";
            default -> "#333";
        };
    }
}
