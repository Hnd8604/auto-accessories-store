package app.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendChatMessageRequest {

    @NotBlank
    String conversationId;

    @NotBlank
    String content;

    @NotBlank
    String senderType; // CUSTOMER | ADMIN
}
