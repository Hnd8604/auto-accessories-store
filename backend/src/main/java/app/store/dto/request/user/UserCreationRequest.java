package app.store.dto.request.user;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserCreationRequest(
        @Size(min = 3, max = 20, message = "USERNAME_INVALID")
        String username,
        @Size(min = 8, message = "PASSWORD_INVALID")
        String password,
        String email,
        String fullName,
        String phoneNumber
) {
}
