package com.ofood.auth.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned upon successful registration")
public record RegistrationResponse(
    @Schema(description = "The ID of the newly registered user") UUID userId, 
    @Schema(description = "Success message", example = "User registered successfully") String message
) {
}
