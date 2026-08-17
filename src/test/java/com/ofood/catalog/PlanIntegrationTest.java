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

    @Test
    void planCrudAndVisibilityWorks() throws Exception {
        String planReq = """
        {
            "name": "Weight Loss Plan",
            "slug": "weight-loss",
            "price": 1500.00,
            "duration": 30,
            "durationUnit": "days",
            "mealCount": 60,
            "mealsPerDay": 2,
            "servingsPerMeal": 1,
            "status": "ACTIVE",
            "meals": [
                {
                    "mealType": "LUNCH",
                    "name": "Healthy Lunch",
                    "calories": 500,
                    "ingredients": {"items": ["chicken", "rice"]}
                }
            ]
        }
        """;

        // Customer forbidden to create
        mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planReq))
                .andExpect(status().isForbidden());

        // Admin creates
        MvcResult result = mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planReq))
                .andExpect(status().isCreated())
                .andReturn();
                
        String planId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Public can get ACTIVE plans
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Weight Loss Plan"))
                .andExpect(jsonPath("$[0].meals[0].mealType").value("LUNCH"))
                .andExpect(jsonPath("$[0].meals[0].ingredients.items[0]").value("chicken"));

        // Admin updates status to DRAFT
        String updateReq = planReq.replace("\"ACTIVE\"", "\"DRAFT\"");
        mockMvc.perform(put("/api/v1/plans/" + planId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateReq))
                .andExpect(status().isOk());

        // Public GET should not return DRAFT plan
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Public GET by id should throw not found/inactive
        mockMvc.perform(get("/api/v1/plans/" + planId))
                .andExpect(status().isBadRequest());

        // Admin GET should see DRAFT plan
        mockMvc.perform(get("/api/v1/plans/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Admin duplicate plan
        mockMvc.perform(post("/api/v1/plans/" + planId + "/duplicate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Weight Loss Plan (Copy)"));
    }
}
