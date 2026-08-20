package com.ofood.subscription;

import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
import com.ofood.catalog.model.Plan;
import com.ofood.catalog.repository.PlanRepository;
import com.ofood.customer.model.Address;
import com.ofood.customer.repository.AddressRepository;
import com.ofood.location.model.City;
import com.ofood.location.repository.CityRepository;
import com.ofood.security.jwt.JwtTokenService;
import com.ofood.subscription.model.Subscription;
import com.ofood.subscription.model.SubscriptionStatus;
import com.ofood.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SubscriptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private JwtTokenService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String customerToken;
    private User customer;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        addressRepository.deleteAll();
        cityRepository.deleteAll();
        planRepository.deleteAll();
        userRepository.deleteAll();

        customer = new User();
        customer.setEmail("sub.customer@example.com");
        customer.setPasswordHash(passwordEncoder.encode("password"));
        customer.setStatus("ACTIVE");
        customer.setCreatedAt(java.time.Instant.now());
        customer.setUpdatedAt(java.time.Instant.now());
        customer = userRepository.save(customer);

        customerToken = jwtService.generateAccessToken(customer.getId(), UUID.randomUUID(), List.of("ROLE_CUSTOMER"));

        City city = new City();
        city.setName("Sub City");
        city.setSlug("sub-city-" + UUID.randomUUID());
        city.setState("Test State");
        city.setStatus("ACTIVE");
        city = cityRepository.save(city);

        Address address = new Address();
        address.setCustomer(customer);
        address.setCity(city);
        address.setAddressLine1("123 Street");
        address.setAddressType("HOME");
        address.setFullName("John Doe");
        address.setMobile("1234567890");
        address.setPincode("123456");
        address = addressRepository.save(address);

        Plan plan = new Plan();
        plan.setName("Test Plan");
        plan.setDescription("Test Plan Desc");
        plan.setDuration(30);
        plan.setDurationUnit("DAYS");
        plan.setMealCount(60);
        plan.setMealsPerDay(2);
        plan.setServingsPerMeal(1);
        plan.setPrice(new BigDecimal("4999.00"));
        plan.setStatus("ACTIVE");
        plan.setSlug("test-plan-" + UUID.randomUUID());
        plan = planRepository.save(plan);

        subscription = new Subscription();
        subscription.setCustomer(customer);
        subscription.setPlan(plan);
        subscription.setAddress(address);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setPrice(new BigDecimal("4999.00"));
        subscription.setTax(new BigDecimal("249.95"));
        subscription.setFinalAmount(new BigDecimal("5248.95"));
        subscription = subscriptionRepository.save(subscription);
    }

    @AfterEach
    void tearDown() {
        subscriptionRepository.deleteAll();
        addressRepository.deleteAll();
        cityRepository.deleteAll();
        planRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testGetSubscriptions_Success() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(subscription.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].planId").value(subscription.getPlan().getId().toString()))
                .andExpect(jsonPath("$[0].addressId").value(subscription.getAddress().getId().toString()))
                .andExpect(jsonPath("$[0].customer").doesNotExist()); // Ensure no lazy relations serialized
    }

    @Test
    void testGetSubscriptionById_Success() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/" + subscription.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(subscription.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetSubscriptions_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testGetSubscriptionById_ForbiddenOtherCustomer() throws Exception {
        User otherCustomer = new User();
        otherCustomer.setEmail("other.customer@example.com");
        otherCustomer.setPasswordHash("hash");
        otherCustomer.setStatus("ACTIVE");
        otherCustomer.setCreatedAt(java.time.Instant.now());
        otherCustomer.setUpdatedAt(java.time.Instant.now());
        otherCustomer = userRepository.save(otherCustomer);
        
        String otherToken = jwtService.generateAccessToken(otherCustomer.getId(), UUID.randomUUID(), List.of("ROLE_CUSTOMER"));
        
        mockMvc.perform(get("/api/v1/subscriptions/" + subscription.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }
}
