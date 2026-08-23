package com.ofood.catalog.service;

import com.ofood.catalog.dto.PlanMealRequest;
import com.ofood.catalog.dto.PlanMealResponse;
import com.ofood.catalog.dto.PlanRequest;
import com.ofood.catalog.dto.PlanResponse;
import com.ofood.catalog.dto.ReorderPlansRequest;
import com.ofood.catalog.model.Plan;
import com.ofood.catalog.model.PlanMeal;
import com.ofood.catalog.repository.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {
        return planRepository.findByStatusOrderByDisplayOrderAsc("ACTIVE").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {
        return planRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanResponse getActivePlanById(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found"));
        if (!"ACTIVE".equals(plan.getStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found");
        }
        return mapToResponse(plan);
    }

    @Transactional(readOnly = true)
    public PlanResponse getPlanByIdForAdmin(UUID id) {
        return planRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found"));
    }

    @Transactional(readOnly = true)
    public PlanResponse getActivePlanBySlug(String slug) {
        Plan plan = planRepository.findBySlug(slug)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found"));
        if (!"ACTIVE".equals(plan.getStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found");
        }
        return mapToResponse(plan);
    }

    @Transactional
    public PlanResponse createPlan(PlanRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        
        Plan plan = new Plan();
        plan.setStatus("DRAFT"); // Default
        
        // Initial slug so it's not null before updates
        plan.setSlug(generateSlug(request.getName())); 
        
        updatePlanFromRequest(plan, request);
        
        // Check activation and pricing rules against final state
        validateFinalPlanState(plan);
        
        plan = planRepository.save(plan);
        return mapToResponse(plan);
    }

    @Transactional
    public PlanResponse updatePlan(UUID id, PlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found"));

        if (request.getName() != null && !request.getName().equals(plan.getName())) {
            plan.setSlug(generateSlug(request.getName()));
        }
        
        if (request.getMeals() != null) {
            plan.getMeals().clear();
        }
        
        updatePlanFromRequest(plan, request);
        
        // Check activation and pricing rules against final state
        validateFinalPlanState(plan);
        
        plan.setUpdatedAt(Instant.now());

        plan = planRepository.save(plan);
        return mapToResponse(plan);
    }

    @Transactional
    public void deletePlan(UUID id) {
        if (!planRepository.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found");
        }
        planRepository.deleteById(id);
    }

    @Transactional
    public PlanResponse duplicatePlan(UUID id) {
        Plan original = planRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found"));

        Plan duplicate = new Plan();
        duplicate.setName(original.getName() + " (Copy)");
        duplicate.setSlug(generateSlug(duplicate.getName()));
        duplicate.setShortDescription(original.getShortDescription());
        duplicate.setDescription(original.getDescription());
        duplicate.setImage(original.getImage());
        duplicate.setGallery(original.getGallery());
        duplicate.setPrice(original.getPrice());
        duplicate.setCompareAtPrice(original.getCompareAtPrice());
        duplicate.setCurrency(original.getCurrency());
        duplicate.setDuration(original.getDuration());
        duplicate.setDurationUnit(original.getDurationUnit());
        duplicate.setMealCount(original.getMealCount());
        duplicate.setMealsPerDay(original.getMealsPerDay());
        duplicate.setServingsPerMeal(original.getServingsPerMeal());
        duplicate.setMealTypes(original.getMealTypes());
        duplicate.setFeatures(original.getFeatures());
        duplicate.setIngredients(original.getIngredients());
        duplicate.setNutrition(original.getNutrition());
        duplicate.setStatus("DRAFT");
        duplicate.setIsFeatured(false);
        duplicate.setDisplayOrder(original.getDisplayOrder() + 1);

        for (PlanMeal meal : original.getMeals()) {
            PlanMeal dm = new PlanMeal();
            dm.setPlan(duplicate);
            dm.setMealType(meal.getMealType());
            dm.setName(meal.getName());
            dm.setDescription(meal.getDescription());
            dm.setCalories(meal.getCalories());
            dm.setServingSize(meal.getServingSize());
            dm.setIngredients(meal.getIngredients());
            dm.setNutrition(meal.getNutrition());
            dm.setImageUrl(meal.getImageUrl());
            dm.setDisplayOrder(meal.getDisplayOrder());
            duplicate.getMeals().add(dm);
        }

        duplicate = planRepository.save(duplicate);
        return mapToResponse(duplicate);
    }

    @Transactional
    public void reorderPlans(ReorderPlansRequest request) {
        List<UUID> ids = request.getPlanIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Plan> plans = planRepository.findAllById(ids);
        if (plans.size() != ids.size()) {
            throw new IllegalArgumentException("One or more plan IDs are invalid");
        }

        Map<UUID, Plan> planMap = plans.stream().collect(Collectors.toMap(Plan::getId, Function.identity()));
        int order = 0;
        for (UUID id : ids) {
            Plan plan = planMap.get(id);
            plan.setDisplayOrder(order++);
            plan.setUpdatedAt(Instant.now());
        }
        planRepository.saveAll(plans);
    }

    
    private String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        String baseSlug = name.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
        // Remove trailing hyphens
        baseSlug = baseSlug.replaceAll("-$", "");
        if (baseSlug.isEmpty()) {
            baseSlug = "plan";
        }
        String slug = baseSlug;
        int counter = 2;
        while (planRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }

    private void validateFinalPlanState(Plan plan) {
        // Pricing validation: compareAtPrice >= price
        if (plan.getPrice() != null && plan.getCompareAtPrice() != null) {
            if (plan.getCompareAtPrice().compareTo(plan.getPrice()) < 0) {
                throw new IllegalArgumentException("compareAtPrice must be greater than or equal to price");
            }
        }

        // Activation validation
        if ("ACTIVE".equals(plan.getStatus())) {
            List<String> missingFields = new ArrayList<>();

            if (plan.getName() == null || plan.getName().trim().isEmpty()) missingFields.add("name");
            if (plan.getPrice() == null) missingFields.add("price");
            if (plan.getCurrency() == null || plan.getCurrency().trim().isEmpty()) missingFields.add("currency");
            if (plan.getDuration() == null) missingFields.add("duration");
            if (plan.getDurationUnit() == null || plan.getDurationUnit().trim().isEmpty()) missingFields.add("durationUnit");
            if (plan.getMealCount() == null) missingFields.add("mealCount");
            if (plan.getMealsPerDay() == null) missingFields.add("mealsPerDay");
            if (plan.getServingsPerMeal() == null) missingFields.add("servingsPerMeal");
            if (plan.getMealTypes() == null || plan.getMealTypes().isEmpty()) missingFields.add("mealTypes");

            if (!missingFields.isEmpty()) {
                throw new IllegalArgumentException("Cannot activate plan. Missing required fields: " + String.join(", ", missingFields));
            }
        }
    }
    private void updatePlanFromRequest(Plan plan, PlanRequest request) {
        if (request.getName() != null) plan.setName(request.getName());
        if (request.getShortDescription() != null) plan.setShortDescription(request.getShortDescription());
        if (request.getDescription() != null) plan.setDescription(request.getDescription());
        if (request.getImage() != null) plan.setImage(request.getImage());
        if (request.getGallery() != null) plan.setGallery(request.getGallery());
        if (request.getPrice() != null) plan.setPrice(request.getPrice());
        if (request.getCompareAtPrice() != null) plan.setCompareAtPrice(request.getCompareAtPrice());
        if (request.getCurrency() != null) plan.setCurrency(request.getCurrency());
        if (request.getDuration() != null) plan.setDuration(request.getDuration());
        if (request.getDurationUnit() != null) plan.setDurationUnit(request.getDurationUnit());
        if (request.getMealCount() != null) plan.setMealCount(request.getMealCount());
        if (request.getMealsPerDay() != null) plan.setMealsPerDay(request.getMealsPerDay());
        if (request.getServingsPerMeal() != null) plan.setServingsPerMeal(request.getServingsPerMeal());
        if (request.getMealTypes() != null) plan.setMealTypes(request.getMealTypes());
        if (request.getFeatures() != null) plan.setFeatures(request.getFeatures());
        if (request.getIngredients() != null) plan.setIngredients(request.getIngredients());
        if (request.getNutrition() != null) plan.setNutrition(request.getNutrition());
        
        if (request.getCaloriesLabel() != null) plan.setCaloriesLabel(request.getCaloriesLabel());
        if (request.getDeliveryInformation() != null) plan.setDeliveryInformation(request.getDeliveryInformation());
        if (request.getTerms() != null) plan.setTerms(request.getTerms());
        if (request.getSeoTitle() != null) plan.setSeoTitle(request.getSeoTitle());
        if (request.getSeoDescription() != null) plan.setSeoDescription(request.getSeoDescription());

        if (request.getStatus() != null) plan.setStatus(request.getStatus());
        if (request.getIsFeatured() != null) plan.setIsFeatured(request.getIsFeatured());
        if (request.getDisplayOrder() != null) plan.setDisplayOrder(request.getDisplayOrder());

        if (request.getMeals() != null) {
            for (PlanMealRequest mealReq : request.getMeals()) {
                PlanMeal meal = new PlanMeal();
                meal.setPlan(plan);
                meal.setMealType(mealReq.getMealType());
                meal.setName(mealReq.getName());
                meal.setDescription(mealReq.getDescription());
                meal.setCalories(mealReq.getCalories());
                meal.setServingSize(mealReq.getServingSize());
                meal.setIngredients(mealReq.getIngredients());
                meal.setNutrition(mealReq.getNutrition());
                meal.setImageUrl(mealReq.getImageUrl());
                meal.setDisplayOrder(mealReq.getDisplayOrder() != null ? mealReq.getDisplayOrder() : 0);
                plan.getMeals().add(meal);
            }
        }
    }

    private PlanResponse mapToResponse(Plan plan) {
        PlanResponse response = new PlanResponse();
        response.setId(plan.getId());
        response.setName(plan.getName());
        response.setSlug(plan.getSlug());
        response.setShortDescription(plan.getShortDescription());
        response.setDescription(plan.getDescription());
        response.setImage(plan.getImage());
        response.setGallery(plan.getGallery());
        response.setPrice(plan.getPrice());
        response.setCompareAtPrice(plan.getCompareAtPrice());
        response.setCurrency(plan.getCurrency());
        response.setDuration(plan.getDuration());
        response.setDurationUnit(plan.getDurationUnit());
        response.setMealCount(plan.getMealCount());
        response.setMealsPerDay(plan.getMealsPerDay());
        response.setServingsPerMeal(plan.getServingsPerMeal());
        response.setMealTypes(plan.getMealTypes());
        response.setFeatures(plan.getFeatures());
        response.setIngredients(plan.getIngredients());
        response.setNutrition(plan.getNutrition());
        response.setCaloriesLabel(plan.getCaloriesLabel());
        response.setDeliveryInformation(plan.getDeliveryInformation());
        response.setTerms(plan.getTerms());
        response.setSeoTitle(plan.getSeoTitle());
        response.setSeoDescription(plan.getSeoDescription());
        response.setStatus(plan.getStatus());
        response.setIsFeatured(plan.getIsFeatured());
        response.setDisplayOrder(plan.getDisplayOrder());
        response.setCreatedAt(plan.getCreatedAt());
        response.setUpdatedAt(plan.getUpdatedAt());

        List<PlanMealResponse> mealResponses = new ArrayList<>();
        for (PlanMeal meal : plan.getMeals()) {
            PlanMealResponse mr = new PlanMealResponse();
            mr.setId(meal.getId());
            mr.setMealType(meal.getMealType());
            mr.setName(meal.getName());
            mr.setDescription(meal.getDescription());
            mr.setCalories(meal.getCalories());
            mr.setServingSize(meal.getServingSize());
            mr.setIngredients(meal.getIngredients());
            mr.setNutrition(meal.getNutrition());
            mr.setImageUrl(meal.getImageUrl());
            mr.setDisplayOrder(meal.getDisplayOrder());
            mr.setCreatedAt(meal.getCreatedAt());
            mr.setUpdatedAt(meal.getUpdatedAt());
            mealResponses.add(mr);
        }
        response.setMeals(mealResponses);

        return response;
    }
}
