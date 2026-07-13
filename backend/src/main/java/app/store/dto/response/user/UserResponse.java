package app.store.dto.response.user;


import app.store.dto.response.RoleResponse;
import app.store.entity.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String username;
    String email;
    String fullName;
    String phoneNumber;
    String avatarUrl;
    Set<RoleResponse> roles;
}
