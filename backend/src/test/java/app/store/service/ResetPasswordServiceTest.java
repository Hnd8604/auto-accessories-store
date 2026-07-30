package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import app.store.dto.ResetPasswordSession;
import app.store.dto.request.ConfirmResetPasswordRequest;
import app.store.dto.request.InitResetPasswordRequest;
import app.store.dto.request.ResendOtpRequest;
import app.store.dto.request.VerifyOtpRequest;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class ResetPasswordServiceTest {

    @Mock
    RedisTemplate<String, Object> objectRedisTemplate;
    @Mock
    ValueOperations<String, Object> valueOperations;
    @Mock
    UserRepository userRepository;
    @Mock
    MailService mailService;
    @InjectMocks
    ResetPasswordService resetPasswordService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(10);

    private static final String SESSION_ID = "sess-1";
    private static final String REDIS_KEY = "reset:session:sess-1";

    private User buildUser() {
        User user = new User();
        user.setId("u1");
        user.setUsername("john");
        user.setEmail("john@mail.com");
        user.setPassword(encoder.encode("old-password"));
        return user;
    }

    private ResetPasswordSession buildSession(String step, String rawOtp) {
        long now = System.currentTimeMillis();
        return ResetPasswordSession.builder()
                .userId("u1")
                .email("john@mail.com")
                .step(step)
                .otpHash(encoder.encode(rawOtp))
                .otpAttempt(0)
                .otpExpireAt(now + ResetPasswordSession.OTP_TTL_MILLIS)
                .createdAt(now)
                .lastSentAt(now)
                .build();
    }

    // ==================== initResetPassword ====================

    @Test
    void initResetPassword_shouldCreateSession_sendOtp_andMaskEmail() {
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(buildUser()));

        var response = resetPasswordService.initResetPassword(
                InitResetPasswordRequest.builder().email("  John@Mail.com ").build()); // có khoảng trắng + hoa

        assertThat(response.getSessionId()).isNotBlank();
        assertThat(response.getMaskedEmail()).isEqualTo("j***@mail.com");

        // OTP gửi qua mail phải khớp với hash lưu trong Redis
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendForgotPasswordEmail(eq("john@mail.com"), otpCaptor.capture());
        assertThat(otpCaptor.getValue()).matches("\\d{6}");

        ArgumentCaptor<ResetPasswordSession> sessionCaptor =
                ArgumentCaptor.forClass(ResetPasswordSession.class);
        verify(valueOperations).set(anyString(), sessionCaptor.capture(), eq(5L), eq(TimeUnit.MINUTES));
        ResetPasswordSession saved = sessionCaptor.getValue();
        assertThat(saved.getStep()).isEqualTo(ResetPasswordSession.STEP_EMAIL_VERIFIED);
        assertThat(saved.getOtpAttempt()).isZero();
        assertThat(encoder.matches(otpCaptor.getValue(), saved.getOtpHash())).isTrue();
    }

    @Test
    void initResetPassword_shouldThrow_whenEmailNotExisted() {
        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resetPasswordService.initResetPassword(
                InitResetPasswordRequest.builder().email("ghost@mail.com").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_NOT_EXISTED);

        verify(mailService, never()).sendForgotPasswordEmail(any(), any());
    }

    // ==================== verifyOtp ====================

    @Test
    void verifyOtp_shouldMoveSessionToOtpVerified_whenCorrect() {
        ResetPasswordSession session =
                buildSession(ResetPasswordSession.STEP_EMAIL_VERIFIED, "123456");
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(session);

        var response = resetPasswordService.verifyOtp(
                VerifyOtpRequest.builder().sessionId(SESSION_ID).otp("123456").build());

        assertThat(response.isVerified()).isTrue();
        assertThat(session.getStep()).isEqualTo(ResetPasswordSession.STEP_OTP_VERIFIED);
        verify(valueOperations).set(eq(REDIS_KEY), eq(session), anyLong(), any());
    }

    @Test
    void verifyOtp_shouldThrow_whenSessionNotFound() {
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);

        assertThatThrownBy(() -> resetPasswordService.verifyOtp(
                VerifyOtpRequest.builder().sessionId(SESSION_ID).otp("123456").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESET_SESSION_NOT_FOUND);
    }

    @Test
    void verifyOtp_shouldThrow_whenWrongStep() {
        ResetPasswordSession session =
                buildSession(ResetPasswordSession.STEP_OTP_VERIFIED, "123456"); // đã verify rồi
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(session);

        assertThatThrownBy(() -> resetPasswordService.verifyOtp(
                VerifyOtpRequest.builder().sessionId(SESSION_ID).otp("123456").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESET_INVALID_STEP);
    }

    @Test
    void verifyOtp_shouldThrow_whenOtpExpired() {
        ResetPasswordSession session =
                buildSession(ResetPasswordSession.STEP_EMAIL_VERIFIED, "123456");
        session.setOtpExpireAt(System.currentTimeMillis() - 1000); // hết hạn 1 giây trước
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(session);

        assertThatThrownBy(() -> resetPasswordService.verifyOtp(
                VerifyOtpRequest.builder().sessionId(SESSION_ID).otp("123456").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.OTP_EXPIRED);
    }

    @Test
    void verifyOtp_shouldDeleteSession_whenMaxAttemptsExceeded() {
        ResetPasswordSession session =
                buildSession(ResetPasswordSession.STEP_EMAIL_VERIFIED, "123456");
        session.setOtpAttempt(ResetPasswordSession.MAX_OTP_ATTEMPTS);
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(session);

        assertThatThrownBy(() -> resetPasswordService.verifyOtp(
                VerifyOtpRequest.builder().sessionId(SESSION_ID).otp("123456").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);

        verify(objectRedisTemplate).delete(REDIS_KEY); // xoá phiên để chống brute-force
    }

    @Test
    void verifyOtp_shouldIncreaseAttempt_whenOtpWrong() {
        ResetPasswordSession session =
                buildSession(ResetPasswordSession.STEP_EMAIL_VERIFIED, "123456");
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(session);

        assertThatThrownBy(() -> resetPasswordService.verifyOtp(
                VerifyOtpRequest.builder().sessionId(SESSION_ID).otp("999999").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.OTP_INVALID);

        assertThat(session.getOtpAttempt()).isEqualTo(1);
        assertThat(session.getStep()).isEqualTo(ResetPasswordSession.STEP_EMAIL_VERIFIED);
    }

    // ==================== confirmResetPassword ====================

    @Test
    void confirmResetPassword_shouldSaveNewPassword_andDeleteSession() {
        ResetPasswordSession session =
                buildSession(ResetPasswordSession.STEP_OTP_VERIFIED, "123456");
        User user = buildUser();
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(session);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        resetPasswordService.confirmResetPassword(ConfirmResetPasswordRequest.builder()
                .sessionId(SESSION_ID).newPassword("new-password").build());

        assertThat(encoder.matches("new-password", user.getPassword())).isTrue();
        verify(userRepository).save(user);
        verify(objectRedisTemplate).delete(REDIS_KEY);
    }

    @Test
    void confirmResetPassword_shouldThrow_whenOtpNotVerifiedYet() {
        ResetPasswordSession session =
                buildSession(ResetPasswordSession.STEP_EMAIL_VERIFIED, "123456");
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(session);

        assertThatThrownBy(() -> resetPasswordService.confirmResetPassword(
                ConfirmResetPasswordRequest.builder()
                        .sessionId(SESSION_ID).newPassword("new-password").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESET_INVALID_STEP);

        verify(userRepository, never()).save(any());
    }

    // ==================== resendOtp ====================

    @Test
    void resendOtp_shouldThrow_whenStillInCooldown() {
        ResetPasswordSession session =
                buildSession(ResetPasswordSession.STEP_EMAIL_VERIFIED, "123456");
        session.setLastSentAt(System.currentTimeMillis()); // vừa gửi xong
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(session);

        assertThatThrownBy(() -> resetPasswordService.resendOtp(
                ResendOtpRequest.builder().sessionId(SESSION_ID).build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.OTP_RESEND_TOO_SOON);

        verify(mailService, never()).sendForgotPasswordEmail(any(), any());
    }

    @Test
    void resendOtp_shouldIssueNewOtp_andResetAttempt_whenCooldownPassed() {
        ResetPasswordSession session =
                buildSession(ResetPasswordSession.STEP_EMAIL_VERIFIED, "123456");
        session.setOtpAttempt(3);
        session.setLastSentAt(System.currentTimeMillis()
                - ResetPasswordSession.RESEND_COOLDOWN_MILLIS - 1000);
        String oldHash = session.getOtpHash();

        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(session);

        var response = resetPasswordService.resendOtp(
                ResendOtpRequest.builder().sessionId(SESSION_ID).build());

        assertThat(response.getMaskedEmail()).isEqualTo("j***@mail.com");
        assertThat(session.getOtpAttempt()).isZero();
        assertThat(session.getOtpHash()).isNotEqualTo(oldHash);

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendForgotPasswordEmail(eq("john@mail.com"), otpCaptor.capture());
        assertThat(encoder.matches(otpCaptor.getValue(), session.getOtpHash())).isTrue();
    }
}
