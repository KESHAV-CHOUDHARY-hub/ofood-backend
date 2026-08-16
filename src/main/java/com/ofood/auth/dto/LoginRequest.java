package com.ofood.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request object for user login")
public class LoginRequest {

    @Schema(description = "User's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @Schema(description = "User's password", example = "Password@123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    private String password;

    @Schema(description = "Optional device info metadata for the refresh token session. Accepts structured JSON objects or plain strings.", example = "{\"device\":\"MacBook Air\", \"platform\":\"macOS\"}")
    private com.fasterxml.jackson.databind.JsonNode deviceInfo;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public com.fasterxml.jackson.databind.JsonNode getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(com.fasterxml.jackson.databind.JsonNode deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
}
