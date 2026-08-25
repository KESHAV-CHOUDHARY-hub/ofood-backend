package com.ofood.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@Schema(description = "Standard API error response")
public record ApiErrorResponse(
    @Schema(description = "High-level error code", example = "INVALID_CREDENTIALS") String code, 
    @Schema(description = "Detailed error message", example = "Invalid email or password") String message, 
    @Schema(description = "List of detailed errors", example = "[\"price must be greater than zero\"]") @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> errors,
    @Schema(description = "Unique trace ID for debugging", example = "abc123xyz") String traceId
) {
    public ApiErrorResponse(String code, String message) {
        this(code, message, null, null);
    }
    
    public ApiErrorResponse(String code, String message, String traceId) {
        this(code, message, null, traceId);
    }
}
