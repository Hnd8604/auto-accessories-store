package app.store.dto.request;

import java.util.List;
import lombok.Builder;

@Builder
public record CategoryBrandsRequest(
        List<Long> brandIds
) {
}
