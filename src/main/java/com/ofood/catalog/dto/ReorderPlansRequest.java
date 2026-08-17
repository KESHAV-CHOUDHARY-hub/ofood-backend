package com.ofood.catalog.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public class ReorderPlansRequest {
    @NotEmpty(message = "Plan IDs cannot be empty")
    private List<UUID> planIds;

    public List<UUID> getPlanIds() { return planIds; }
    public void setPlanIds(List<UUID> planIds) { this.planIds = planIds; }
}
