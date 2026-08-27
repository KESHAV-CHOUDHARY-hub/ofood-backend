package com.ofood.serviceability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
import com.ofood.delivery.repository.DeliveryPersonRepository;
import com.ofood.location.repository.CityRepository;
import com.ofood.location.repository.ServicePincodeRepository;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ServiceabilityIntegrationTest {

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
    private CityRepository cityRepository;

    @Autowired
    private ServicePincodeRepository pincodeRepository;

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setup() throws Exception {
        deliveryPersonRepository.deleteAll();
        pincodeRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();

        adminToken = createUserAndLogin("admin@test.com", "ROLE_ADMIN");
        customerToken = createUserAndLogin("customer@test.com", "ROLE_CUSTOMER");
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
    void citiesCrudWorksForAdminAndPublicGetWorks() throws Exception {
        // Customer cannot create city
        mockMvc.perform(post("/api/v1/cities")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mumbai\",\"slug\":\"mumbai\",\"state\":\"MH\"}"))
                .andExpect(status().isForbidden());

        // Admin can create city
        MvcResult result = mockMvc.perform(post("/api/v1/cities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mumbai\",\"slug\":\"mumbai\",\"state\":\"MH\"}"))
                .andExpect(status().isCreated())
                .andReturn();
                
        String cityId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Public can get active cities
        mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mumbai"));

        // Admin can update city
        mockMvc.perform(put("/api/v1/cities/" + cityId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mumbai New\",\"slug\":\"mumbai\",\"state\":\"MH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mumbai New"));

        // Admin can delete city
        mockMvc.perform(delete("/api/v1/cities/" + cityId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void pincodesCrudAndServiceabilityLookup() throws Exception {
        // Create city
        MvcResult cityResult = mockMvc.perform(post("/api/v1/cities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pune\",\"slug\":\"pune\",\"state\":\"MH\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String cityId = objectMapper.readTree(cityResult.getResponse().getContentAsString()).get("id").asText();

        // Admin creates pincode
        MvcResult pincodeResult = mockMvc.perform(post("/api/v1/pincodes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pincode\":\"411001\",\"cityId\":\"" + cityId + "\",\"areaName\":\"Pune Area\",\"isActive\":true,\"serviceArea\":{\"ring\":[[0,0],[1,0],[1,1]]}}"))
                .andExpect(status().isCreated())
                .andReturn();
                
        String pincodeId = objectMapper.readTree(pincodeResult.getResponse().getContentAsString()).get("id").asText();

        // Serviceability lookup - Public
        mockMvc.perform(get("/api/v1/serviceability?pincode=411001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isServiceable").value(true))
                .andExpect(jsonPath("$.cityName").value("Pune"));

        // Non-existent pincode
        mockMvc.perform(get("/api/v1/serviceability?pincode=999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isServiceable").value(false));
                
        // Inactive pincode
        mockMvc.perform(put("/api/v1/pincodes/" + pincodeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pincode\":\"411001\",\"cityId\":\"" + cityId + "\",\"areaName\":\"Pune Area\",\"isActive\":false,\"serviceArea\":{\"ring\":[[0,0],[1,0],[1,1]]}}"))
                .andExpect(status().isOk());
                
        mockMvc.perform(get("/api/v1/serviceability?pincode=411001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isServiceable").value(false));
    }

    @Test
    void deliveryPersonCrudByAdminOnly() throws Exception {
        String reqBody = "{\"firstName\":\"Raju\",\"lastName\":\"Bhai\",\"mobile\":\"9876543210\",\"vehicleType\":\"BIKE\",\"vehicleNumber\":\"MH12AB1234\"}";
        
        // Customer forbidden
        mockMvc.perform(post("/api/v1/delivery-persons")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isForbidden());

        // Admin allowed
        MvcResult dpResult = mockMvc.perform(post("/api/v1/delivery-persons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isCreated())
                .andReturn();
                
        String dpId = objectMapper.readTree(dpResult.getResponse().getContentAsString()).get("id").asText();
        
        // Verify GET
        mockMvc.perform(get("/api/v1/delivery-persons/" + dpId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Raju"));
    }
}
