package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
public class MailServiceTest {

    @Mock
    JavaMailSender mailSender;
    @InjectMocks
    MailService mailService;

    /** MimeMessageHelper cần một MimeMessage thật, không mock được nội dung bên trong. */
    private MimeMessage realMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    /**
     * Helper bật multipart nên nội dung HTML nằm lồng trong nhiều lớp Multipart.
     * Duyệt đệ quy để gom toàn bộ phần text ra so sánh.
     */
    private String htmlBodyOf(MimeMessage message) throws Exception {
        StringBuilder builder = new StringBuilder();
        collectText(message.getContent(), builder);
        return builder.toString();
    }

    private void collectText(Object content, StringBuilder builder) throws Exception {
        if (content instanceof String text) {
            builder.append(text);
        } else if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                collectText(multipart.getBodyPart(i).getContent(), builder);
            }
        }
    }

    @Test
    void sendOrderCreatedEmail_shouldSetSubjectWithOrderCode_andHtmlBody() throws Exception {
        MimeMessage message = realMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        mailService.sendOrderCreatedEmail("john@mail.com", "John", "DH123", BigDecimal.valueOf(200_000));

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Xác nhận đơn hàng #DH123");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("john@mail.com");
        String body = htmlBodyOf(message);
        assertThat(body).contains("John").contains("DH123").contains("200.000");
    }

    @Test
    void sendOrderStatusChangedEmail_shouldMentionBothStatuses() throws Exception {
        MimeMessage message = realMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        mailService.sendOrderStatusChangedEmail("john@mail.com", "John", "DH123", "PENDING", "SHIPPING");

        assertThat(message.getSubject()).isEqualTo("Cập nhật đơn hàng #DH123");
        String body = htmlBodyOf(message);
        assertThat(body).contains("PENDING").contains("SHIPPING")
                .contains("#F39C12"); // màu riêng của trạng thái SHIPPING
    }

    @Test
    void sendForgotPasswordEmail_shouldContainOtp() throws Exception {
        MimeMessage message = realMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        mailService.sendForgotPasswordEmail("john@mail.com", "123456");

        assertThat(message.getSubject()).isEqualTo("Mã OTP đặt lại mật khẩu");
        assertThat(htmlBodyOf(message)).contains("123456");
    }

    @Test
    void sendEmail_shouldThrowRuntimeException_whenRecipientInvalid() {
        MimeMessage message = realMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        assertThatThrownBy(() -> mailService.sendForgotPasswordEmail("dia-chi-sai@@", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send email");

        verify(mailSender, never()).send(message);
    }
}
