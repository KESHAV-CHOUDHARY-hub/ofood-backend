package com.ofood.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic message response")
public record ApiMessageResponse(
    @Schema(description = "Response message", example = "Operation successful")
    String message
) {}
