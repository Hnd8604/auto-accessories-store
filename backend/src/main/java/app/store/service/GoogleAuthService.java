package app.store.service;

import app.store.dto.request.GoogleAuthRequest;
import app.store.dto.response.auth.AuthenticationResponse;
import app.store.entity.Cart;
import app.store.entity.Role;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.UserMapper;
import app.store.repository.RoleRepository;
import app.store.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service xử lý đăng nhập bằng Google OAuth2.
 *
 * Flow:
 * 1. FE gửi authorization code (nhận từ Google sau khi user consent)
 * 2. Backend đổi code → Google access token
 * 3. Backend dùng access token lấy user info từ Google
 * 4. Tìm hoặc tạo user trong DB
 * 5. Generate JWT tokens và trả về FE
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GoogleAuthService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    AuthenticationService authenticationService;

    @NonFinal
    @Value("${google.client-id}")
    String clientId;

    @NonFinal
    @Value("${google.client-secret}")
    String clientSecret;

    @NonFinal
    @Value("${google.redirect-uri}")
    String redirectUri;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    /**
     * Xử lý đăng nhập Google.
     * Đổi code → token → user info → tạo/login user → trả JWT tokens.
     */
    public AuthenticationResponse authenticateWithGoogle(GoogleAuthRequest request) {
        // Step 1: Đổi authorization code → Google access token
        String googleAccessToken = exchangeCodeForToken(request.getCode());

        // Step 2: Lấy user info từ Google
        GoogleUserInfo googleUserInfo = fetchGoogleUserInfo(googleAccessToken);
        log.info("Google login: email={}, name={}", googleUserInfo.email, googleUserInfo.name);

        // Step 3: Tìm hoặc tạo user
        User user = findOrCreateUser(googleUserInfo);

        // Step 4: Generate JWT tokens
        return authenticationService.generateAuthResponse(user);
    }

    /**
     * Đổi authorization code lấy access token từ Google.
     */
    private String exchangeCodeForToken(String code) {
        RestTemplate restTemplate = new RestTemplate();// help backend call another api

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GOOGLE_TOKEN_URL, requestEntity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.getBody());// parse json to object

            String accessToken = jsonNode.get("access_token").asText();
            if (accessToken == null || accessToken.isBlank()) {
                throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
            }
            return accessToken;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to exchange Google authorization code: {}", e.getMessage());
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    /**
     * Lấy thông tin user từ Google bằng access token.
     */
    private GoogleUserInfo fetchGoogleUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USERINFO_URL, HttpMethod.GET, requestEntity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.getBody());
        
            GoogleUserInfo info = new GoogleUserInfo();
            info.id = jsonNode.get("id").asText();
            info.email = jsonNode.has("email") ? jsonNode.get("email").asText() : null;
            info.name = jsonNode.has("name") ? jsonNode.get("name").asText() : null;
            info.picture = jsonNode.has("picture") ? jsonNode.get("picture").asText() : null;

            if (info.id == null || info.id.isBlank()) {
                throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
            }
            return info;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch Google user info: {}", e.getMessage());
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    /**
     * Tìm user hiện tại hoặc tạo mới từ thông tin Google.
     * Ưu tiên: googleId → email → tạo mới
     */
    private User findOrCreateUser(GoogleUserInfo googleUserInfo) {
        // 1. Tìm theo googleId
        return userRepository.findByGoogleId(googleUserInfo.id)
                .map(user -> {
                    // Cập nhật avatar nếu thay đổi
                    if (googleUserInfo.picture != null) {
                        user.setAvatarUrl(googleUserInfo.picture);
                        userRepository.save(user);
                    }
                    return user;
                })
                .orElseGet(() -> {
                    // 2. Tìm theo email (user đã đăng ký bằng email, giờ link Google)
                    if (googleUserInfo.email != null) {
                        return userRepository.findByEmail(googleUserInfo.email)
                                .map(existingUser -> {
                                    // Link Google account vào user hiện tại
                                    existingUser.setGoogleId(googleUserInfo.id);
                                    if (googleUserInfo.picture != null) {
                                        existingUser.setAvatarUrl(googleUserInfo.picture);
                                    }
                                    log.info("Linked Google account to existing user: {}", existingUser.getUsername());
                                    return userRepository.save(existingUser);
                                })
                                .orElseGet(() -> createNewGoogleUser(googleUserInfo));
                    }
                    return createNewGoogleUser(googleUserInfo);
                });
    }

    /**
     * Tạo user mới từ thông tin Google.
     */
    private User createNewGoogleUser(GoogleUserInfo googleUserInfo) {
        // Tạo username unique từ email hoặc Google ID
        String username = generateUniqueUsername(googleUserInfo);

        Set<Role> roles = new HashSet<>();
        var roleDefault = roleRepository.findById("USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));
        roles.add(roleDefault);

        User user = User.builder()
                .username(username)
                .email(googleUserInfo.email)
                .fullName(googleUserInfo.name)
                .avatarUrl(googleUserInfo.picture)
                .googleId(googleUserInfo.id)
                .password(null) // Google users không cần password
                .roles(roles)
                .build();

        // Tạo cart
        Cart cart = new Cart();
        user.setCart(cart);
        cart.setUser(user);

        User savedUser = userRepository.save(user);
        log.info("Created new Google user: username={}, email={}", username, googleUserInfo.email);
        return savedUser;
    }

    /**
     * Tạo username duy nhất từ email Google.
     */
    private String generateUniqueUsername(GoogleUserInfo googleUserInfo) {
        // Lấy phần trước @ của email làm base
        String base = "google_user";
        if (googleUserInfo.email != null && googleUserInfo.email.contains("@")) {
            base = googleUserInfo.email.split("@")[0];
        }

        // Kiểm tra trùng, nếu trùng thêm suffix ngẫu nhiên
        String username = base;
        if (userRepository.existsByUsername(username)) {
            username = base + "_" + UUID.randomUUID().toString().substring(0, 6);
        }
        return username;
    }

    /**
     * Inner class chứa thông tin user từ Google.
     */
    private static class GoogleUserInfo {
        String id;
        String email;
        String name;
        String picture;
    }
}
