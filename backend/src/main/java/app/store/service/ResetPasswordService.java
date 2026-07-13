package app.store.service;

import app.store.dto.ResetPasswordSession;
import app.store.dto.request.ConfirmResetPasswordRequest;
import app.store.dto.request.InitResetPasswordRequest;
import app.store.dto.request.ResendOtpRequest;
import app.store.dto.request.VerifyOtpRequest;
import app.store.dto.response.InitResetPasswordResponse;
import app.store.dto.response.ResendOtpResponse;
import app.store.dto.response.VerifyOtpResponse;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ResetPasswordService {
    
    RedisTemplate<String, Object> objectRedisTemplate;
    UserRepository userRepository;
    MailService mailService;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
    SecureRandom secureRandom = new SecureRandom();
    
    private static final String REDIS_KEY_PREFIX = "reset:session:";
    private static final long SESSION_TTL_MINUTES = 5;

    public InitResetPasswordResponse initResetPassword(InitResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        
        // Verify email exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));
        
        // Generate secure session ID (base64 encoded 32 bytes)
        String sessionId = generateSessionId();
        
        // Generate 6-digit OTP
        String otp = generateOtp();
        
        long now = System.currentTimeMillis();
        
        // Create session
        ResetPasswordSession session = ResetPasswordSession.builder()
                .userId(user.getId())
                .email(email)
                .step(ResetPasswordSession.STEP_EMAIL_VERIFIED)
                .otpHash(passwordEncoder.encode(otp))
                .otpAttempt(0)
                .otpExpireAt(now + ResetPasswordSession.OTP_TTL_MILLIS)
                .createdAt(now)
                .lastSentAt(now)
                .build();
        
        // Save to Redis with TTL
        saveSession(sessionId, session);
        
        // Send OTP email
        mailService.sendForgotPasswordEmail(email, otp);
        
        log.info("Password reset initiated for email: {}, sessionId: {}", maskEmail(email), sessionId);
        
        return InitResetPasswordResponse.builder()
                .sessionId(sessionId)
                .maskedEmail(maskEmail(email))
                .build();
    }

    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String sessionId = request.getSessionId();
        String inputOtp = request.getOtp();
        
        ResetPasswordSession session = getSession(sessionId);
        
        if (session == null) {
            throw new AppException(ErrorCode.RESET_SESSION_NOT_FOUND);
        }
        
        // Check if session is at correct step
        if (!ResetPasswordSession.STEP_EMAIL_VERIFIED.equals(session.getStep())) {
            throw new AppException(ErrorCode.RESET_INVALID_STEP);
        }
        
        // Check if OTP has expired
        long now = System.currentTimeMillis();
        if (now > session.getOtpExpireAt()) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }
        
        // Check if max attempts exceeded
        if (session.getOtpAttempt() >= ResetPasswordSession.MAX_OTP_ATTEMPTS) {
            // Delete session for security
            deleteSession(sessionId);
            throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }
        
        // Verify OTP
        if (!passwordEncoder.matches(inputOtp, session.getOtpHash())) {
            // Increment attempt counter
            session.setOtpAttempt(session.getOtpAttempt() + 1);
            saveSession(sessionId, session);
            
            int remainingAttempts = ResetPasswordSession.MAX_OTP_ATTEMPTS - session.getOtpAttempt();
            log.warn("Invalid OTP attempt for session: {}, remaining: {}", sessionId, remainingAttempts);
            
            throw new AppException(ErrorCode.OTP_INVALID);
        }
        
        // OTP verified, update step
        session.setStep(ResetPasswordSession.STEP_OTP_VERIFIED);
        saveSession(sessionId, session);
        
        log.info("OTP verified successfully for session: {}", sessionId);
        
        return VerifyOtpResponse.builder()
                .sessionId(sessionId)
                .verified(true)
                .remainingAttempts(null)
                .build();
    }

    public void confirmResetPassword(ConfirmResetPasswordRequest request) {
        String sessionId = request.getSessionId();
        String newPassword = request.getNewPassword();
        
        ResetPasswordSession session = getSession(sessionId);
        
        if (session == null) {
            throw new AppException(ErrorCode.RESET_SESSION_NOT_FOUND);
        }
        
        // Check if session is at correct step
        if (!ResetPasswordSession.STEP_OTP_VERIFIED.equals(session.getStep())) {
            throw new AppException(ErrorCode.RESET_INVALID_STEP);
        }
        
        // Get user and update password
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Delete session
        deleteSession(sessionId);
        
        log.info("Password reset completed for user: {}", user.getUsername());
    }

    public ResendOtpResponse resendOtp(ResendOtpRequest request) {
        String sessionId = request.getSessionId();
        
        ResetPasswordSession session = getSession(sessionId);
        
        if (session == null) {
            throw new AppException(ErrorCode.RESET_SESSION_NOT_FOUND);
        }
        
        // Only allow resend if at EMAIL_VERIFIED step
        if (!ResetPasswordSession.STEP_EMAIL_VERIFIED.equals(session.getStep())) {
            throw new AppException(ErrorCode.RESET_INVALID_STEP);
        }
        
        long now = System.currentTimeMillis();
        long timeSinceLastSent = now - session.getLastSentAt();
        
        // Check cooldown
        if (timeSinceLastSent < ResetPasswordSession.RESEND_COOLDOWN_MILLIS) {
            long waitSeconds = (ResetPasswordSession.RESEND_COOLDOWN_MILLIS - timeSinceLastSent) / 1000;
            throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON, 
                    "Vui lòng đợi " + waitSeconds + " giây trước khi gửi lại OTP");
        }
        
        // Generate new OTP
        String otp = generateOtp();
        
        // Update session with new OTP
        session.setOtpHash(passwordEncoder.encode(otp));
        session.setOtpAttempt(0); // Reset attempt counter
        session.setOtpExpireAt(now + ResetPasswordSession.OTP_TTL_MILLIS);
        session.setLastSentAt(now);
        
        saveSession(sessionId, session);
        
        // Send email
        mailService.sendForgotPasswordEmail(session.getEmail(), otp);
        
        log.info("OTP resent for session: {}", sessionId);
        
        return ResendOtpResponse.builder()
                .sessionId(sessionId)
                .maskedEmail(maskEmail(session.getEmail()))
                .build();
    }
    
    // ==================== Helper Methods ====================
    
    private String generateSessionId() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(999999));
    }
    
    private String buildRedisKey(String sessionId) {
        return REDIS_KEY_PREFIX + sessionId;
    }
    
    private void saveSession(String sessionId, ResetPasswordSession session) {
        objectRedisTemplate.opsForValue().set(
                buildRedisKey(sessionId),
                session,
                SESSION_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }
    
    private ResetPasswordSession getSession(String sessionId) {
        return (ResetPasswordSession) objectRedisTemplate.opsForValue()
                .get(buildRedisKey(sessionId));
    }
    
    private void deleteSession(String sessionId) {
        objectRedisTemplate.delete(buildRedisKey(sessionId));
    }
    
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String prefix = email.substring(0, 1);
        String domain = email.substring(atIndex);
        return prefix + "***" + domain;
    }
}
