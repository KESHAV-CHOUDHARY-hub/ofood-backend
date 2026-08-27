package com.ofood.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.ofood.location.model.ServiceArea;
import java.util.UUID;

public class ServicePincodeRequest {
    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    @NotNull(message = "City ID is required")
    private UUID cityId;

    @NotBlank(message = "Area name is required")
    @Size(max = 100, message = "Area name must not exceed 100 characters")
    private String areaName;

    private Boolean isActive;

    private ServiceArea serviceArea;

    private String status = "ACTIVE";

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public ServiceArea getServiceArea() { return serviceArea; }
    public void setServiceArea(ServiceArea serviceArea) { this.serviceArea = serviceArea; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
