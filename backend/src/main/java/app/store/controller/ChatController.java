package app.store.controller;

import java.security.Principal;

import app.store.config.StompAuthChannelInterceptor;
import app.store.dto.request.SendChatMessageRequest;
import app.store.dto.response.ChatMessageResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.enums.SenderType;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.ObjectError;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatController {

    static final String ERROR_DESTINATION = "/queue/errors";

    ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public ChatMessageResponse sendMessage(@Valid @Payload SendChatMessageRequest request, Principal principal) {
        SenderType senderType = StompAuthChannelInterceptor.isAdmin(principal) ? SenderType.ADMIN : SenderType.CUSTOMER;
        return chatMessageService.send(request, senderType);
    }

    @MessageExceptionHandler(AppException.class)
    @SendToUser(destinations = ERROR_DESTINATION, broadcast = false)
    public ApiResponse<Void> handleAppException(AppException exception) {
        return ApiResponse.<Void>builder()
                .code(exception.getErrorCode().getCode())
                .message(exception.getMessage())
                .build();
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(destinations = ERROR_DESTINATION, broadcast = false)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        String message = exception.getBindingResult() == null
                ? errorCode.getMessage()
                : exception.getBindingResult().getAllErrors().stream()
                        .findFirst()
                        .map(ObjectError::getDefaultMessage)
                        .orElse(errorCode.getMessage());

        return ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(message)
                .build();
    }
}
