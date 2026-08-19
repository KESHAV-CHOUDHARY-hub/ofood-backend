package com.ofood.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API error response")
public record ApiErrorResponse(
    @Schema(description = "High-level error code", example = "INVALID_CREDENTIALS") String code, 
    @Schema(description = "Detailed error message", example = "Invalid email or password") String message, 
    @Schema(description = "Unique trace ID for debugging", example = "abc123xyz") String traceId
) {
    public ApiErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
