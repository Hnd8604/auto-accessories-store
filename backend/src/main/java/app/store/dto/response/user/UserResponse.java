package app.store.dto.response.user;

import app.store.dto.response.RoleResponse;

import java.util.Set;
import lombok.Builder;

@Builder
public record UserResponse(
        String id,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        String avatarUrl,
        Set<RoleResponse> roles
) {
}
