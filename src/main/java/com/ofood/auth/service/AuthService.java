package com.ofood.auth.service;

import com.ofood.auth.config.AuthCookieProperties;
import com.ofood.auth.config.AuthLockoutProperties;
import com.ofood.auth.dto.AuthTokenResponse;
import com.ofood.auth.dto.ChangePasswordRequest;
import com.ofood.auth.dto.LoginRequest;
import com.ofood.auth.dto.LoginResult;
import com.ofood.auth.dto.RegisterRequest;
import com.ofood.auth.dto.RegistrationResponse;
import com.ofood.auth.exception.DuplicateEmailException;
import com.ofood.auth.exception.InvalidRefreshTokenException;
import com.ofood.auth.model.PasswordResetToken;
import com.ofood.auth.model.RefreshToken;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.PasswordResetTokenRepository;
import com.ofood.auth.repository.RefreshTokenRepository;
import com.ofood.auth.repository.UserRepository;
import com.ofood.role.Role;
import com.ofood.role.repository.RoleRepository;
import com.ofood.security.jwt.JwtTokenService;
import com.ofood.security.jwt.RsaKeyProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ofood.notification.event.PasswordResetRequestedEvent;
import com.ofood.notification.event.UserRegisteredEvent;
import com.ofood.auth.dto.ForgotPasswordRequest;
import com.ofood.auth.dto.ResetPasswordRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final AuthCookieProperties cookieProperties;
    private final RsaKeyProperties jwtProperties;
    private final AuthLockoutProperties lockoutProperties;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenService jwtTokenService,
                       AuthCookieProperties cookieProperties,
                       RsaKeyProperties jwtProperties,
                       AuthLockoutProperties lockoutProperties,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.cookieProperties = cookieProperties;
        this.jwtProperties = jwtProperties;
        this.lockoutProperties = lockoutProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException("Email already registered");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            String fName = request.getFirstName() != null ? request.getFirstName().trim() : "";
            String lName = request.getLastName() != null ? request.getLastName().trim() : "";
            user.setFullName((fName + " " + lName).trim());
        } else {
            user.setFullName(request.getFullName().trim());
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMobile(request.getMobile());
        user.setStatus(ACTIVE_STATUS);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        Role customerRole = roleRepository.findByName(CUSTOMER_ROLE)
                .orElseThrow(() -> new IllegalArgumentException("Required role not found: ROLE_CUSTOMER"));
        user.getRoles().add(customerRole);

        user = userRepository.save(user);
        
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail(), user.getFirstName()));
        
        return new RegistrationResponse(user.getId(), "User registered successfully");
    }

    @Transactional(noRollbackFor = org.springframework.security.core.AuthenticationException.class)
    public LoginResult login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            if (user != null) {
                // If it's a LockedException, we don't increment because they're already locked
                if (!(ex instanceof org.springframework.security.authentication.LockedException)) {
                    if (user.getLockUntil() != null && user.getLockUntil().isBefore(Instant.now())) {
                        user.setFailedLogins(0);
                        user.setLockUntil(null);
                    }
                    int newFailedCount = user.getFailedLogins() + 1;
                    user.setFailedLogins(newFailedCount);
                    if (newFailedCount >= lockoutProperties.getMaxFailedAttempts()) {
                        user.setLockUntil(Instant.now().plus(lockoutProperties.getLockDuration()));
                    }
                    userRepository.save(user);
                }
            }
            throw ex;
        }

        if (user == null) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid email or password");
        }

        Instant now = Instant.now();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        user.setFailedLogins(0);
        user.setLockUntil(null);
        userRepository.save(user);

        RefreshTokenBundle tokenBundle = createRefreshToken(user, request.getDeviceInfo());
        refreshTokenRepository.save(tokenBundle.refreshToken());

        UUID sid = tokenBundle.refreshToken().getId();
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String accessToken = jwtTokenService.generateAccessToken(user.getId(), sid, roleNames);

        return new LoginResult(
                new AuthTokenResponse(accessToken, "Bearer", jwtProperties.getAccessTokenTtl().getSeconds(), user.getId()),
                tokenBundle.rawToken()
        );
    }

    @Transactional
    public LoginResult refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }

        String tokenHash = hashToken(refreshTokenValue);
        Optional<RefreshToken> maybeToken = refreshTokenRepository.findByTokenHash(tokenHash);
        RefreshToken refreshToken = maybeToken.orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid"));

        if (refreshToken.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        // Token rotation: revoke old, create new
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new InvalidRefreshTokenException("User is inactive or deleted");
        }
        RefreshTokenBundle tokenBundle = createRefreshToken(user, refreshToken.getDeviceInfo());
        refreshTokenRepository.save(tokenBundle.refreshToken());

        UUID sid = tokenBundle.refreshToken().getId();
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String accessToken = jwtTokenService.generateAccessToken(user.getId(), sid, roleNames);

        return new LoginResult(
                new AuthTokenResponse(accessToken, "Bearer", jwtProperties.getAccessTokenTtl().getSeconds(), user.getId()),
                tokenBundle.rawToken()
        );
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        String tokenHash = hashToken(refreshTokenValue);
        Optional<RefreshToken> maybeToken = refreshTokenRepository.findByTokenHash(tokenHash);
        if (maybeToken.isPresent()) {
            RefreshToken token = maybeToken.get();
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, String principal) {
        User user = findUserByPrincipal(principal);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUser(user, Instant.now());
    }
    
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Optional<User> maybeUser = userRepository.findByEmail(normalizedEmail);
        
        if (maybeUser.isEmpty() || !ACTIVE_STATUS.equals(maybeUser.get().getStatus())) {
            // Protect against account enumeration
            return;
        }
        
        User user = maybeUser.get();
        Instant now = Instant.now();
        
        passwordResetTokenRepository.invalidateAllActiveTokensForUser(user, now);
        
        String rawToken = generateOpaqueRefreshToken();
        String tokenHash = hashToken(rawToken);
        
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setCreatedAt(now);
        resetToken.setExpiresAt(now.plus(Duration.ofMinutes(15))); // 15 min expiry
        passwordResetTokenRepository.save(resetToken);
        
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), rawToken));
    }
    
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
                
        Instant now = Instant.now();
        
        if (resetToken.getUsedAt() != null || resetToken.getInvalidatedAt() != null) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        
        if (resetToken.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        
        int updated = passwordResetTokenRepository.consumeTokenAtomic(tokenHash, now);
        if (updated == 0) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        
        User user = resetToken.getUser();
        if (user == null || !ACTIVE_STATUS.equals(user.getStatus())) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(now);
        userRepository.save(user);
        
        refreshTokenRepository.revokeAllByUser(user, now);
    }

    @Transactional(readOnly = true)
    public com.ofood.auth.dto.UserDto getMe(String principal) {
        User user = findUserByPrincipal(principal);
        com.ofood.auth.dto.UserDto dto = new com.ofood.auth.dto.UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFullName(user.getFullName());
        dto.setMobile(user.getMobile());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setIsActive("ACTIVE".equals(user.getStatus()));
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setRoles(user.getRoles().stream().map(Role::getName).toList());
        return dto;
    }

    public AuthCookieProperties getCookieProperties() {
        return cookieProperties;
    }

    private RefreshTokenBundle createRefreshToken(User user, com.fasterxml.jackson.databind.JsonNode deviceInfo) {
        String rawToken = generateOpaqueRefreshToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        Instant now = Instant.now();
        token.setIssuedAt(now);
        token.setLastUsedAt(now);
        token.setExpiresAt(now.plus(cookieProperties.getMaxAge()));
        token.setDeviceInfo(deviceInfo);
        return new RefreshTokenBundle(token, rawToken);
    }

    private String generateOpaqueRefreshToken() {
        byte[] randomBytes = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private User findUserByPrincipal(String principal) {
        try {
            return userRepository.findById(UUID.fromString(principal))
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        } catch (IllegalArgumentException ex) {
            if (!"Invalid UUID string".equals(ex.getMessage()) && !ex.getMessage().startsWith("Invalid UUID string:")) {
                throw ex;
            }
            return userRepository.findByEmail(normalizeEmail(principal))
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
    }

    private record RefreshTokenBundle(RefreshToken refreshToken, String rawToken) {
    }
}
