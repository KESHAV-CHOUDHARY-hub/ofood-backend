package com.ofood.auth.dto;

public record LoginResult(AuthTokenResponse tokenResponse, String refreshToken) {
}
