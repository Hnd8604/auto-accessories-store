package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import app.store.dto.OtpRedisModel;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock
    RedisTemplate<String, Object> objectRedisTemplate;
    @Mock
    ValueOperations<String, Object> valueOperations; // mock lồng: opsForValue() trả về mock này
    @InjectMocks
    OtpService otpService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(10);

    private static final String EMAIL = "john@mail.com";
    private static final String KEY = "otp:reset:john@mail.com";

    @Test
    void generateOtp_shouldReturn6Digits_andStoreHashedOtpWith5MinuteTtl() {
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String otp = otpService.generateOtp(EMAIL);

        assertThat(otp).matches("\\d{6}");

        ArgumentCaptor<OtpRedisModel> captor = ArgumentCaptor.forClass(OtpRedisModel.class);
        verify(valueOperations).set(eq(KEY), captor.capture(), eq(5L), eq(TimeUnit.MINUTES));
        OtpRedisModel stored = captor.getValue();
        assertThat(stored.isUsed()).isFalse();
        assertThat(stored.getOtpHash()).isNotEqualTo(otp);          // lưu hash, không lưu OTP thô
        assertThat(encoder.matches(otp, stored.getOtpHash())).isTrue();
    }

    @Test
    void verifyOtp_shouldThrow_whenOtpNotFoundOrExpired() {
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("hết hạn");
    }

    @Test
    void verifyOtp_shouldThrow_whenOtpAlreadyUsed() {
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY))
                .thenReturn(new OtpRedisModel(encoder.encode("123456"), true));

        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã được sử dụng");
    }

    @Test
    void verifyOtp_shouldThrow_whenOtpWrong() {
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY))
                .thenReturn(new OtpRedisModel(encoder.encode("123456"), false));

        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "999999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không đúng");

        verify(valueOperations, never()).set(any(), any(), anyLong(), any());
    }

    @Test
    void verifyOtp_shouldMarkAsUsed_whenCorrect() {
        OtpRedisModel model = new OtpRedisModel(encoder.encode("123456"), false);
        when(objectRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(model);
        when(objectRedisTemplate.getExpire(KEY)).thenReturn(120L);

        otpService.verifyOtp(EMAIL, "123456");

        assertThat(model.isUsed()).isTrue();
        // Ghi lại vào Redis với TTL còn lại (giây) để OTP không dùng được lần 2
        verify(valueOperations).set(KEY, model, 120L, TimeUnit.SECONDS);
    }

    @Test
    void deleteOtp_shouldDeleteByKey() {
        otpService.deleteOtp(EMAIL);

        verify(objectRedisTemplate).delete(KEY);
    }
}
