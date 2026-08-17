package com.ofood.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request object for user registration")
public class RegisterRequest {

    @Schema(description = "Valid email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @Schema(description = "Password (8 to 128 characters)", example = "Password@123", minLength = 8, maxLength = 128, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @Schema(description = "User's full name (Optional if firstName and lastName are provided)", example = "John Doe", maxLength = 255)
    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;

    @Schema(description = "User's first name", example = "John", maxLength = 255)
    @Size(max = 255, message = "First name must be at most 255 characters")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe", maxLength = 255)
    @Size(max = 255, message = "Last name must be at most 255 characters")
    private String lastName;

    @Schema(description = "User's mobile number", example = "+1234567890", maxLength = 50)
    @Size(max = 50, message = "Mobile must be at most 50 characters")
    private String mobile;

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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @Schema(hidden = true)
    @AssertTrue(message = "Either full name or both first and last name must be provided")
    public boolean isValidName() {
        boolean hasFullName = fullName != null && !fullName.trim().isEmpty();
        boolean hasFirstAndLast = firstName != null && !firstName.trim().isEmpty() && lastName != null && !lastName.trim().isEmpty();
        return hasFullName || hasFirstAndLast;
    }
}
