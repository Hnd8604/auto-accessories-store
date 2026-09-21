package app.store.exception;

import app.store.dto.request.user.UserCreationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.SQLException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validation_returnsFirstConstraintMessage() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"john","password":"short","email":"john@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters"));
    }

    @Test
    void malformedJson_returnsStableClientMessage() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void typeMismatch_andMissingParameter_returnMalformedRequest() throws Exception {
        mockMvc.perform(get("/test/number/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: id"));

        mockMvc.perform(get("/test/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("Missing required parameter: value"));
    }

    @Test
    void methodParameterValidation_returnsValidationError() throws Exception {
        mockMvc.perform(get("/test/page").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("Page must be at least 1"));
    }

    @Test
    void appException_usesItsStatusCodeAndMessage() throws Exception {
        mockMvc.perform(get("/test/app-error"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.ORDER_NOT_CANCELABLE.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ORDER_NOT_CANCELABLE.getMessage()));
    }

    @Test
    void unhandledException_returns500WithoutLeakingMessage() throws Exception {
        mockMvc.perform(get("/test/unhandled"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage()))
                .andExpect(content().string(not(containsString("secret-internal-message"))));
    }

    @Test
    void wrongMethod_preservesAllowHeader() throws Exception {
        mockMvc.perform(get("/test/only-post"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("POST")))
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.getCode()));
    }

    @Test
    void unknownRoute_returnsApiResponse404() throws Exception {
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ENDPOINT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ENDPOINT_NOT_FOUND.getMessage()));
    }

    @Test
    void unsupportedMediaType_hasDedicatedCode_andNotAcceptableReturns406() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode()));

        mockMvc.perform(get("/test/json").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().string(""));
    }

    @Test
    void uploadTooLarge_andAsyncTimeout_haveDedicatedCodes() throws Exception {
        mockMvc.perform(get("/test/upload-too-large"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAYLOAD_TOO_LARGE.getCode()));

        mockMvc.perform(get("/test/async-timeout"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(ErrorCode.ASYNC_REQUEST_TIMEOUT.getCode()));
    }

    @Test
    void accessDenied_returns403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void expectedDatabaseConflict_returns409_butUnexpectedIntegrityFailureReturns500() throws Exception {
        mockMvc.perform(get("/test/unique-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DATA_CONFLICT.getCode()))
                .andExpect(content().string(not(containsString("hidden-constraint"))));

        mockMvc.perform(get("/test/not-null-failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode()))
                .andExpect(content().string(not(containsString("hidden-column"))));
    }

    @RestController
    static class TestController {

        @PostMapping(value = "/test/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        void body(@Valid @RequestBody UserCreationRequest request) {
        }

        @GetMapping("/test/number/{id}")
        void number(@PathVariable Long id) {
        }

        @GetMapping("/test/required")
        void required(@RequestParam String value) {
        }

        @GetMapping("/test/page")
        void page(@RequestParam @Min(value = 1, message = "Page must be at least 1") int page) {
        }

        @GetMapping("/test/app-error")
        void appError() {
            throw new AppException(ErrorCode.ORDER_NOT_CANCELABLE);
        }

        @GetMapping("/test/unhandled")
        void unhandled() {
            throw new RuntimeException("secret-internal-message");
        }

        @PostMapping("/test/only-post")
        void onlyPost() {
        }

        @GetMapping(value = "/test/json", produces = MediaType.APPLICATION_JSON_VALUE)
        String json() {
            return "{}";
        }

        @GetMapping("/test/upload-too-large")
        void uploadTooLarge() {
            throw new MaxUploadSizeExceededException(10);
        }

        @GetMapping("/test/async-timeout")
        void asyncTimeout() {
            throw new AsyncRequestTimeoutException();
        }

        @GetMapping("/test/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("hidden-security-detail");
        }

        @GetMapping("/test/unique-conflict")
        void uniqueConflict() {
            throw new DataIntegrityViolationException(
                    "hidden-constraint", new SQLException("hidden-constraint", "23505"));
        }

        @GetMapping("/test/not-null-failure")
        void notNullFailure() {
            throw new DataIntegrityViolationException(
                    "hidden-column", new SQLException("hidden-column", "23502"));
        }
    }
}
