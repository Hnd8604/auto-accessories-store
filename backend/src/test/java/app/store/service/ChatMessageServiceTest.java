package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import app.store.dto.request.SendChatMessageRequest;
import app.store.dto.response.ChatMessageResponse;
import app.store.entity.ChatMessage;
import app.store.repository.ChatMessageRepository;

@ExtendWith(MockitoExtension.class)
public class ChatMessageServiceTest {

    @Mock
    ChatMessageRepository chatMessageRepository;
    @Mock
    ConversationService conversationService;
    @Mock
    SimpMessagingTemplate messagingTemplate;
    @InjectMocks
    ChatMessageService chatMessageService;

    private SendChatMessageRequest request(String senderType) {
        return new SendChatMessageRequest("c1", "Xin chào", senderType);
    }

    @Test
    void send_fromCustomer_shouldBroadcast_andIncrementUnreadForAdmin() {
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = chatMessageService.send(request("CUSTOMER"));

        assertThat(response.getContent()).isEqualTo("Xin chào");
        assertThat(response.getSenderType()).isEqualTo("CUSTOMER");

        verify(messagingTemplate).convertAndSend(eq("/topic/conversation/c1"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/admin/new-message"), any(Object.class));
        verify(conversationService).incrementUnread("c1", "Xin chào");
        verify(conversationService, never()).updateLastMessage(any(), any());
    }

    @Test
    void send_fromAdmin_shouldOnlyUpdateLastMessage() {
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        chatMessageService.send(request("ADMIN"));

        verify(messagingTemplate).convertAndSend(eq("/topic/conversation/c1"), any(Object.class));
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/admin/new-message"), any(Object.class));
        verify(conversationService).updateLastMessage("c1", "Xin chào");
        verify(conversationService, never()).incrementUnread(any(), any());
    }

    @Test
    void getMessages_shouldPageInAscendingOrder() {
        ChatMessage message = ChatMessage.builder()
                .id("m1").conversationId("c1").senderType("ADMIN").content("Chào bạn").build();

        when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc("c1", PageRequest.of(0, 50)))
                .thenReturn(new PageImpl<>(List.of(message)));

        var page = chatMessageService.getMessages("c1", 0, 50);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Chào bạn");
    }
}
