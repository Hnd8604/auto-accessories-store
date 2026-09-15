package app.store.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record UserUpdateRequest(
        // max theo cột DB: username sinh từ email Google có thể dài hơn 20 ký tự
        @Size(min = 3, max = 255, message = "Username must be between {min} and {max} characters")
        String username,
        @Size(min = 8, message = "Password must be at least {min} characters")
        String password,
        @Email(message = "Email is invalid")
        String email,
        @Size(max = 255, message = "Full name must not exceed {max} characters")
        String fullName,
        @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Phone number is invalid")
        String phoneNumber,
        List<String> roles
) {
}
