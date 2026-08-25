package com.ofood.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
import com.ofood.catalog.repository.PlanRepository;
import com.ofood.role.Role;
import com.ofood.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PlanIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ofood")
            .withUsername("ofood")
            .withPassword("ofood");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("ofood.security.jwt.issuer", () -> "http://localhost:8080");
        registry.add("ofood.security.jwt.audience", () -> "ofood-api");
        registry.add("ofood.security.jwt.key-id", () -> "test-kid");
        registry.add("ofood.security.jwt.private-key-path",
                () -> "file:" + System.getProperty("user.dir") + "/src/test/resources/keys/test_private.pem");
        registry.add("ofood.auth.cookie.secure", () -> false);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlanRepository planRepository;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setup() throws Exception {
        planRepository.deleteAll();
        userRepository.deleteAll();

        adminToken = createUserAndLogin("admin2@test.com", "ROLE_ADMIN");
        customerToken = createUserAndLogin("customer2@test.com", "ROLE_CUSTOMER");
    }

    private String createUserAndLogin(String email, String roleName) throws Exception {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password@123"));
        user.setFullName(email);
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.getRoles().add(role);
        userRepository.save(user);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String createBaseDraft() throws Exception {
        String draftReq = """
        {
            "name": "Base Plan"
        }
        """;
        MvcResult result = mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftReq))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
    
    private String createActivePlan() throws Exception {
        String id = createBaseDraft();
        String fullUpdateReq = """
        {
            "price": 250,
            "currency": "INR",
            "duration": 30,
            "durationUnit": "DAYS",
            "mealCount": 90,
            "mealsPerDay": 3,
            "servingsPerMeal": 1,
            "mealTypes": ["BREAKFAST", "LUNCH", "DINNER"],
            "status": "ACTIVE"
        }
        """;
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fullUpdateReq))
                .andExpect(status().isOk());
        return id;
    }

    @Test
    void testPostWithoutNameRejected() throws Exception {
        mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
    
    @Test
    void testPostWithNullNameRejected() throws Exception {
        mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPostWithEmptyNameRejected() throws Exception {
        mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testPatchOmittedNamePreserved() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Base Plan"));
    }
    
    @Test
    void testPatchNameWithValidValueUpdatesAndSlugRegenerated() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Plan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Plan"))
                .andExpect(jsonPath("$.slug").value("new-plan"));
    }
    
    @Test
    void testPatchNameNullRejected() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
    
    @Test
    void testPatchNameBlankRejected() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testPatchOmittedPricePreserved() throws Exception {
        String id = createActivePlan();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration\": 60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(250.0))
                .andExpect(jsonPath("$.duration").value(60));
    }
    
    @Test
    void testPatchValidPriceUpdated() throws Exception {
        String id = createActivePlan();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(500.0));
    }
    
    @Test
    void testPatchPriceNullOnDraftCleared() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 100}")); // setup
                        
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").isEmpty());
    }
    
    @Test
    void testPatchDurationNullOnDraftCleared() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration\": 30}")); // setup
                        
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration").isEmpty());
    }
    
    @Test
    void testPatchNullableStringNullCleared() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"desc\"}"));
                        
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").isEmpty());
    }
    
    @Test
    void testNegativePriceRejected() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": -10}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testDurationZeroRejected() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration\": 0}"))
                .andExpect(status().isBadRequest());
    }
    

    @Test
    void testOmittedCollectionPreserved() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"features\": [\"A\"]}"));
                        
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features", hasSize(1)));
    }
    
    @Test
    void testCollectionEmptyArrayExplicitlyEmpty() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"features\": [\"A\"]}"));
                        
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"features\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features", hasSize(0)));
    }
    
    @Test
    void testCollectionNullExplicitlyNull() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"features\": [\"A\"]}"));
                        
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"features\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features").isEmpty());
    }
    
    @Test
    void testActiveClearRequiredFieldRemainingActiveRejected() throws Exception {
        String id = createActivePlan();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": null}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testDraftClearRequiredFieldAccepted() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 100}"));
                        
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").isEmpty());
    }
    
    @Test
    void testActiveClearRequiredFieldStatusDraftAccepted() throws Exception {
        String id = createActivePlan();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": null, \"status\": \"DRAFT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").isEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }
    
    @Test
    void testDraftClearRequiredFieldStatusActiveRejected() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": null, \"status\": \"ACTIVE\"}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testCompareAtPriceLessThanPriceRejected() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 500, \"compareAtPrice\": 400}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testCompareAtPriceWithPriceAbsentOnDraftHandled() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(patch("/api/v1/plans/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compareAtPrice\": 400}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compareAtPrice").value(400.0));
    }
    
    @Test
    void testCreateDirectlyAsActiveWithIncompleteDataRejected() throws Exception {
        String req = """
        {
            "name": "Active Plan",
            "status": "ACTIVE"
        }
        """;
        mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testDuplicatePlanGetsNewSlugAndDraft() throws Exception {
        String id = createActivePlan();
        mockMvc.perform(post("/api/v1/plans/" + id + "/duplicate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.slug").value("base-plan-copy"));
    }
    
    @Test
    void testPublicLookupCannotRetrieveDraftById() throws Exception {
        String id = createBaseDraft();
        mockMvc.perform(get("/api/v1/plans/" + id))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testPublicLookupCannotRetrieveDraftBySlug() throws Exception {
        createBaseDraft(); // base-plan
        mockMvc.perform(get("/api/v1/plans/slug/base-plan"))
                .andExpect(status().isNotFound());
    }
}