package app.store.service;

import app.store.dto.request.ChangePasswordRequest;
import app.store.dto.request.auth.AuthenticationRequest;
import app.store.dto.request.auth.IntrospectRequest;
import app.store.dto.request.auth.LogoutRequest;
import app.store.dto.request.auth.RefreshRequest;
import app.store.dto.request.user.UserCreationRequest;
import app.store.dto.response.auth.AuthenticationResponse;
import app.store.dto.response.auth.IntrospectResponse;
import app.store.dto.response.auth.RefreshResponse;
import app.store.dto.response.user.UserResponse;
import app.store.entity.Cart;
import app.store.entity.InvalidatedToken;
import app.store.entity.Role;
import app.store.entity.User;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.UserMapper;
import app.store.repository.InvalidatedRepository;
import app.store.repository.RoleRepository;
import app.store.repository.UserRepository;
import app.store.utils.SecurityUtils;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;

import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedRepository invalidatedRepository;
    UserMapper userMapper;
    CartSyncService cartSyncService;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
    RoleRepository roleRepository;
    @NonFinal // Don't inject this field to constructor
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.access-duration}")
    protected long ACCESS_DURATION;

    @NonFinal
    @Value("${jwt.refresh-duration}")
    protected long REFRESH_DURATION;

    public UserResponse register(UserCreationRequest request) {
        User user = userMapper.toUser(request);

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }
        user.setPassword(passwordEncoder.encode(request.password()));
        Set<Role> roles = new HashSet<>();
        var roleDefault = roleRepository.findById("USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));
        roles.add(roleDefault);
        user.setRoles(roles);

        // create cart when creating user
        Cart cart = new Cart();
        user.setCart(cart);
        cart.setUser(user);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.token();
        boolean isValid = true;
        try {
            verifyToken(token);
        } catch (AppException e) {
            isValid = false;
            System.out.println(e.getMessage());
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request, HttpSession session) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.password(), user.getPassword());

        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var accessToken = generateAccessToken(user);
        var refreshToken = generateRefreshToken(user);

        // Sync cart from session to user cart
        cartSyncService.syncSessionCart(user, session);

        return AuthenticationResponse.builder()
                .user(userMapper.toUserResponse(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    /**
     * Generate JWT tokens cho user (dùng chung cho login thường và Google login).
     */
    public AuthenticationResponse generateAuthResponse(User user) {
        var accessToken = generateAccessToken(user);
        var refreshToken = generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .user(userMapper.toUserResponse(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    /**
     * Đổi mật khẩu cho user đang đăng nhập.
     * Kiểm tra: mật khẩu cũ đúng, mật khẩu mới không trùng cũ, confirm khớp.
     */
    public void changePassword(ChangePasswordRequest request) {
        String userId = SecurityUtils.currentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_CURRENT_PASSWORD);
        }

        // Kiểm tra mật khẩu mới != mật khẩu cũ
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_CURRENT);
        }

        // Kiểm tra xác nhận mật khẩu
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }

        // Lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for userId: {}", userId);
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        invalidateToken(request.accessToken(), "accessToken");
        invalidateToken(request.refreshToken(), "refreshToken");
    }

    private void invalidateToken(String token, String type) throws ParseException, JOSEException {
        try {
            SignedJWT signToken = verifyToken(token);
            String jti = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jti)
                    .expiryTime(expiryTime)
                    .type(type) // add type to the token
                    .build();

            invalidatedRepository.save(invalidatedToken); // save logout token to database
        } catch (AppException e) {
            log.info("Token already expired or invalidated");
        }
    }

    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verifiered = signedJWT.verify(verifier);

        if (!(verifiered && expirationTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        return signedJWT;
    }

    public RefreshResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signJWT = verifyToken(request.refreshToken());

        var jti = signJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signJWT.getJWTClaimsSet().getExpirationTime();
        var type = signJWT.getJWTClaimsSet().getClaim("type");

        var userId = signJWT.getJWTClaimsSet().getSubject();

        if (!"refreshToken".equals(type)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var newToken = generateAccessToken(user);
        return RefreshResponse.builder()
                .accessToken(newToken)
                .authenticated(true)
                .build();
    }

    private String generateAccessToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                // sub = user.id (UUID) — bất biến và không bao giờ cấp lại, khác với
                // username (đổi được, và dùng lại được sau khi user bị xoá).
                .subject(user.getId())
                .issuer("app.store")
                .issueTime(new Date()) // Current time
                .expirationTime(new Date(Instant.now().plus(ACCESS_DURATION, ChronoUnit.SECONDS).toEpochMilli())) // Token
                                                                                                                  // expiration
                                                                                                                  // time
                                                                                                                  // (1
                                                                                                                  // hour
                                                                                                                  // from
                                                                                                                  // now)
                .jwtID(UUID.randomUUID().toString()) // add UUID to the token
                .claim("scope", buildScope(user))
                .claim("type", "accessToken") // add type to the token
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload); // jwsObject include header and payload

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize(); // Serialize the JWS object to a compact string representation
        } catch (JOSEException e) {
            log.error("Error generating access token", e);
            throw new RuntimeException(e);
        }
    }

    private String generateRefreshToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId()) // cùng lý do như access token
                .issuer("app.store")
                .issueTime(new Date()) // Current time
                .expirationTime(new Date(Instant.now().plus(REFRESH_DURATION, ChronoUnit.SECONDS).toEpochMilli())) // Token
                                                                                                                   // expiration
                                                                                                                   // time
                                                                                                                   // (1
                                                                                                                   // hour
                                                                                                                   // from
                                                                                                                   // now)
                .jwtID(UUID.randomUUID().toString()) // add UUID to the token
                .claim("type", "refreshToken") // add type to the token
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload); // jwsObject include header and payload

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize(); // Serialize the JWS object to a compact string representation
        } catch (JOSEException e) {
            log.error("Error generating refresh token", e);
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
            });
        }
        return stringJoiner.toString();
    }
}
