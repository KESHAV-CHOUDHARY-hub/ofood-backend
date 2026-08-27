package com.ofood.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.auth.model.PasswordResetToken;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.PasswordResetTokenRepository;
import com.ofood.auth.repository.UserRepository;
import com.ofood.role.Role;
import com.ofood.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PasswordResetIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ofood")
            .withUsername("ofood")
            .withPassword("ofood");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("ofood.security.jwt.issuer", () -> "http://localhost:8080");
        registry.add("ofood.security.jwt.audience", () -> "ofood-api");
        registry.add("ofood.security.jwt.key-id", () -> "test-kid");
        registry.add("ofood.security.jwt.private-key-path",
                () -> "file:" + System.getProperty("user.dir") + "/src/test/resources/keys/test_private.pem");
        registry.add("ofood.auth.cookie.secure", () -> false);
        // Turn off real mail attempts by not providing a host, though it might still try to connect. 
        // We will just let the try/catch in SmtpEmailSender swallow the error as designed.
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void forgotPasswordReturnsGenericSuccessForExistingUserAndCreatesToken() throws Exception {
        User user = createUser("reset1@example.com", "OldPassword@123");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset1@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists for this email address, password reset instructions have been sent."));

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        PasswordResetToken token = tokens.get(0);
        assertThat(token.getUser().getId()).isEqualTo(user.getId());
        assertThat(token.getUsedAt()).isNull();
        assertThat(token.getInvalidatedAt()).isNull();
    }

    @Test
    void forgotPasswordReturnsGenericSuccessForNonExistingUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists for this email address, password reset instructions have been sent."));

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
    }
    
    @Test
    void subsequentForgotPasswordRequestsInvalidateOldTokens() throws Exception {
        User user = createUser("reset2@example.com", "OldPassword@123");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset2@example.com\"}"))
                .andExpect(status().isOk());
                
        List<PasswordResetToken> tokens1 = passwordResetTokenRepository.findAll();
        assertThat(tokens1).hasSize(1);
        assertThat(tokens1.get(0).getInvalidatedAt()).isNull();
        
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset2@example.com\"}"))
                .andExpect(status().isOk());
                
        List<PasswordResetToken> tokens2 = passwordResetTokenRepository.findAll();
        assertThat(tokens2).hasSize(2);
        
        long activeTokens = tokens2.stream().filter(t -> t.getInvalidatedAt() == null).count();
        long invalidatedTokens = tokens2.stream().filter(t -> t.getInvalidatedAt() != null).count();
        
        assertThat(activeTokens).isEqualTo(1);
        assertThat(invalidatedTokens).isEqualTo(1);
    }
    
    @Test
    void resetPasswordSucceedsWithValidTokenAndRevokesSessions() throws Exception {
        User user = createUser("reset3@example.com", "OldPassword@123");
        
        String rawToken = "test-raw-token";
        String tokenHash = hashToken(rawToken);
        
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setCreatedAt(Instant.now());
        resetToken.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(resetToken);
        
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"NewSecurePassword@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been successfully reset."));
                
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewSecurePassword@123", updatedUser.getPasswordHash())).isTrue();
        
        PasswordResetToken updatedToken = passwordResetTokenRepository.findById(resetToken.getId()).orElseThrow();
        assertThat(updatedToken.getUsedAt()).isNotNull();
    }
    
    @Test
    void resetPasswordFailsWithExpiredToken() throws Exception {
        User user = createUser("reset4@example.com", "OldPassword@123");
        
        String rawToken = "test-raw-token";
        String tokenHash = hashToken(rawToken);
        
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setCreatedAt(Instant.now().minus(30, ChronoUnit.MINUTES));
        resetToken.setExpiresAt(Instant.now().minus(15, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(resetToken);
        
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"NewSecurePassword@123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
    
    @Test
    void resetPasswordPreventsDoubleUsageWithSameToken() throws Exception {
        User user = createUser("reset5@example.com", "OldPassword@123");
        
        String rawToken = "test-raw-token-single-use";
        String tokenHash = hashToken(rawToken);
        
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setCreatedAt(Instant.now());
        resetToken.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(resetToken);
        
        // First attempt succeeds
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"NewSecurePassword@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been successfully reset."));
                
        // Second attempt fails
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"AnotherPassword@123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
                
        // Password remains the one from the first successful attempt
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewSecurePassword@123", updatedUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("AnotherPassword@123", updatedUser.getPasswordHash())).isFalse();
    }
    
    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }
    
    private User createUser(String email, String password) {
        Role role = roleRepository.findByName("ROLE_CUSTOMER").orElseThrow();
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(email);
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.getRoles().add(role);
        return userRepository.save(user);
    }
}
