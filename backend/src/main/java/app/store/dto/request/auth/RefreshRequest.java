package app.store.dto.request.auth;

import lombok.Builder;

@Builder
public record RefreshRequest(
        String refreshToken
) {
}
