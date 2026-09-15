package app.store.dto.request.auth;

import lombok.Builder;

@Builder
public record LogoutRequest(
        String accessToken,
        String refreshToken
) {
}
