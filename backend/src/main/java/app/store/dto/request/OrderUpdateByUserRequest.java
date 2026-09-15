package app.store.dto.request;

import lombok.Builder;

@Builder
public record OrderUpdateByUserRequest(
        String nameRecipient,
        String phoneRecipient,
        String addressRecipient,
        String note
) {
}
