import re

with open('src/main/java/com/ofood/catalog/service/PlanService.java', 'r') as f:
    content = f.read()

# 1. Add generateSlug method
generate_slug_code = """
    private String generateSlug(String name) {
        if (name == null || name.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        String baseSlug = name.toLowerCase().replaceAll("[^a-z0-9\\\\s-]", "").replaceAll("\\\\s+", "-");
        String slug = baseSlug;
        int counter = 2;
        while (planRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }

    private void validateForActivation(Plan plan, PlanRequest request, String status) {
        if ("ACTIVE".equals(status)) {
            List<String> missingFields = new ArrayList<>();
            String name = request.getName() != null ? request.getName() : plan.getName();
            BigDecimal price = request.getPrice() != null ? request.getPrice() : plan.getPrice();
            String currency = request.getCurrency() != null ? request.getCurrency() : plan.getCurrency();
            Integer duration = request.getDuration() != null ? request.getDuration() : plan.getDuration();
            String durationUnit = request.getDurationUnit() != null ? request.getDurationUnit() : plan.getDurationUnit();
            Integer mealCount = request.getMealCount() != null ? request.getMealCount() : plan.getMealCount();
            Integer mealsPerDay = request.getMealsPerDay() != null ? request.getMealsPerDay() : plan.getMealsPerDay();
            Integer servingsPerMeal = request.getServingsPerMeal() != null ? request.getServingsPerMeal() : plan.getServingsPerMeal();
            JsonNode mealTypes = request.getMealTypes() != null ? request.getMealTypes() : plan.getMealTypes();

            if (name == null || name.trim().isEmpty()) missingFields.add("name");
            if (price == null) missingFields.add("price");
            if (currency == null || currency.trim().isEmpty()) missingFields.add("currency");
            if (duration == null) missingFields.add("duration");
            if (durationUnit == null || durationUnit.trim().isEmpty()) missingFields.add("durationUnit");
            if (mealCount == null) missingFields.add("mealCount");
            if (mealsPerDay == null) missingFields.add("mealsPerDay");
            if (servingsPerMeal == null) missingFields.add("servingsPerMeal");
            if (mealTypes == null || mealTypes.isEmpty()) missingFields.add("mealTypes");

            if (!missingFields.isEmpty()) {
                throw new IllegalArgumentException("Cannot activate plan. Missing required fields: " + String.join(", ", missingFields));
            }
        }
    }
"""

content = content.replace("private void updatePlanFromRequest(Plan plan, PlanRequest request) {", generate_slug_code + "    private void updatePlanFromRequest(Plan plan, PlanRequest request) {")

# 2. Update createPlan
create_plan_old = """    @Transactional
    public PlanResponse createPlan(PlanRequest request) {
        if (planRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Plan slug already exists");
        }
        Plan plan = new Plan();
        updatePlanFromRequest(plan, request);
        plan = planRepository.save(plan);
        return mapToResponse(plan);
    }"""

create_plan_new = """    @Transactional
    public PlanResponse createPlan(PlanRequest request) {
        Plan plan = new Plan();
        String status = request.getStatus() != null ? request.getStatus() : "DRAFT";
        validateForActivation(plan, request, status);
        
        plan.setSlug(generateSlug(request.getName()));
        updatePlanFromRequest(plan, request);
        plan.setStatus(status);
        
        plan = planRepository.save(plan);
        return mapToResponse(plan);
    }"""
content = content.replace(create_plan_old, create_plan_new)

# 3. Update updatePlan
update_plan_old = """    @Transactional
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
    }"""

update_plan_new = """    @Transactional
    public PlanResponse updatePlan(UUID id, PlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        String newStatus = request.getStatus() != null ? request.getStatus() : plan.getStatus();
        validateForActivation(plan, request, newStatus);
        
        if (request.getName() != null && !request.getName().equals(plan.getName())) {
            plan.setSlug(generateSlug(request.getName()));
        }
        
        if (request.getMeals() != null) {
            plan.getMeals().clear();
        }
        
        updatePlanFromRequest(plan, request);
        
        plan.setUpdatedAt(Instant.now());

        plan = planRepository.save(plan);
        return mapToResponse(plan);
    }"""
content = content.replace(update_plan_old, update_plan_new)

# 4. duplicatePlan slug generation
dup_plan_old = 'duplicate.setSlug(original.getSlug() + "-copy-" + System.currentTimeMillis());'
dup_plan_new = 'duplicate.setSlug(generateSlug(duplicate.getName()));'
content = content.replace(dup_plan_old, dup_plan_new)

# 5. updatePlanFromRequest - partial updates + new fields
update_req_old = """    private void updatePlanFromRequest(Plan plan, PlanRequest request) {
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
    }"""

update_req_new = """    private void updatePlanFromRequest(Plan plan, PlanRequest request) {
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
    }"""
content = content.replace(update_req_old, update_req_new)

# 6. mapToResponse - new fields
map_res_old = """        response.setNutrition(plan.getNutrition());
        response.setStatus(plan.getStatus());"""
map_res_new = """        response.setNutrition(plan.getNutrition());
        response.setCaloriesLabel(plan.getCaloriesLabel());
        response.setDeliveryInformation(plan.getDeliveryInformation());
        response.setTerms(plan.getTerms());
        response.setSeoTitle(plan.getSeoTitle());
        response.setSeoDescription(plan.getSeoDescription());
        response.setStatus(plan.getStatus());"""
content = content.replace(map_res_old, map_res_new)

# 7. Add BigDecimal import if missing, wait it's there. 

with open('src/main/java/com/ofood/catalog/service/PlanService.java', 'w') as f:
    f.write(content)
