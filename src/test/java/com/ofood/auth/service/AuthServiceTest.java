package com.ofood.auth.service;

import com.ofood.auth.config.AuthCookieProperties;
import com.ofood.auth.config.AuthLockoutProperties;
import com.ofood.auth.dto.ChangePasswordRequest;
import com.ofood.auth.dto.LoginRequest;
import com.ofood.auth.dto.RegisterRequest;
import com.ofood.auth.exception.DuplicateEmailException;
import com.ofood.auth.model.RefreshToken;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.RefreshTokenRepository;
import com.ofood.auth.repository.UserRepository;
import com.ofood.auth.repository.PasswordResetTokenRepository;
import com.ofood.role.Role;
import com.ofood.role.repository.RoleRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.ofood.security.jwt.JwtTokenService;
import com.ofood.security.jwt.RsaKeyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private AuthCookieProperties cookieProperties;
    private RsaKeyProperties jwtProperties;

    @BeforeEach
    void setUp() {
        cookieProperties = new AuthCookieProperties();
        cookieProperties.setMaxAge(Duration.ofDays(30));
        jwtProperties = new RsaKeyProperties();
        jwtProperties.setAccessTokenTtl(Duration.ofMinutes(10));
        AuthLockoutProperties lockoutProperties = new AuthLockoutProperties();
        authService = new AuthService(userRepository, roleRepository, refreshTokenRepository, passwordResetTokenRepository, passwordEncoder,
                authenticationManager, jwtTokenService, cookieProperties, jwtProperties, lockoutProperties, eventPublisher);
    }

    @Test
    void registerShouldCreateActiveCustomerUser() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("Password@123");
        request.setFullName("Keshav Choudhary");

        Role customerRole = new Role();
        customerRole.setName("ROLE_CUSTOMER");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        var response = authService.register(request);

        assertThat(response.userId()).isNotNull();
        assertThat(response.message()).isEqualTo("User registered successfully");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getRoles()).extracting(Role::getName).contains("ROLE_CUSTOMER");
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("Password@123");
        request.setFullName("Keshav Choudhary");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void loginShouldGenerateJwtAndPersistHashedRefreshToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Password@123");
        request.setDeviceInfo(com.fasterxml.jackson.databind.node.TextNode.valueOf("mobile"));

        Authentication authentication = new UsernamePasswordAuthenticationToken("user@example.com", "Password@123");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("encoded-password");
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        Role role = new Role();
        role.setName("ROLE_CUSTOMER");
        user.setRoles(Set.of(role));

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken refreshToken = invocation.getArgument(0);
            refreshToken.setId(UUID.randomUUID());
            return refreshToken;
        });
        when(jwtTokenService.generateAccessToken(any(UUID.class), any(UUID.class), any())).thenReturn("access-token");

        var loginResult = authService.login(request);

        assertThat(loginResult.tokenResponse().accessToken()).isEqualTo("access-token");
        assertThat(loginResult.refreshToken()).isNotBlank();
        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken persisted = refreshTokenCaptor.getValue();
        assertThat(persisted.getTokenHash()).isNotEqualTo(loginResult.refreshToken());
        assertThat(persisted.getExpiresAt()).isAfter(Instant.now());
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void changePasswordShouldUpdateStoredHash() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("old-hash");
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Password@123");
        request.setNewPassword("NewPassword@123");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.changePassword(request, "user@example.com");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }
}
