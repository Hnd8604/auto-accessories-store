package app.store.exception;

import app.store.dto.response.auth.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        if (errorCode.getStatusCode().is5xxServerError()) {
            log.error("Application failure, returning {}", errorCode.getStatusCode(), exception);
        }
        return build(errorCode, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<?>> handlingAccessDeniedException() {
        return build(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<?>> handlingDataIntegrityViolationException(
            DataIntegrityViolationException exception) {
        String sqlState = findSqlState(exception);
        if (UNIQUE_VIOLATION.equals(sqlState) || FOREIGN_KEY_VIOLATION.equals(sqlState)) {
            log.warn("Database conflict (SQL state {})", sqlState);
            return build(ErrorCode.DATA_CONFLICT);
        }

        log.error("Unexpected data integrity failure", exception);
        return build(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    ResponseEntity<ApiResponse<?>> handlingInfrastructureException(RuntimeException exception) {
        log.error("Infrastructure failure", exception);
        return build(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<?>> handlingValidation(MethodArgumentNotValidException exception) {
        return build(
                ErrorCode.VALIDATION_ERROR,
                firstValidationMessage(exception.getBindingResult().getAllErrors()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiResponse<?>> handlingMethodValidation(
            HandlerMethodValidationException exception) {
        return build(
                ErrorCode.VALIDATION_ERROR,
                firstValidationMessage(exception.getAllErrors()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<?>> handlingHttpMessageNotReadable(
            HttpMessageNotReadableException exception) {
        log.debug("Malformed request body", exception);
        return build(ErrorCode.MALFORMED_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(TypeMismatchException.class)
    ResponseEntity<ApiResponse<?>> handlingTypeMismatch(TypeMismatchException exception) {
        String message = exception instanceof MethodArgumentTypeMismatchException mismatch
                ? "Invalid value for parameter: " + mismatch.getName()
                : ErrorCode.MALFORMED_REQUEST.getMessage();
        return build(ErrorCode.MALFORMED_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<?>> handlingMissingRequestParameter(
            MissingServletRequestParameterException exception) {
        return build(
                ErrorCode.MALFORMED_REQUEST,
                "Missing required parameter: " + exception.getParameterName(),
                exception.getHeaders());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ApiResponse<?>> handlingMissingRequestPart(
            MissingServletRequestPartException exception) {
        return build(
                ErrorCode.MALFORMED_REQUEST,
                "Missing required part: " + exception.getRequestPartName(),
                exception.getHeaders());
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    ResponseEntity<ApiResponse<?>> handlingServletRequestBinding(
            ServletRequestBindingException exception) {
        return build(ErrorCode.MALFORMED_REQUEST, null, exception.getHeaders());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<?>> handlingMethodNotSupported(
            HttpRequestMethodNotSupportedException exception) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, null, exception.getHeaders());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ApiResponse<?>> handlingEndpointNotFound(Exception exception) {
        return build(ErrorCode.ENDPOINT_NOT_FOUND);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<?>> handlingMaxUploadSize(
            MaxUploadSizeExceededException exception) {
        log.debug("Upload exceeds configured size limit", exception);
        return build(ErrorCode.PAYLOAD_TOO_LARGE, null, exception.getHeaders());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<?>> handlingMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception) {
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null, exception.getHeaders());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ApiResponse<?>> handlingMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception) {
        // The client rejected JSON, so an ApiResponse body cannot be serialized safely.
        return new ResponseEntity<>(
                null, exception.getHeaders(), ErrorCode.NOT_ACCEPTABLE.getStatusCode());
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    ResponseEntity<ApiResponse<?>> handlingAsyncRequestTimeout(
            AsyncRequestTimeoutException exception,
            HttpServletResponse response) {
        log.debug("Async request timed out");
        if (response.isCommitted()) {
            return null;
        }
        return build(ErrorCode.ASYNC_REQUEST_TIMEOUT, null, exception.getHeaders());
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    void handlingAsyncRequestNotUsable(AsyncRequestNotUsableException exception) {
        log.debug("Async response is no longer usable", exception);
    }

    @ExceptionHandler(ConversionNotSupportedException.class)
    ResponseEntity<ApiResponse<?>> handlingConversionNotSupported(
            ConversionNotSupportedException exception) {
        log.error("Server-side conversion failure", exception);
        return build(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    ResponseEntity<ApiResponse<?>> handlingHttpMessageNotWritable(
            HttpMessageNotWritableException exception) {
        log.error("Failed to serialize response", exception);
        return build(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ApiResponse<?>> handlingMultipartException(MultipartException exception) {
        log.debug("Malformed multipart request", exception);
        return build(ErrorCode.MALFORMED_REQUEST, "Malformed multipart request");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<?>> handlingUnhandledException(Exception exception) {
        log.error("Unhandled exception", exception);
        return build(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    private ResponseEntity<ApiResponse<?>> build(ErrorCode errorCode) {
        return build(errorCode, null);
    }

    private ResponseEntity<ApiResponse<?>> build(ErrorCode errorCode, String message) {
        return build(errorCode, message, HttpHeaders.EMPTY);
    }

    private ResponseEntity<ApiResponse<?>> build(
            ErrorCode errorCode,
            String message,
            HttpHeaders headers) {
        return build(errorCode, message, headers, errorCode.getStatusCode());
    }

    private ResponseEntity<ApiResponse<?>> build(
            ErrorCode errorCode,
            String message,
            HttpHeaders headers,
            HttpStatusCode statusCode) {
        return new ResponseEntity<>(response(errorCode, message), headers, statusCode);
    }

    private ApiResponse<?> response(ErrorCode errorCode, String message) {
        return ApiResponse.builder()
                .code(errorCode.getCode())
                .message(message != null ? message : errorCode.getMessage())
                .build();
    }

    private String firstValidationMessage(
            Iterable<? extends MessageSourceResolvable> errors) {
        for (MessageSourceResolvable error : errors) {
            if (error.getDefaultMessage() != null) {
                return error.getDefaultMessage();
            }
        }
        return ErrorCode.VALIDATION_ERROR.getMessage();
    }

    private String findSqlState(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
            current = current.getCause();
        }
        return null;
    }
}
