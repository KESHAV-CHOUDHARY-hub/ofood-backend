package com.ofood.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CityRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug can only contain lowercase letters, numbers, and hyphens")
    private String slug;

    @NotBlank(message = "State is required")
    private String state;

    private String status = "ACTIVE";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
