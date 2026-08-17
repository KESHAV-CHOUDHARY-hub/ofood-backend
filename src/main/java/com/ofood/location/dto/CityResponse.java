package com.ofood.location.dto;

import java.time.Instant;
import java.util.UUID;

public class CityResponse {
    private UUID id;
    private String name;
    private String slug;
    private String state;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public CityResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
