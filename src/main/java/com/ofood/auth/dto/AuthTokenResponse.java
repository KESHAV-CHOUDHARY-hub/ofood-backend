package com.ofood.auth.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing the newly issued access token. Note: The refresh token is strictly delivered via an HttpOnly Set-Cookie header and is omitted from this JSON payload.")
public record AuthTokenResponse(
    @Schema(description = "The RS256 JWT access token", example = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIn0...") String accessToken, 
    @Schema(description = "Token type", example = "Bearer") String tokenType, 
    @Schema(description = "Access token expiration time in seconds", example = "600") long expiresIn, 
    @Schema(description = "The ID of the authenticated user") UUID userId
) {
}
