package app.store.service;

import app.store.dto.request.SendChatMessageRequest;
import app.store.dto.response.ChatMessageResponse;
import app.store.entity.ChatMessage;
import app.store.enums.SenderType;
import app.store.repository.ChatMessageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageService {

    ChatMessageRepository chatMessageRepository;
    ConversationService conversationService;
    SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessageResponse send(SendChatMessageRequest request, SenderType senderType) {
        conversationService.getOpenConversation(request.conversationId());

        ChatMessage message = ChatMessage.builder()
                .conversationId(request.conversationId())
                .senderType(senderType.name())
                .content(request.content())
                .build();
        message = chatMessageRepository.save(message);

        ChatMessageResponse response = toResponse(message);

        // Broadcast to conversation topic
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + request.conversationId(),
                response
        );

        // If message from customer, increment unread for admin
        if (senderType == SenderType.CUSTOMER) {
            conversationService.incrementUnread(request.conversationId(), request.content());
            // Notify admin panel of new message
            messagingTemplate.convertAndSend("/topic/admin/new-message", response);
        } else {
            conversationService.updateLastMessage(request.conversationId(), request.content());
        }

        return response;
    }

    public Page<ChatMessageResponse> getMessages(String conversationId, int page, int size) {
        return chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    private ChatMessageResponse toResponse(ChatMessage m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderType(m.getSenderType())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
