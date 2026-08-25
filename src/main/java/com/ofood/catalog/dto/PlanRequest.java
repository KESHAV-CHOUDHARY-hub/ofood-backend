package com.ofood.catalog.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class PlanRequest {

    private Optional<String> name;
    private Optional<String> shortDescription;
    private Optional<String> description;
    private Optional<String> image;
    private Optional<JsonNode> gallery;
    private Optional<BigDecimal> price;
    private Optional<BigDecimal> compareAtPrice;
    private Optional<String> currency;
    private Optional<Integer> duration;
    private Optional<String> durationUnit;
    private Optional<Integer> mealCount;
    private Optional<Integer> mealsPerDay;
    private Optional<Integer> servingsPerMeal;

    private Optional<JsonNode> mealTypes;
    private Optional<JsonNode> features;
    private Optional<JsonNode> ingredients;
    private Optional<JsonNode> nutrition;
    
    private Optional<String> caloriesLabel;
    private Optional<String> deliveryInformation;
    private Optional<String> terms;
    private Optional<String> seoTitle;
    private Optional<String> seoDescription;

    private Optional<String> status;
    private Optional<Boolean> isFeatured;
    private Optional<Integer> displayOrder;

    private Optional<List<PlanMealRequest>> meals;

    public Optional<String> getName() { return name; }
    public void setName(Optional<String> name) { this.name = name; }
    public Optional<String> getShortDescription() { return shortDescription; }
    public void setShortDescription(Optional<String> shortDescription) { this.shortDescription = shortDescription; }
    public Optional<String> getDescription() { return description; }
    public void setDescription(Optional<String> description) { this.description = description; }
    public Optional<String> getImage() { return image; }
    public void setImage(Optional<String> image) { this.image = image; }
    public Optional<JsonNode> getGallery() { return gallery; }
    public void setGallery(Optional<JsonNode> gallery) { this.gallery = gallery; }
    public Optional<BigDecimal> getPrice() { return price; }
    public void setPrice(Optional<BigDecimal> price) { this.price = price; }
    public Optional<BigDecimal> getCompareAtPrice() { return compareAtPrice; }
    public void setCompareAtPrice(Optional<BigDecimal> compareAtPrice) { this.compareAtPrice = compareAtPrice; }
    public Optional<String> getCurrency() { return currency; }
    public void setCurrency(Optional<String> currency) { this.currency = currency; }
    public Optional<Integer> getDuration() { return duration; }
    public void setDuration(Optional<Integer> duration) { this.duration = duration; }
    public Optional<String> getDurationUnit() { return durationUnit; }
    public void setDurationUnit(Optional<String> durationUnit) { this.durationUnit = durationUnit; }
    public Optional<Integer> getMealCount() { return mealCount; }
    public void setMealCount(Optional<Integer> mealCount) { this.mealCount = mealCount; }
    public Optional<Integer> getMealsPerDay() { return mealsPerDay; }
    public void setMealsPerDay(Optional<Integer> mealsPerDay) { this.mealsPerDay = mealsPerDay; }
    public Optional<Integer> getServingsPerMeal() { return servingsPerMeal; }
    public void setServingsPerMeal(Optional<Integer> servingsPerMeal) { this.servingsPerMeal = servingsPerMeal; }
    public Optional<JsonNode> getMealTypes() { return mealTypes; }
    public void setMealTypes(Optional<JsonNode> mealTypes) { this.mealTypes = mealTypes; }
    public Optional<JsonNode> getFeatures() { return features; }
    public void setFeatures(Optional<JsonNode> features) { this.features = features; }
    public Optional<JsonNode> getIngredients() { return ingredients; }
    public void setIngredients(Optional<JsonNode> ingredients) { this.ingredients = ingredients; }
    public Optional<JsonNode> getNutrition() { return nutrition; }
    public void setNutrition(Optional<JsonNode> nutrition) { this.nutrition = nutrition; }
    
    public Optional<String> getCaloriesLabel() { return caloriesLabel; }
    public void setCaloriesLabel(Optional<String> caloriesLabel) { this.caloriesLabel = caloriesLabel; }
    public Optional<String> getDeliveryInformation() { return deliveryInformation; }
    public void setDeliveryInformation(Optional<String> deliveryInformation) { this.deliveryInformation = deliveryInformation; }
    public Optional<String> getTerms() { return terms; }
    public void setTerms(Optional<String> terms) { this.terms = terms; }
    public Optional<String> getSeoTitle() { return seoTitle; }
    public void setSeoTitle(Optional<String> seoTitle) { this.seoTitle = seoTitle; }
    public Optional<String> getSeoDescription() { return seoDescription; }
    public void setSeoDescription(Optional<String> seoDescription) { this.seoDescription = seoDescription; }
    public Optional<String> getStatus() { return status; }
    public void setStatus(Optional<String> status) { this.status = status; }
    public Optional<Boolean> getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Optional<Boolean> isFeatured) { this.isFeatured = isFeatured; }
    public Optional<Integer> getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Optional<Integer> displayOrder) { this.displayOrder = displayOrder; }
    public Optional<List<PlanMealRequest>> getMeals() { return meals; }
    public void setMeals(Optional<List<PlanMealRequest>> meals) { this.meals = meals; }
}
