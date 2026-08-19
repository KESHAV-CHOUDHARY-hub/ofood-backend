package com.ofood.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
import com.ofood.catalog.model.Plan;
import com.ofood.catalog.repository.PlanRepository;
import com.ofood.checkout.dto.CheckoutPreviewRequest;
import com.ofood.customer.model.Address;
import com.ofood.customer.repository.AddressRepository;
import com.ofood.location.model.City;
import com.ofood.location.repository.CityRepository;
import com.ofood.security.jwt.JwtTokenService;
import com.ofood.role.Role;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CheckoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private com.ofood.payment.repository.PaymentRepository paymentRepository;

    @Autowired
    private com.ofood.subscription.repository.SubscriptionRepository subscriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.ofood.payment.repository.PaymentTransactionRepository paymentTransactionRepository;

    private String customerToken;
    private Plan plan;
    private Address address;

    @BeforeEach
    void setUp() {
        paymentTransactionRepository.deleteAll();
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
        planRepository.deleteAll();
        addressRepository.deleteAll();
        cityRepository.deleteAll();

        Role role = new Role();
        role.setName("ROLE_CUSTOMER");
        // Role might need to be saved if it's managed, but test might allow transient or we can mock/save.
        // Actually, we should autowire RoleRepository and find/create it, like AddressIntegrationTest.
        // I will just use setPasswordHash for now and add a transient role or see if it works without roles since we mock JWT.
        User customer = new User();
        customer.setEmail("customer.checkout@example.com");
        customer.setPasswordHash(passwordEncoder.encode("password"));
        customer.setStatus("ACTIVE");
        customer.setCreatedAt(java.time.Instant.now());
        customer.setUpdatedAt(java.time.Instant.now());
        customer = userRepository.save(customer);

        customerToken = jwtService.generateAccessToken(customer.getId(), java.util.UUID.randomUUID(), java.util.List.of("ROLE_CUSTOMER"));

        City city = new City();
        city.setName("Checkout City");
        city.setSlug("checkout-city-" + java.util.UUID.randomUUID().toString());
        city.setState("Test State");
        city.setStatus("ACTIVE");
        city = cityRepository.save(city);

        City inactiveCity = new City();
        inactiveCity.setName("Inactive City");
        inactiveCity.setSlug("inactive-city-" + java.util.UUID.randomUUID().toString());
        inactiveCity.setState("Test State");
        inactiveCity.setStatus("INACTIVE");
        inactiveCity = cityRepository.save(inactiveCity);

        address = new Address();
        address.setCustomer(customer);
        address.setCity(city);
        address.setAddressLine1("123 Street");
        address.setPincode("111111");
        address = addressRepository.save(address);

        plan = new Plan();
        plan.setName("Test Plan");
        plan.setSlug("test-plan-checkout");
        plan.setPrice(new BigDecimal("1000.00"));
        plan.setDuration(1);
        plan.setDurationUnit("MONTH");
        plan.setMealCount(30);
        plan.setMealsPerDay(1);
        plan.setServingsPerMeal(1);
        plan.setStatus("ACTIVE");
        plan = planRepository.save(plan);
    }

    @Test
    void testPreviewCheckout() throws Exception {
        CheckoutPreviewRequest request = new CheckoutPreviewRequest();
        request.setPlanId(plan.getId());
        request.setAddressId(address.getId());

        mockMvc.perform(post("/api/v1/checkout/preview")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planPrice").value(1000.0))
                .andExpect(jsonPath("$.finalAmount").value(1100.0));
    }

    @Test
    void testCheckout() throws Exception {
        CheckoutPreviewRequest request = new CheckoutPreviewRequest();
        request.setPlanId(plan.getId());
        request.setAddressId(address.getId());

        mockMvc.perform(post("/api/v1/checkout")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").exists())
                .andExpect(jsonPath("$.subscriptionStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.providerPaymentId").exists());
    }
}
