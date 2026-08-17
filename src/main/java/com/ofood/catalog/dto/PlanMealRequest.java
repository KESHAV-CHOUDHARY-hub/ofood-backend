package com.ofood.catalog.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public class PlanMealRequest {

    @NotBlank(message = "Meal type is required")
    private String mealType;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private Integer calories;
    private String servingSize;
    private JsonNode ingredients;
    private JsonNode nutrition;
    private String imageUrl;
    private Integer displayOrder = 0;

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }
    public String getServingSize() { return servingSize; }
    public void setServingSize(String servingSize) { this.servingSize = servingSize; }
    public JsonNode getIngredients() { return ingredients; }
    public void setIngredients(JsonNode ingredients) { this.ingredients = ingredients; }
    public JsonNode getNutrition() { return nutrition; }
    public void setNutrition(JsonNode nutrition) { this.nutrition = nutrition; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
