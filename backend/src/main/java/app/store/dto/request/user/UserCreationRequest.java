package app.store.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserCreationRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20, message = "Username must be between {min} and {max} characters")
        String username,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least {min} characters")
        String password,
        @Email(message = "Email is invalid")
        String email,
        @Size(max = 255, message = "Full name must not exceed {max} characters")
        String fullName,
        @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Phone number is invalid")
        String phoneNumber
) {
}
