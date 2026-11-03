package app.store.dto.response;

import lombok.Builder;

@Builder
public record ImageUploadResponse(
        String imageUrl
) {
}
