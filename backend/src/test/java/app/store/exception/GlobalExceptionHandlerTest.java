package app.store.exception;

import app.store.dto.request.user.UserCreationRequest;
import app.store.dto.response.auth.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlingValidation_returnsConstraintMessageWithInterpolatedAttributes() throws Exception {
        UserCreationRequest request = UserCreationRequest.builder()
                .username("john")
                .password("short")
                .build();

        ResponseEntity<ApiResponse<?>> response = handler.handlingValidation(validate(request));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
        assertThat(response.getBody().message()).isEqualTo("Password must be at least 8 characters");
    }

    @Test
    void handlingValidation_withoutErrors_fallsBackToErrorCodeMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        ResponseEntity<ApiResponse<?>> response = handler.handlingValidation(
                new MethodArgumentNotValidException(methodParameter(), bindingResult));

        assertThat(response.getBody().message()).isEqualTo(ErrorCode.VALIDATION_ERROR.getMessage());
    }

    private MethodArgumentNotValidException validate(Object target) throws Exception {
        try (LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
            validator.validate(target, bindingResult);
            return new MethodArgumentNotValidException(methodParameter(), bindingResult);
        }
    }

    private MethodParameter methodParameter() throws NoSuchMethodException {
        return new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("methodParameter"), -1);
    }
}
