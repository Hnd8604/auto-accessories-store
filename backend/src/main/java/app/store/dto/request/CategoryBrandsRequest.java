package app.store.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record CategoryBrandsRequest(
        @NotNull(message = "Brand IDs are required")
        List<@NotNull(message = "Brand ID must not be null") Long> brandIds
) {
}
