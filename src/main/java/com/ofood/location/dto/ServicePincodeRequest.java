package com.ofood.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public class ServicePincodeRequest {
    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    @NotNull(message = "City ID is required")
    private UUID cityId;

    private String status = "ACTIVE";

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
