import re

with open('src/test/java/com/ofood/catalog/PlanIntegrationTest.java', 'r') as f:
    content = f.read()

new_tests = """
    @Test
    void planDraftCreationAndSequentialSlugAndPartialUpdate() throws Exception {
        // 1. Draft Creation & Sequential Slug Generation
        String draftReq = \"\"\"
        {
            "name": "Weight Loss Plan"
        }
        \"\"\";

        // Admin creates draft 1
        MvcResult result1 = mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftReq))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.slug").value("weight-loss"))
                .andReturn();
        String planId1 = objectMapper.readTree(result1.getResponse().getContentAsString()).get("id").asText();

        // Admin creates draft 2 (same name -> sequential slug)
        mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftReq))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("weight-loss-2"));

        // 2. Partial Update
        String patchReq = \"\"\"
        {
            "price": 1500.00,
            "currency": "USD"
        }
        \"\"\";

        mockMvc.perform(patch("/api/v1/plans/" + planId1)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(1500.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.name").value("Weight Loss Plan"));

        // 3. Validation failure on activation
        String activateReq = \"\"\"
        {
            "status": "ACTIVE"
        }
        \"\"\";

        mockMvc.perform(patch("/api/v1/plans/" + planId1)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activateReq))
                .andExpect(status().isBadRequest()); // assuming ExceptionHandler maps IllegalArgumentException to 400

        // 4. Successful activation
        String fullUpdateReq = \"\"\"
        {
            "duration": 30,
            "durationUnit": "days",
            "mealCount": 60,
            "mealsPerDay": 2,
            "servingsPerMeal": 1,
            "mealTypes": {"types": ["LUNCH"]},
            "status": "ACTIVE"
        }
        \"\"\";

        mockMvc.perform(patch("/api/v1/plans/" + planId1)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fullUpdateReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verify public endpoint DOES return ACTIVE
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
"""

content = re.sub(r'    @Test\n    void planCrudAndVisibilityWorks\(\) throws Exception \{.*$', new_tests + '}', content, flags=re.DOTALL)

with open('src/test/java/com/ofood/catalog/PlanIntegrationTest.java', 'w') as f:
    f.write(content)
