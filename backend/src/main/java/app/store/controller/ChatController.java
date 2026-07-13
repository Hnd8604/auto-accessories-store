package app.store.controller;

import app.store.dto.request.SendChatMessageRequest;
import app.store.dto.response.ChatMessageResponse;
import app.store.service.ChatMessageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatController {

    ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public ChatMessageResponse sendMessage(SendChatMessageRequest request) {
        return chatMessageService.send(request);
    }
}
