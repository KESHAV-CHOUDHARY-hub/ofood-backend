package com.ofood.catalog.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PlanResponse {
    private UUID id;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String image;
    private JsonNode gallery;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String currency;
    private Integer duration;
    private String durationUnit;
    private Integer mealCount;
    private Integer mealsPerDay;
    private Integer servingsPerMeal;
    private JsonNode mealTypes;
    private JsonNode features;
    private JsonNode ingredients;
    private JsonNode nutrition;
    private String status;
    private Boolean isFeatured;
    private Integer displayOrder;
    private List<PlanMealResponse> meals;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public JsonNode getGallery() { return gallery; }
    public void setGallery(JsonNode gallery) { this.gallery = gallery; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getCompareAtPrice() { return compareAtPrice; }
    public void setCompareAtPrice(BigDecimal compareAtPrice) { this.compareAtPrice = compareAtPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getDurationUnit() { return durationUnit; }
    public void setDurationUnit(String durationUnit) { this.durationUnit = durationUnit; }
    public Integer getMealCount() { return mealCount; }
    public void setMealCount(Integer mealCount) { this.mealCount = mealCount; }
    public Integer getMealsPerDay() { return mealsPerDay; }
    public void setMealsPerDay(Integer mealsPerDay) { this.mealsPerDay = mealsPerDay; }
    public Integer getServingsPerMeal() { return servingsPerMeal; }
    public void setServingsPerMeal(Integer servingsPerMeal) { this.servingsPerMeal = servingsPerMeal; }
    public JsonNode getMealTypes() { return mealTypes; }
    public void setMealTypes(JsonNode mealTypes) { this.mealTypes = mealTypes; }
    public JsonNode getFeatures() { return features; }
    public void setFeatures(JsonNode features) { this.features = features; }
    public JsonNode getIngredients() { return ingredients; }
    public void setIngredients(JsonNode ingredients) { this.ingredients = ingredients; }
    public JsonNode getNutrition() { return nutrition; }
    public void setNutrition(JsonNode nutrition) { this.nutrition = nutrition; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean featured) { isFeatured = featured; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public List<PlanMealResponse> getMeals() { return meals; }
    public void setMeals(List<PlanMealResponse> meals) { this.meals = meals; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
