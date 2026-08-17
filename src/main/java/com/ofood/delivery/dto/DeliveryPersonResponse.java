package com.ofood.delivery.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DeliveryPersonResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String mobile;
    private String vehicleType;
    private String vehicleNumber;
    private String status;
    private List<String> servicePincodes;
    private Instant createdAt;
    private Instant updatedAt;

    public DeliveryPersonResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getServicePincodes() { return servicePincodes; }
    public void setServicePincodes(List<String> servicePincodes) { this.servicePincodes = servicePincodes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
