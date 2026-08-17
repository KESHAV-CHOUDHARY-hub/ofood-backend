package com.ofood.voucher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
import com.ofood.catalog.model.Plan;
import com.ofood.catalog.repository.PlanRepository;
import com.ofood.role.Role;
import com.ofood.role.repository.RoleRepository;
import com.ofood.voucher.repository.VoucherRepository;
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

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class VoucherIntegrationTest {

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
    private VoucherRepository voucherRepository;
    
    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setup() throws Exception {
        voucherRepository.deleteAll();
        planRepository.deleteAll();
        userRepository.deleteAll();

        adminToken = createUserAndLogin("admin_v@test.com", "ROLE_ADMIN");
        customerToken = createUserAndLogin("customer_v@test.com", "ROLE_CUSTOMER");
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
    void testVoucherAdminOperations() throws Exception {
        String createReq = """
        {
            "code": "SUMMER10",
            "name": "Summer Discount",
            "discountType": "PERCENTAGE",
            "discountValue": 10.0,
            "maxDiscount": 50.0,
            "status": "ACTIVE"
        }
        """;

        // CUSTOMER cannot create -> 403
        mockMvc.perform(post("/api/v1/vouchers")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReq))
                .andExpect(status().isForbidden());

        // ADMIN creates
        MvcResult res = mockMvc.perform(post("/api/v1/vouchers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReq))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUMMER10"))
                .andReturn();

        String voucherId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();

        // CUSTOMER cannot list all (admin only) -> 403
        mockMvc.perform(get("/api/v1/vouchers")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // ADMIN lists all
        mockMvc.perform(get("/api/v1/vouchers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // ADMIN updates
        String updateReq = createReq.replace("\"PERCENTAGE\"", "\"FIXED_AMOUNT\"");
        mockMvc.perform(put("/api/v1/vouchers/" + voucherId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountType").value("FIXED_AMOUNT"));

        // ADMIN deletes
        mockMvc.perform(delete("/api/v1/vouchers/" + voucherId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/v1/vouchers/" + voucherId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testVoucherCustomerValidation() throws Exception {
        // Setup Plan
        Plan p = new Plan();
        p.setName("Basic Plan");
        p.setSlug("basic-plan");
        p.setPrice(new BigDecimal("1000.00"));
        p.setCurrency("INR");
        p.setDuration(30);
        p.setDurationUnit("days");
        p.setMealCount(30);
        p.setMealsPerDay(1);
        p.setServingsPerMeal(1);
        p.setStatus("ACTIVE");
        p = planRepository.save(p);
        String planId = p.getId().toString();

        // Setup Voucher
        String createReq = """
        {
            "code": "HELLO50",
            "name": "Welcome Bonus",
            "discountType": "FIXED_AMOUNT",
            "discountValue": 50.0,
            "minimumOrderValue": 500.0,
            "status": "ACTIVE",
            "applicablePlanIds": ["%s"]
        }
        """.formatted(planId);

        mockMvc.perform(post("/api/v1/vouchers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReq))
                .andExpect(status().isCreated());

        // Customer gets by code
        mockMvc.perform(get("/api/v1/vouchers/code/HELLO50")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Welcome Bonus"))
                .andExpect(jsonPath("$.usedCount").doesNotExist()); // Ensure usedCount is hidden

        // Customer validates valid order
        String valReq = """
        {
            "code": "HELLO50",
            "planId": "%s",
            "orderValue": 1000.0
        }
        """.formatted(planId);
        
        mockMvc.perform(post("/api/v1/vouchers/validate")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountAmount").value(50.0))
                .andExpect(jsonPath("$.finalAmount").value(950.0));

        // Customer validates order below minimum -> rejected
        String valReq2 = """
        {
            "code": "HELLO50",
            "planId": "%s",
            "orderValue": 400.0
        }
        """.formatted(planId);
        
        mockMvc.perform(post("/api/v1/vouchers/validate")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valReq2))
                .andExpect(status().isOk()) // returns 200 with isValid=false per service design
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Minimum order value not met"));

        // Customer validates non-applicable plan -> rejected
        Plan p2 = new Plan();
        p2.setName("Premium Plan");
        p2.setSlug("premium-plan");
        p2.setPrice(new BigDecimal("2000.00"));
        p2.setDuration(30);
        p2.setDurationUnit("days");
        p2.setMealCount(30);
        p2.setMealsPerDay(1);
        p2.setServingsPerMeal(1);
        p2.setStatus("ACTIVE");
        p2 = planRepository.save(p2);
        
        String valReq3 = """
        {
            "code": "HELLO50",
            "planId": "%s",
            "orderValue": 2000.0
        }
        """.formatted(p2.getId().toString());
        
        mockMvc.perform(post("/api/v1/vouchers/validate")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valReq3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Voucher is not applicable to this plan"));
    }
}
