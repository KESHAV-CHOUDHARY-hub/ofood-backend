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
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        if (!"ACTIVE".equals(plan.getStatus())) {
            throw new IllegalArgumentException("Plan not found or inactive");
        }
        return mapToResponse(plan);
    }

    @Transactional(readOnly = true)
    public PlanResponse getPlanByIdForAdmin(UUID id) {
        return planRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
    }

    @Transactional(readOnly = true)
    public PlanResponse getActivePlanBySlug(String slug) {
        Plan plan = planRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        if (!"ACTIVE".equals(plan.getStatus())) {
            throw new IllegalArgumentException("Plan not found or inactive");
        }
        return mapToResponse(plan);
    }

    @Transactional
    public PlanResponse createPlan(PlanRequest request) {
        if (planRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Plan slug already exists");
        }
        Plan plan = new Plan();
        updatePlanFromRequest(plan, request);
        plan = planRepository.save(plan);
        return mapToResponse(plan);
    }

    @Transactional
    public PlanResponse updatePlan(UUID id, PlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        if (!plan.getSlug().equals(request.getSlug()) && planRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Plan slug already exists");
        }
        
        plan.getMeals().clear(); // Let Hibernate manage orphan removal
        updatePlanFromRequest(plan, request);
        plan.setUpdatedAt(Instant.now());

        plan = planRepository.save(plan);
        return mapToResponse(plan);
    }

    @Transactional
    public void deletePlan(UUID id) {
        if (!planRepository.existsById(id)) {
            throw new IllegalArgumentException("Plan not found");
        }
        planRepository.deleteById(id);
    }

    @Transactional
    public PlanResponse duplicatePlan(UUID id) {
        Plan original = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        Plan duplicate = new Plan();
        duplicate.setName(original.getName() + " (Copy)");
        duplicate.setSlug(original.getSlug() + "-copy-" + System.currentTimeMillis());
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

    private void updatePlanFromRequest(Plan plan, PlanRequest request) {
        plan.setName(request.getName());
        plan.setSlug(request.getSlug());
        plan.setShortDescription(request.getShortDescription());
        plan.setDescription(request.getDescription());
        plan.setImage(request.getImage());
        plan.setGallery(request.getGallery());
        plan.setPrice(request.getPrice());
        plan.setCompareAtPrice(request.getCompareAtPrice());
        if (request.getCurrency() != null) plan.setCurrency(request.getCurrency());
        plan.setDuration(request.getDuration());
        plan.setDurationUnit(request.getDurationUnit());
        plan.setMealCount(request.getMealCount());
        plan.setMealsPerDay(request.getMealsPerDay());
        plan.setServingsPerMeal(request.getServingsPerMeal());
        plan.setMealTypes(request.getMealTypes());
        plan.setFeatures(request.getFeatures());
        plan.setIngredients(request.getIngredients());
        plan.setNutrition(request.getNutrition());
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
