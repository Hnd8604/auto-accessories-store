package app.store.controller;

import app.store.dto.request.CreateConversationRequest;
import app.store.dto.response.ChatMessageResponse;
import app.store.dto.response.ConversationResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.service.ChatMessageService;
import app.store.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Live Chat", description = "APIs for live chat between customers and admin")
public class ConversationController {

    ConversationService conversationService;
    ChatMessageService chatMessageService;

    @PostMapping
    @Operation(summary = "Create a new conversation (guest)")
    public ApiResponse<ConversationResponse> create(@Valid @RequestBody CreateConversationRequest request) {
        return ApiResponse.<ConversationResponse>builder()
                .result(conversationService.create(request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all conversations (admin)")
    public ApiResponse<Page<ConversationResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<Page<ConversationResponse>>builder()
                .result(conversationService.getAll(page, size))
                .build();
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get messages of a conversation")
    public ApiResponse<Page<ChatMessageResponse>> getMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ApiResponse.<Page<ChatMessageResponse>>builder()
                .result(chatMessageService.getMessages(id, page, size))
                .build();
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark conversation as read (admin)")
    public ApiResponse<Void> markAsRead(@PathVariable String id) {
        conversationService.markAsRead(id);
        return ApiResponse.<Void>builder().build();
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Close a conversation (admin)")
    public ApiResponse<Void> close(@PathVariable String id) {
        conversationService.close(id);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get total unread count across all conversations (admin)")
    public ApiResponse<Long> totalUnread() {
        return ApiResponse.<Long>builder()
                .result(conversationService.getTotalUnread())
                .build();
    }
}
