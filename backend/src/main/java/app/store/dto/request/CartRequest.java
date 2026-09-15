package app.store.dto.request;

import lombok.Builder;

@Builder
public record CartRequest(
        String userId
) {
}
