package app.store.service;

import app.store.dto.OtpRedisModel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OtpService {
    RedisTemplate<String, Object> objectRedisTemplate;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);;

    private static final long OTP_TTL_MINUTES = 5;

    public String generateOtp(String email) {
        String otp = String.format("%06d",
                new SecureRandom().nextInt(999999));

        String key = buildKey(email);

        OtpRedisModel otpModel = new OtpRedisModel(
                passwordEncoder.encode(otp),
                false // chưa sử dụng
        );

        objectRedisTemplate.opsForValue().set(
                key,
                otpModel,
                OTP_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        return otp;
    }

    public void verifyOtp(String email, String inputOtp) {
        String key = buildKey(email);

        OtpRedisModel otpModel =
                (OtpRedisModel) objectRedisTemplate.opsForValue().get(key);

        if (otpModel == null) {
            throw new RuntimeException("OTP không tồn tại hoặc đã hết hạn");
        }

        if (otpModel.isUsed()) {
            throw new RuntimeException("OTP đã được sử dụng");
        }

        if (!passwordEncoder.matches(inputOtp, otpModel.getOtpHash())) {
            throw new RuntimeException("OTP không đúng");
        }

        // Đánh dấu OTP đã dùng
        otpModel.setUsed(true);
        objectRedisTemplate.opsForValue().set(
                key,
                otpModel,
                objectRedisTemplate.getExpire(key),
                TimeUnit.SECONDS
        );
    }
    public void deleteOtp(String email) {
        objectRedisTemplate.delete(buildKey(email));
    }

    private String buildKey(String email) {
        return "otp:reset:" + email;
    }
}

