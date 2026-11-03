package app.store.dto.response.auth;

import app.store.dto.response.user.UserResponse;
import lombok.Builder;

@Builder
public record AuthenticationResponse(
        UserResponse user,
        String accessToken,
        String refreshToken,
        boolean authenticated
) {
}
