package com.ofood.serviceability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ServicePincodeIntegrationTest {

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

    private String adminToken;
    private String cityId;

    @BeforeEach
    void setup() throws Exception {
        pincodeRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();

        adminToken = createUserAndLogin("admin2@test.com", "ROLE_ADMIN");
        
        City city = new City();
        city.setName("Bangalore");
        city.setSlug("bangalore");
        city.setState("KA");
        city.setStatus("ACTIVE");
        city = cityRepository.save(city);
        cityId = city.getId().toString();
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
    void canCreateServiceAreaWithValidPolygon() throws Exception {
        String payload = """
        {
            "pincode": "560001",
            "cityId": "%s",
            "areaName": "MG Road",
            "isActive": true,
            "serviceArea": {
                "ring": [
                    [12.9715, 77.5945],
                    [12.9716, 77.5946],
                    [12.9714, 77.5947],
                    [12.9715, 77.5945]
                ]
            }
        }
        """.formatted(cityId);

        mockMvc.perform(post("/api/v1/pincodes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pincode").value("560001"))
                .andExpect(jsonPath("$.areaName").value("MG Road"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
                
        assertTrue(pincodeRepository.existsByCityIdAndPincodeAndAreaName(java.util.UUID.fromString(cityId), "560001", "MG Road"));
    }

    @Test
    void rejectsInvalidServiceArea() throws Exception {
        // Less than 3 points
        String payloadInvalidRing = """
        {
            "pincode": "560001",
            "cityId": "%s",
            "areaName": "Invalid Ring",
            "isActive": true,
            "serviceArea": {
                "ring": [
                    [12.9715, 77.5945],
                    [12.9716, 77.5946]
                ]
            }
        }
        """.formatted(cityId);

        mockMvc.perform(post("/api/v1/pincodes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalidRing))
                .andExpect(status().isBadRequest());
                
        // Lat > 90
        String payloadInvalidLat = """
        {
            "pincode": "560001",
            "cityId": "%s",
            "areaName": "Invalid Lat",
            "isActive": true,
            "serviceArea": {
                "ring": [
                    [100.0, 77.5945],
                    [12.9716, 77.5946],
                    [12.9714, 77.5946]
                ]
            }
        }
        """.formatted(cityId);

        mockMvc.perform(post("/api/v1/pincodes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalidLat))
                .andExpect(status().isBadRequest());
    }
}
