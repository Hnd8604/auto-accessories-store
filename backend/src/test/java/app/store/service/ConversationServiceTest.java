package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import app.store.dto.request.CreateConversationRequest;
import app.store.entity.Conversation;
import app.store.repository.ConversationRepository;

@ExtendWith(MockitoExtension.class)
public class ConversationServiceTest {

    @Mock
    ConversationRepository conversationRepository;
    @InjectMocks
    ConversationService conversationService;

    private Conversation buildConversation() {
        return Conversation.builder()
                .id("c1")
                .guestName("Khách A")
                .channel("WEB")
                .status("OPEN")
                .unreadCount(2)
                .build();
    }

    @Test
    void create_shouldSetDefaultChannelStatusAndZeroUnread() {
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = conversationService.create(new CreateConversationRequest("Khách A"));

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo("WEB");
        assertThat(captor.getValue().getStatus()).isEqualTo("OPEN");
        assertThat(captor.getValue().getUnreadCount()).isZero();
        assertThat(response.getGuestName()).isEqualTo("Khách A");
    }

    @Test
    void getAll_shouldPageByLastMessageDesc() {
        var pageable = PageRequest.of(0, 20);

        when(conversationRepository.findAllByOrderByLastMessageAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(buildConversation())));

        var page = conversationService.getAll(0, 20);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo("c1");
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(conversationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getById("missing"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    void markAsRead_shouldResetUnreadCount() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conversation));

        conversationService.markAsRead("c1");

        assertThat(conversation.getUnreadCount()).isZero();
        verify(conversationRepository).save(conversation);
    }

    @Test
    void markAsRead_shouldDoNothing_whenConversationMissing() {
        when(conversationRepository.findById("missing")).thenReturn(Optional.empty());

        // ifPresent -> không tìm thấy thì bỏ qua, không ném lỗi
        assertThatCode(() -> conversationService.markAsRead("missing")).doesNotThrowAnyException();

        verify(conversationRepository, never()).save(any());
    }

    @Test
    void close_shouldSetStatusClosed() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conversation));

        conversationService.close("c1");

        assertThat(conversation.getStatus()).isEqualTo("CLOSED");
        verify(conversationRepository).save(conversation);
    }

    @Test
    void incrementUnread_shouldIncreaseCounter_andStampLastMessageTime() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conversation));

        LocalDateTime before = LocalDateTime.now();
        conversationService.incrementUnread("c1", "tin nhắn mới");

        assertThat(conversation.getUnreadCount()).isEqualTo(3);
        assertThat(conversation.getLastMessageAt()).isAfterOrEqualTo(before);
    }

    @Test
    void updateLastMessage_shouldStampTime_withoutTouchingUnread() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conversation));

        conversationService.updateLastMessage("c1", "admin trả lời");

        assertThat(conversation.getLastMessageAt()).isNotNull();
        assertThat(conversation.getUnreadCount()).isEqualTo(2); // giữ nguyên
    }

    @Test
    void getTotalUnread_shouldDelegateToRepository() {
        when(conversationRepository.sumTotalUnread()).thenReturn(7L);

        assertThat(conversationService.getTotalUnread()).isEqualTo(7L);
    }
}
