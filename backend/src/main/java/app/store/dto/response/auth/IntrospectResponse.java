package app.store.dto.response.auth;

import lombok.Builder;

@Builder
public record IntrospectResponse(
        boolean valid
) {
}
