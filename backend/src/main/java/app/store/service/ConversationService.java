package app.store.service;

import app.store.dto.request.CreateConversationRequest;
import app.store.dto.response.ConversationResponse;
import app.store.entity.Conversation;
import app.store.repository.ConversationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {

    ConversationRepository conversationRepository;

    @Transactional
    public ConversationResponse create(CreateConversationRequest request) {
        Conversation conversation = Conversation.builder()
                .guestName(request.getGuestName())
                .channel("WEB")
                .status("OPEN")
                .unreadCount(0)
                .build();
        conversation = conversationRepository.save(conversation);
        return toResponse(conversation, null);
    }

    public Page<ConversationResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return conversationRepository.findAllByOrderByLastMessageAtDesc(pageable)
                .map(c -> toResponse(c, null));
    }

    public ConversationResponse getById(String id) {
        Conversation c = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + id));
        return toResponse(c, null);
    }

    @Transactional
    public void markAsRead(String id) {
        conversationRepository.findById(id).ifPresent(c -> {
            c.setUnreadCount(0);
            conversationRepository.save(c);
        });
    }

    @Transactional
    public void close(String id) {
        conversationRepository.findById(id).ifPresent(c -> {
            c.setStatus("CLOSED");
            conversationRepository.save(c);
        });
    }

    @Transactional
    public void incrementUnread(String id, String lastMessage) {
        conversationRepository.findById(id).ifPresent(c -> {
            c.setUnreadCount(c.getUnreadCount() + 1);
            c.setLastMessageAt(java.time.LocalDateTime.now());
            conversationRepository.save(c);
        });
    }

    @Transactional
    public void updateLastMessage(String id, String lastMessage) {
        conversationRepository.findById(id).ifPresent(c -> {
            c.setLastMessageAt(java.time.LocalDateTime.now());
            conversationRepository.save(c);
        });
    }

    public long getTotalUnread() {
        return conversationRepository.sumTotalUnread();
    }

    private ConversationResponse toResponse(Conversation c, String lastMessage) {
        return ConversationResponse.builder()
                .id(c.getId())
                .guestName(c.getGuestName())
                .channel(c.getChannel())
                .status(c.getStatus())
                .unreadCount(c.getUnreadCount())
                .lastMessageAt(c.getLastMessageAt())
                .createdAt(c.getCreatedAt())
                .lastMessage(lastMessage)
                .build();
    }
}
