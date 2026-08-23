package com.ofood.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.auth.model.RefreshToken;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.RefreshTokenRepository;
import com.ofood.auth.repository.UserRepository;
import com.ofood.role.Role;
import com.ofood.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthHttpIntegrationTest {

    private static final String REFRESH_COOKIE = "OFOOD_REFRESH_TOKEN";

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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private com.ofood.security.jwt.RsaKeyProvider keyProvider;

    @Autowired
    private com.ofood.security.jwt.RsaKeyProperties rsaKeyProperties;

    @BeforeEach
    void resetUsers() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registrationValidatesInputAndCreatesActiveCustomerWithBcryptPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"customer@example.com","password":"Password@123","fullName":"Customer One"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        User user = userRepository.findByEmail("customer@example.com").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("Password@123");
        assertThat(passwordEncoder.matches("Password@123", user.getPasswordHash())).isTrue();
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
        assertThat(user.getRoles()).extracting(Role::getName).containsExactly("ROLE_CUSTOMER");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"customer@example.com","password":"Password@123","fullName":"Customer One"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid","password":"short","fullName":"Customer One"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void loginIssuesJwtAndPersistsOnlyRefreshTokenHash() throws Exception {
        register("customer@example.com", "Password@123", "Customer One");

        LoginResponse login = login("customer@example.com", "Password@123");
        Jwt jwt = jwtDecoder.decode(login.accessToken());
        assertThat(jwt.getSubject()).isEqualTo(login.userId().toString());
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaimAsString("sid")).isNotBlank();
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_CUSTOMER");

        RefreshToken stored = refreshTokenRepository.findAll().getFirst();
        assertThat(stored.getTokenHash()).isEqualTo(hash(login.refreshToken()));
        assertThat(stored.getTokenHash()).isNotEqualTo(login.refreshToken());
        assertThat(stored.getTokenHash()).hasSize(64);
        assertThat(stored.getExpiresAt()).isAfter(Instant.now());
        assertThat(stored.getId().toString()).isEqualTo(jwt.getClaimAsString("sid"));
    }

    @Test
    void loginRejectsUnknownInactiveSuspendedAndInvalidPasswordUsers() throws Exception {
        register("customer@example.com", "Password@123", "Customer One");
        assertLoginRejected("customer@example.com", "WrongPassword@123", "INVALID_CREDENTIALS");
        assertLoginRejected("unknown@example.com", "Password@123", "INVALID_CREDENTIALS");

        User user = userRepository.findByEmail("customer@example.com").orElseThrow();
        user.setStatus("INACTIVE");
        userRepository.save(user);
        assertLoginRejected("customer@example.com", "Password@123", "ACCOUNT_DISABLED");

        user.setStatus("SUSPENDED");
        userRepository.save(user);
        assertLoginRejected("customer@example.com", "Password@123", "ACCOUNT_SUSPENDED");
    }

    @Test
    void refreshAcceptsValidTokenAndRejectsInvalidExpiredAndRevokedTokens() throws Exception {
        register("customer@example.com", "Password@123", "Customer One");
        LoginResponse login = login("customer@example.com", "Password@123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(login.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString(REFRESH_COOKIE + "=")))
                .andReturn();
                
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        String newRefreshToken = setCookie.substring((REFRESH_COOKIE + "=").length(), setCookie.indexOf(';'));
        
        // Old token should no longer work
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(login.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie("invalid")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(newRefreshToken)).orElseThrow();
        token.setExpiresAt(Instant.now().minusSeconds(1));
        refreshTokenRepository.save(token);
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(newRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        token.setExpiresAt(Instant.now().plusSeconds(60));
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(newRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        // Clear revokedAt so we can test success
        token.setRevokedAt(null);
        refreshTokenRepository.save(token);

        // New token should work
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(newRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
        register("customer@example.com", "Password@123", "Customer One");
        LoginResponse login = login("customer@example.com", "Password@123");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + login.accessToken())
                        .cookie(refreshCookie(login.refreshToken())))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(login.refreshToken())).orElseThrow();
        assertThat(token.getRevokedAt()).isNotNull();
    }

    @Test
    void changePasswordRequiresValidCurrentPasswordAndPersistsBcryptHash() throws Exception {
        register("customer@example.com", "Password@123", "Customer One");
        LoginResponse login = login("customer@example.com", "Password@123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + login.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrong","newPassword":"NewPassword@123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + login.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Password@123","newPassword":"NewPassword@123"}
                                """))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("customer@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("NewPassword@123", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("Password@123", user.getPasswordHash())).isFalse();

        // Verify that the existing refresh token is invalidated
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(login.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void filterChainEnforcesAuthenticationRolesAndCsrfPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/test/customer"))
                .andExpect(status().isUnauthorized());

        register("customer@example.com", "Password@123", "Customer One");
        LoginResponse customer = login("customer@example.com", "Password@123");
        mockMvc.perform(get("/api/v1/test/customer").header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("customer-access"));
        mockMvc.perform(get("/api/v1/test/admin").header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());

        User admin = createUser("admin@example.com", "Password@123", "ROLE_ADMIN");
        LoginResponse adminLogin = login(admin.getEmail(), "Password@123");
        mockMvc.perform(get("/api/v1/test/admin").header("Authorization", "Bearer " + adminLogin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("admin-access"));

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie(adminLogin.refreshToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void discoveryEndpointsExposeConfiguredIssuerAndPublicJwks() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].kid").value("test-kid"))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());

        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8080"))
                .andExpect(jsonPath("$.jwks_uri").value("http://localhost:8080/.well-known/jwks.json"));
    }

    private void register(String email, String password, String fullName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationRequest(email, password, fullName))))
                .andExpect(status().isCreated());
    }

    private LoginResponse login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString(REFRESH_COOKIE + "=")))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        return new LoginResponse(body.get("accessToken").asText(), UUID.fromString(body.get("userId").asText()),
                setCookie.substring((REFRESH_COOKIE + "=").length(), setCookie.indexOf(';')));
    }

    private void assertLoginRejected(String email, String password, String code) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(code));
    }

    private User createUser(String email, String password, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
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

    private MockCookie refreshCookie(String value) {
        return new MockCookie(REFRESH_COOKIE, value);
    }

    private String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private record RegistrationRequest(String email, String password, String fullName) {
    }

    private record LoginRequest(String email, String password) {
    }

    private record LoginResponse(String accessToken, UUID userId, String refreshToken) {
    }

    @Test
    void accountLockoutPreventsLoginAfterTooManyFailedAttempts() throws Exception {
        register("lockout@example.com", "Password@123", "Lockout User");

        // 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"lockout@example.com","password":"wrong","deviceInfo":"{}"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        User user = userRepository.findByEmail("lockout@example.com").orElseThrow();
        assertThat(user.getFailedLogins()).isEqualTo(5);
        assertThat(user.getLockUntil()).isNotNull();

        // Login should now fail even with correct password because it's locked
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"lockout@example.com","password":"Password@123","deviceInfo":"{}"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));

        // Simulate lock expiry
        user.setLockUntil(Instant.now().minusSeconds(1));
        userRepository.save(user);

        // Login should succeed and reset counters
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"lockout@example.com","password":"Password@123","deviceInfo":"{}"}
                                """))
                .andExpect(status().isOk());

        user = userRepository.findByEmail("lockout@example.com").orElseThrow();
        assertThat(user.getFailedLogins()).isEqualTo(0);
        assertThat(user.getLockUntil()).isNull();
    }
    @Test
    void expiredLockoutResetsFailedLoginsCountWhenPasswordIsWrong() throws Exception {
        register("lockout2@example.com", "Password@123", "Lockout User 2");

        // Lock the account
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"lockout2@example.com","password":"wrong","deviceInfo":"{}"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        User user = userRepository.findByEmail("lockout2@example.com").orElseThrow();
        assertThat(user.getFailedLogins()).isEqualTo(5);

        // Simulate lock expiry
        user.setLockUntil(Instant.now().minusSeconds(1));
        userRepository.save(user);

        // Failed login should only increment count to 1, not 6 (which would instantly re-lock)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"lockout2@example.com","password":"wrong","deviceInfo":"{}"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        user = userRepository.findByEmail("lockout2@example.com").orElseThrow();
        assertThat(user.getFailedLogins()).isEqualTo(1);
        assertThat(user.getLockUntil()).isNull();
    }

    @Test
    void authMeWithoutTokenReturnsAuthenticationRequired() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void authMeWithMalformedTokenReturnsAccessTokenInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_INVALID"));
    }

    @Test
    void authMeWithTamperedTokenReturnsAccessTokenInvalid() throws Exception {
        register("tamper@example.com", "Password@123", "Tamper User");
        LoginResponse login = login("tamper@example.com", "Password@123");
        
        String tamperedToken = login.accessToken().substring(0, login.accessToken().length() - 5) + "abcde";
        
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_INVALID"));
    }

    @Test
    void authMeWithExpiredTokenReturnsAccessTokenExpired() throws Exception {
        com.nimbusds.jose.jwk.RSAKey rsaKey = keyProvider.getRsaKey();
        org.springframework.security.oauth2.jwt.JwtEncoder encoder = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(rsaKey)));
        
        Instant now = Instant.now().minusSeconds(3600); // 1 hour ago
        Instant exp = now.plusSeconds(1800); // expired 30 mins ago
        
        org.springframework.security.oauth2.jwt.JwtClaimsSet claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .issuer(rsaKeyProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of("ROLE_CUSTOMER"))
                .claim("sid", UUID.randomUUID().toString())
                .audience(Collections.singletonList(rsaKeyProperties.getAudience()))
                .build();
                
        org.springframework.security.oauth2.jwt.JwsHeader header = org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .keyId(rsaKeyProperties.getKeyId())
                .build();
                
        String expiredToken = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims)).getTokenValue();
        
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_EXPIRED"));
    }

    @Test
    void loginWithStructuredJsonDeviceInfoSucceeds() throws Exception {
        register("device1@example.com", "Password@123", "Device User");
        
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"device1@example.com","password":"Password@123","deviceInfo":{"device":"MacBook Air","platform":"macOS"}}
                                """))
                .andExpect(status().isOk())
                .andReturn();
                
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        String rawToken = setCookie.substring((REFRESH_COOKIE + "=").length(), setCookie.indexOf(';'));
        
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken)).orElseThrow();
        assertThat(stored.getDeviceInfo()).isNotNull();
        assertThat(stored.getDeviceInfo().isObject()).isTrue();
        assertThat(stored.getDeviceInfo().get("device").asText()).isEqualTo("MacBook Air");
    }

    @Test
    void loginWithStringDeviceInfoSucceeds() throws Exception {
        register("device2@example.com", "Password@123", "Device User");
        
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"device2@example.com","password":"Password@123","deviceInfo":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
                
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        String rawToken = setCookie.substring((REFRESH_COOKIE + "=").length(), setCookie.indexOf(';'));
        
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken)).orElseThrow();
        assertThat(stored.getDeviceInfo()).isNotNull();
        assertThat(stored.getDeviceInfo().isTextual()).isTrue();
        assertThat(stored.getDeviceInfo().asText()).isEqualTo("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
    }

    @Test
    void shouldReturnMe() throws Exception {
        String email = "me_test@example.com";
        String password = "Password@123";
        String firstName = "Keshav";
        String lastName = "Choudhary";
        
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\"}"))
                .andExpect(status().isCreated());
                
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
                
        String jsonResponse = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(jsonResponse).get("accessToken").asText();
        
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value(firstName))
                .andExpect(jsonPath("$.lastName").value(lastName))
                .andExpect(jsonPath("$.fullName").value(firstName + " " + lastName))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CUSTOMER"));
    }
}
