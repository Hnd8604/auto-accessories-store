package app.store.dto.request.auth;

import lombok.Builder;

@Builder
public record AuthenticationRequest(
        String email,
        String password
) {
}
