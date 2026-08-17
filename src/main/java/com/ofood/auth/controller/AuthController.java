package com.ofood.auth.controller;

import com.ofood.auth.config.AuthCookieProperties;
import com.ofood.auth.dto.AuthTokenResponse;
import com.ofood.auth.dto.ChangePasswordRequest;
import com.ofood.auth.dto.LoginRequest;
import com.ofood.auth.dto.LoginResult;
import com.ofood.auth.dto.RegisterRequest;
import com.ofood.auth.dto.RegistrationResponse;
import com.ofood.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for registration, login, token rotation, and password management")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieProperties cookieProperties;

    public AuthController(AuthService authService, AuthCookieProperties cookieProperties) {
        this.authService = authService;
        this.cookieProperties = cookieProperties;
    }

    @Operation(summary = "Register a new customer", description = "Creates a new user with CUSTOMER role and ACTIVE status.")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = com.ofood.auth.dto.ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = com.ofood.auth.dto.ApiErrorResponse.class)))
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegistrationResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login and issue tokens", description = "Authenticates a user and issues an access JWT. Sets an HttpOnly cookie with an opaque refresh token.")
    @ApiResponse(responseCode = "200", description = "Authentication successful", headers = @Header(name = "Set-Cookie", description = "Contains the HttpOnly OFOOD_REFRESH_TOKEN. Attributes: HttpOnly, Secure, SameSite=Lax, Path=/, Max-Age=30d", schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = com.ofood.auth.dto.ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid credentials, account disabled, or account suspended (lockout)", content = @Content(schema = @Schema(implementation = com.ofood.auth.dto.ApiErrorResponse.class)))
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request,
                                                  HttpServletResponse response) {
        LoginResult loginResult = authService.login(request);
        response.addHeader("Set-Cookie", buildRefreshTokenCookie(loginResult.refreshToken()).toString());
        return ResponseEntity.ok(loginResult.tokenResponse());
    }

    @Operation(summary = "Refresh access token", description = "Validates the HttpOnly OFOOD_REFRESH_TOKEN cookie, marks the old token as revoked (revokedAt), and issues a new access JWT and a new refresh token cookie.")
    @ApiResponse(responseCode = "200", description = "Refresh successful", headers = @Header(name = "Set-Cookie", description = "Contains the new rotated HttpOnly OFOOD_REFRESH_TOKEN", schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "401", description = "Invalid, expired, missing, or revoked refresh token", content = @Content(schema = @Schema(implementation = com.ofood.auth.dto.ApiErrorResponse.class)))
    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@Parameter(hidden = true) HttpServletRequest request, @Parameter(hidden = true) HttpServletResponse response) {
        String refreshTokenValue = readRefreshToken(request);
        LoginResult loginResult = authService.refresh(refreshTokenValue);
        response.addHeader("Set-Cookie", buildRefreshTokenCookie(loginResult.refreshToken()).toString());
        return ResponseEntity.ok(loginResult.tokenResponse());
    }

    @Operation(summary = "Logout user", description = "Revokes the current refresh token (via revokedAt) and clears the HttpOnly cookie. Requires a valid Bearer JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Logout successful", headers = @Header(name = "Set-Cookie", description = "Clears the OFOOD_REFRESH_TOKEN cookie (Max-Age=0)", schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "401", description = "Unauthorized (missing/invalid JWT)", content = @Content(schema = @Schema(implementation = com.ofood.auth.dto.ApiErrorResponse.class)))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Parameter(hidden = true) HttpServletRequest request, @Parameter(hidden = true) HttpServletResponse response) {
        String refreshTokenValue = readRefreshToken(request);
        authService.logout(refreshTokenValue);
        response.addHeader("Set-Cookie", clearRefreshTokenCookie().toString());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Change password", description = "Changes the user's password and revokes all active refresh tokens for the user. Requires a valid Bearer JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed, user not found, or current password incorrect", content = @Content(schema = @Schema(implementation = com.ofood.auth.dto.ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized (missing/invalid JWT)", content = @Content(schema = @Schema(implementation = com.ofood.auth.dto.ApiErrorResponse.class)))
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               @Parameter(hidden = true) Authentication authentication) {
        authService.changePassword(request, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get current user profile", description = "Returns the currently authenticated user's profile details. Requires a valid Bearer JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user profile")
    @ApiResponse(responseCode = "401", description = "Unauthorized (missing/invalid JWT)", content = @Content(schema = @Schema(implementation = com.ofood.auth.dto.ApiErrorResponse.class)))
    @GetMapping("/me")
    public ResponseEntity<com.ofood.auth.dto.UserDto> getMe(@Parameter(hidden = true) Authentication authentication) {
        com.ofood.auth.dto.UserDto userDto = authService.getMe(authentication.getName());
        return ResponseEntity.ok(userDto);
    }

    private String readRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieProperties.getRefreshTokenName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(cookieProperties.getRefreshTokenName(), refreshToken)
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(cookieProperties.getMaxAge().getSeconds()))
                .build();
    }

    private ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(cookieProperties.getRefreshTokenName(), "")
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
    }
}
