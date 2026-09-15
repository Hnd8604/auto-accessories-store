package app.store.dto.request.user;

import java.util.List;
import lombok.Builder;

@Builder
public record UserUpdateRequest(
        String username,
        String password,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        List<String> roles
) {
}
