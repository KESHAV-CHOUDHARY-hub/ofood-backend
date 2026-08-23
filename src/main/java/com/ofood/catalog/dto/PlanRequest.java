package com.ofood.catalog.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public class PlanRequest {

    private String name;

    private String shortDescription;
    private String description;
    private String image;
    private JsonNode gallery;
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    private BigDecimal price;

    private BigDecimal compareAtPrice;
    private String currency = "INR";
    @Min(value = 1, message = "Duration must be at least 1")
    private Integer duration;

        private String durationUnit;
    @Min(value = 1, message = "Meal count must be at least 1")
    private Integer mealCount;
    @Min(value = 1, message = "Meals per day must be at least 1")
    private Integer mealsPerDay;
    @Min(value = 1, message = "Servings per meal must be at least 1")
    private Integer servingsPerMeal;

    private JsonNode mealTypes;
    private JsonNode features;
    private JsonNode ingredients;
    private JsonNode nutrition;
    private String caloriesLabel;
    private String deliveryInformation;
    private String terms;
    private String seoTitle;
    private String seoDescription;

    private String status = "DRAFT";
    private Boolean isFeatured = false;
    private Integer displayOrder = 0;

    private List<PlanMealRequest> meals;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    
    public String getCaloriesLabel() { return caloriesLabel; }
    public void setCaloriesLabel(String caloriesLabel) { this.caloriesLabel = caloriesLabel; }
    public String getDeliveryInformation() { return deliveryInformation; }
    public void setDeliveryInformation(String deliveryInformation) { this.deliveryInformation = deliveryInformation; }
    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean featured) { isFeatured = featured; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public List<PlanMealRequest> getMeals() { return meals; }
    public void setMeals(List<PlanMealRequest> meals) { this.meals = meals; }
}
