package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.request.ChangePasswordRequest;
import app.store.dto.request.ConfirmResetPasswordRequest;
import app.store.dto.request.GoogleAuthRequest;
import app.store.dto.request.InitResetPasswordRequest;
import app.store.dto.request.ResendOtpRequest;
import app.store.dto.request.VerifyOtpRequest;
import app.store.dto.request.auth.AuthenticationRequest;
import app.store.dto.request.auth.IntrospectRequest;
import app.store.dto.request.auth.LogoutRequest;
import app.store.dto.request.auth.RefreshRequest;
import app.store.dto.request.user.UserCreationRequest;
import app.store.dto.response.InitResetPasswordResponse;
import app.store.dto.response.ResendOtpResponse;
import app.store.dto.response.VerifyOtpResponse;
import app.store.dto.response.auth.ApiResponse;
import app.store.dto.response.auth.AuthenticationResponse;
import app.store.dto.response.auth.IntrospectResponse;
import app.store.dto.response.auth.RefreshResponse;
import app.store.dto.response.user.UserResponse;
import app.store.service.AuthenticationService;
import app.store.service.GoogleAuthService;
import app.store.service.ResetPasswordService;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Authentication", description = "APIs for user authentication including login, token refresh, logout and password reset")
public class AuthenticationController {
    AuthenticationService AuthenticationService;
    GoogleAuthService googleAuthService;
    ResetPasswordService resetPasswordService;
    
    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates user with email and password. Returns JWT access token and refresh token."
    )
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request, HttpSession session) {
        var result = AuthenticationService.authenticate(request, session);

        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .message(ResponseMessage.AUTHENTICATE_SUCCESS)
                .build();

    }
    
    @PostMapping("/google")
    @Operation(
        summary = "Login with Google",
        description = "Authenticates user with Google OAuth2 authorization code. Creates new account if user doesn't exist."
    )
    ApiResponse<AuthenticationResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        var result = googleAuthService.authenticateWithGoogle(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .message(ResponseMessage.AUTHENTICATE_SUCCESS)
                .build();
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Registers a new user with the provided details. Returns the created user information."
    )
    ApiResponse<UserResponse> register(@Valid @RequestBody UserCreationRequest request){
        var result = AuthenticationService.register(request);
        return ApiResponse.<UserResponse>builder()
                .result(result)
                .message(ResponseMessage.REGISTER_SUCCESS)
                .build();

    }
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh token",
        description = "Refreshes the access token using a valid refresh token. Returns new access token."
    )
    ApiResponse<RefreshResponse> refresh(@RequestBody RefreshRequest request) throws ParseException, JOSEException {
        var result = AuthenticationService.refreshToken(request);
        return ApiResponse.<RefreshResponse>builder()
                .result(result)
                .message(ResponseMessage.AUTHENTICATE_SUCCESS)
                .build();

    }

    @PostMapping("/introspect")
    @Operation(
        summary = "Introspect token",
        description = "Validates a JWT token and returns its status. Used to check if a token is valid and not expired."
    )
    ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {

        var result = AuthenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .message(ResponseMessage.INTROSPECT_SUCCESS)
                .build();

    }


    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Logs out user by invalidating the JWT token. Adds token to blacklist to prevent reuse."
    )
    ApiResponse<Void> logout(@RequestBody LogoutRequest request)
            throws ParseException, JOSEException {

        AuthenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.LOGOUT_SUCCESS)
                .build();
    }
    
    // ==================== Change Password ====================
    
    @PutMapping("/password/change")
    @Operation(
            summary = "Change password",
            description = "Changes the password for the currently authenticated user. Requires current password verification."
    )
    ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        AuthenticationService.changePassword(request);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.CHANGE_PASSWORD_SUCCESS)
                .build();
    }
    
    // ==================== 3-Step Password Reset Flow ====================
    
    @PostMapping("/password/reset/init")
    @Operation(
            summary = "Step 1: Initiate password reset",
            description = "Initiates the password reset flow. Verifies email exists, generates OTP, and sends it to the user's email. Returns a unique sessionId for the entire flow."
    )
    ApiResponse<InitResetPasswordResponse> initResetPassword(@RequestBody InitResetPasswordRequest request) {
        var result = resetPasswordService.initResetPassword(request);
        return ApiResponse.<InitResetPasswordResponse>builder()
                .result(result)
                .message(ResponseMessage.INIT_RESET_PASSWORD_SUCCESS)
                .build();
    }
    
    @PostMapping("/password/reset/verify-otp")
    @Operation(
            summary = "Step 2: Verify OTP",
            description = "Verifies the OTP sent to user's email. Requires the sessionId from step 1. Max 5 attempts allowed. OTP expires in 3 minutes."
    )
    ApiResponse<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        var result = resetPasswordService.verifyOtp(request);
        return ApiResponse.<VerifyOtpResponse>builder()
                .result(result)
                .message(ResponseMessage.VERIFY_OTP_SUCCESS)
                .build();
    }
    
    @PostMapping("/password/reset/confirm")
    @Operation(
            summary = "Step 3: Confirm password reset",
            description = "Confirms the password reset with a new password. Requires sessionId and OTP must be verified in step 2."
    )
    ApiResponse<Void> confirmResetPassword(@Valid @RequestBody ConfirmResetPasswordRequest request) {
        resetPasswordService.confirmResetPassword(request);
        return ApiResponse.<Void>builder()
                .message(ResponseMessage.CONFIRM_RESET_PASSWORD_SUCCESS)
                .build();
    }
    
    @PostMapping("/password/reset/resend-otp")
    @Operation(
            summary = "Resend OTP",
            description = "Resends OTP to user's email. Rate limited to 1 request per minute. Requires valid sessionId from step 1."
    )
    ApiResponse<ResendOtpResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        var result = resetPasswordService.resendOtp(request);
        return ApiResponse.<ResendOtpResponse>builder()
                .result(result)
                .message(ResponseMessage.RESEND_OTP_SUCCESS)
                .build();
    }
}
