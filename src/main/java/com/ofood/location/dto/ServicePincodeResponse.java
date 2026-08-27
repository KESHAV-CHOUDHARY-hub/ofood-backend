package com.ofood.location.dto;

import com.ofood.location.model.ServiceArea;
import java.time.Instant;
import java.util.UUID;

public class ServicePincodeResponse {
    private UUID id;
    private String pincode;
    private UUID cityId;
    private String cityName;
    private String areaName;
    private ServiceArea serviceArea;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public ServicePincodeResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public ServiceArea getServiceArea() { return serviceArea; }
    public void setServiceArea(ServiceArea serviceArea) { this.serviceArea = serviceArea; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
