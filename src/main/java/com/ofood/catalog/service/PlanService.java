package com.ofood.catalog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.catalog.dto.PlanMealRequest;
import com.ofood.catalog.dto.PlanMealResponse;
import com.ofood.catalog.dto.PlanRequest;
import com.ofood.catalog.dto.PlanResponse;
import com.ofood.catalog.dto.ReorderPlansRequest;
import com.ofood.catalog.exception.CatalogValidationException;
import com.ofood.catalog.model.Plan;
import com.ofood.catalog.model.PlanMeal;
import com.ofood.catalog.repository.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final ObjectMapper objectMapper;

    public PlanService(PlanRepository planRepository, ObjectMapper objectMapper) {
        this.planRepository = planRepository;
        this.objectMapper = objectMapper;
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
        // Enforce name on create
        if (request.getName() == null || request.getName().isEmpty() || request.getName().get() == null || request.getName().get().trim().isEmpty()) {
            throw new CatalogValidationException("Validation failed", List.of("name is required for creating a plan"));
        }
        
        Plan plan = new Plan();
        plan.setStatus("DRAFT"); // Default
        String name = request.getName().get();
        plan.setName(name);
        plan.setSlug(generateSlug(name));
        
        updatePlanFromRequest(plan, request);
        validateFinalPlanState(plan);
        
        plan = planRepository.save(plan);
        return mapToResponse(plan);
    }

    @Transactional
    public PlanResponse updatePlan(UUID id, PlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found"));

        if (request.getName() != null) {
            String newName = request.getName().orElse(null);
            if (newName == null || newName.trim().isEmpty()) {
                throw new CatalogValidationException("Validation failed", List.of("name cannot be null or blank"));
            }
            if (!newName.equals(plan.getName())) {
                plan.setName(newName);
                plan.setSlug(generateSlug(newName));
            }
        }
        
        updatePlanFromRequest(plan, request);
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

        validateFinalPlanState(duplicate);
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
        List<String> errors = new ArrayList<>();

        // Normal numeric/business validation
        if (plan.getPrice() != null && plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Price must be greater than zero");
        }
        if (plan.getCompareAtPrice() != null && plan.getCompareAtPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Compare at price must be zero or positive");
        }
        if (plan.getPrice() != null && plan.getCompareAtPrice() != null) {
            if (plan.getCompareAtPrice().compareTo(plan.getPrice()) < 0) {
                errors.add("compareAtPrice must be greater than or equal to price");
            }
        }
        if (plan.getDuration() != null && plan.getDuration() < 1) {
            errors.add("Duration must be at least 1");
        }
        if (plan.getMealCount() != null && plan.getMealCount() < 1) {
            errors.add("Meal count must be at least 1");
        }
        if (plan.getMealsPerDay() != null && plan.getMealsPerDay() < 1) {
            errors.add("Meals per day must be at least 1");
        }
        if (plan.getServingsPerMeal() != null && plan.getServingsPerMeal() < 1) {
            errors.add("Servings per meal must be at least 1");
        }
        if (plan.getDisplayOrder() != null && plan.getDisplayOrder() < 0) {
            errors.add("Display order must be zero or positive");
        }

        // Activation validation
        if ("ACTIVE".equals(plan.getStatus())) {
            if (plan.getName() == null || plan.getName().trim().isEmpty()) errors.add("name is required for ACTIVE plan");
            if (plan.getPrice() == null) errors.add("price is required for ACTIVE plan");
            if (plan.getCurrency() == null || plan.getCurrency().trim().isEmpty()) errors.add("currency is required for ACTIVE plan");
            if (plan.getDuration() == null) errors.add("duration is required for ACTIVE plan");
            if (plan.getDurationUnit() == null || plan.getDurationUnit().trim().isEmpty()) errors.add("durationUnit is required for ACTIVE plan");
            if (plan.getMealCount() == null) errors.add("mealCount is required for ACTIVE plan");
            if (plan.getMealsPerDay() == null) errors.add("mealsPerDay is required for ACTIVE plan");
            if (plan.getServingsPerMeal() == null) errors.add("servingsPerMeal is required for ACTIVE plan");
            if (plan.getMealTypes() == null || plan.getMealTypes().isEmpty() || (plan.getMealTypes().isArray() && plan.getMealTypes().size() == 0)) {
                errors.add("mealTypes is required for ACTIVE plan");
            }
        }

        if (!errors.isEmpty()) {
            throw new CatalogValidationException("Plan validation failed", errors);
        }
    }

    private void updatePlanFromRequest(Plan plan, PlanRequest request) {
        // Name is handled in createPlan / updatePlan directly
        if (request.getShortDescription() != null) plan.setShortDescription(request.getShortDescription().orElse(null));
        if (request.getDescription() != null) plan.setDescription(request.getDescription().orElse(null));
        if (request.getImage() != null) plan.setImage(request.getImage().orElse(null));
        if (request.getPrice() != null) plan.setPrice(request.getPrice().orElse(null));
        if (request.getCompareAtPrice() != null) plan.setCompareAtPrice(request.getCompareAtPrice().orElse(null));
        if (request.getCurrency() != null) plan.setCurrency(request.getCurrency().orElse(null));
        if (request.getDuration() != null) plan.setDuration(request.getDuration().orElse(null));
        if (request.getDurationUnit() != null) plan.setDurationUnit(request.getDurationUnit().orElse(null));
        if (request.getMealCount() != null) plan.setMealCount(request.getMealCount().orElse(null));
        if (request.getMealsPerDay() != null) plan.setMealsPerDay(request.getMealsPerDay().orElse(null));
        if (request.getServingsPerMeal() != null) plan.setServingsPerMeal(request.getServingsPerMeal().orElse(null));
        if (request.getCaloriesLabel() != null) plan.setCaloriesLabel(request.getCaloriesLabel().orElse(null));
        if (request.getDeliveryInformation() != null) plan.setDeliveryInformation(request.getDeliveryInformation().orElse(null));
        if (request.getTerms() != null) plan.setTerms(request.getTerms().orElse(null));
        if (request.getSeoTitle() != null) plan.setSeoTitle(request.getSeoTitle().orElse(null));
        if (request.getSeoDescription() != null) plan.setSeoDescription(request.getSeoDescription().orElse(null));
        if (request.getStatus() != null) plan.setStatus(request.getStatus().orElse("DRAFT"));
        if (request.getIsFeatured() != null) plan.setIsFeatured(request.getIsFeatured().orElse(false));
        if (request.getDisplayOrder() != null) plan.setDisplayOrder(request.getDisplayOrder().orElse(0));

        // Collection handling
        if (request.getGallery() != null) plan.setGallery(handleJsonCollection(request.getGallery()));
        if (request.getMealTypes() != null) plan.setMealTypes(handleJsonCollection(request.getMealTypes()));
        if (request.getFeatures() != null) plan.setFeatures(handleJsonCollection(request.getFeatures()));
        if (request.getIngredients() != null) plan.setIngredients(handleJsonCollection(request.getIngredients()));
        if (request.getNutrition() != null) plan.setNutrition(handleJsonCollection(request.getNutrition()));

        if (request.getMeals() != null) {
            Optional<List<PlanMealRequest>> mealsOpt = request.getMeals();
            plan.getMeals().clear();
            if (mealsOpt.isPresent() && mealsOpt.get() != null) {
                for (PlanMealRequest mealReq : mealsOpt.get()) {
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
    }

    private JsonNode handleJsonCollection(Optional<JsonNode> optionalNode) {
        if (!optionalNode.isPresent()) {
            return null; // Explicit null -> set column to null
        }
        JsonNode node = optionalNode.get();
        if (node == null) {
            return null; // Safety check
        }
        if (node.isArray() && node.isEmpty()) {
            return objectMapper.createArrayNode(); // Explicit [] -> empty array
        }
        return node; // Supplied values -> replace
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
