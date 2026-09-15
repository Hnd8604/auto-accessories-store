package app.store.dto.request;

import lombok.Builder;

@Builder
public record ServiceRequest(
        String name,
        String shortDescription,
        String fullDescription,
        Integer displayOrder
) {
}
