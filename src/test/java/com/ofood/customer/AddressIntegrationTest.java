package com.ofood.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
import com.ofood.customer.repository.AddressRepository;
import com.ofood.location.model.City;
import com.ofood.location.model.ServicePincode;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AddressIntegrationTest {

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
    private AddressRepository addressRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private ServicePincodeRepository pincodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String customer1Token;
    private String customer2Token;

    @BeforeEach
    void setup() throws Exception {
        addressRepository.deleteAll();
        pincodeRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();

        customer1Token = createUserAndLogin("c1@test.com", "ROLE_CUSTOMER");
        customer2Token = createUserAndLogin("c2@test.com", "ROLE_CUSTOMER");

        City city = new City();
        city.setName("Bangalore");
        city.setSlug("bangalore");
        city.setState("Karnataka");
        city.setStatus("ACTIVE");
        city = cityRepository.save(city);

        ServicePincode p1 = new ServicePincode();
        p1.setCity(city);
        p1.setPincode("560001");
        p1.setStatus("ACTIVE");
        pincodeRepository.save(p1);

        ServicePincode p2 = new ServicePincode();
        p2.setCity(city);
        p2.setPincode("560002");
        p2.setStatus("INACTIVE");
        pincodeRepository.save(p2);
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
    void testAddressLifecycleAndOwnership() throws Exception {
        // 1. Create first address (should become default)
        String req1 = """
        {
            "fullName": "Customer One",
            "addressLine1": "123 Main St",
            "pincode": "560001"
        }
        """;
        
        MvcResult res1 = mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true))
                .andExpect(jsonPath("$.city").value("Bangalore"))
                .andReturn();
                
        String addr1Id = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asText();

        // 2. Customer 2 tries to access Customer 1's address -> 400 (not found/ownership)
        mockMvc.perform(get("/api/v1/addresses/" + addr1Id)
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isBadRequest());

        // 3. Create second address for Customer 1 with isDefault = true
        String req2 = """
        {
            "fullName": "Customer One Home",
            "addressLine1": "456 Cross St",
            "pincode": "560001",
            "isDefault": true
        }
        """;
        
        MvcResult res2 = mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true))
                .andReturn();
                
        String addr2Id = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asText();

        // Verify Address 1 is no longer default
        mockMvc.perform(get("/api/v1/addresses/" + addr1Id)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(false));

        // 4. Try unserviceable pincode (not in DB)
        String req3 = """
        {
            "fullName": "Fail",
            "addressLine1": "Fail St",
            "pincode": "999999"
        }
        """;
        mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req3))
                .andExpect(status().isBadRequest());

        // 5. Try inactive pincode
        String req4 = """
        {
            "fullName": "Fail 2",
            "addressLine1": "Fail 2 St",
            "pincode": "560002"
        }
        """;
        mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req4))
                .andExpect(status().isBadRequest());

        // 6. Mark Address 1 as default again
        mockMvc.perform(post("/api/v1/addresses/" + addr1Id + "/default")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));

        mockMvc.perform(get("/api/v1/addresses/" + addr2Id)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(false));

        // 7. Delete Address 1
        mockMvc.perform(delete("/api/v1/addresses/" + addr1Id)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/v1/addresses/" + addr1Id)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isBadRequest());
    }
}
