package app.store.dto.response.auth;

import lombok.Builder;

@Builder
public record RefreshResponse(
        String accessToken,
        boolean authenticated
) {
}
