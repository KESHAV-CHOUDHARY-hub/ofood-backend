package com.ofood.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
import com.ofood.catalog.model.Plan;
import com.ofood.catalog.repository.PlanRepository;
import com.ofood.customer.model.Address;
import com.ofood.customer.repository.AddressRepository;
import com.ofood.location.model.City;
import com.ofood.location.repository.CityRepository;
import com.ofood.payment.dto.PaymentConfirmationRequest;
import com.ofood.payment.model.Payment;
import com.ofood.payment.model.PaymentStatus;
import com.ofood.payment.repository.PaymentRepository;
import com.ofood.security.jwt.JwtTokenService;
import com.ofood.role.Role;
import com.ofood.subscription.model.Subscription;
import com.ofood.subscription.model.SubscriptionStatus;
import com.ofood.subscription.repository.SubscriptionRepository;
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
public class PaymentIntegrationTest {

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
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String customerToken;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
        planRepository.deleteAll();
        addressRepository.deleteAll();
        cityRepository.deleteAll();

        Role role = new Role();
        role.setName("ROLE_CUSTOMER");
        User customer = new User();
        customer.setEmail("customer.payment@example.com");
        customer.setPasswordHash(passwordEncoder.encode("password"));
        customer.setStatus("ACTIVE");
        customer.setCreatedAt(java.time.Instant.now());
        customer.setUpdatedAt(java.time.Instant.now());
        customer = userRepository.save(customer);

        customerToken = jwtService.generateAccessToken(customer.getId(), java.util.UUID.randomUUID(), java.util.List.of("ROLE_CUSTOMER"));

        City city = new City();
        city.setName("Payment City");
        city.setStatus("ACTIVE");
        city = cityRepository.save(city);

        Address address = new Address();
        address.setCustomer(customer);
        address.setCity(city);
        address.setAddressLine1("123 Street");
        address.setPincode("111111");
        address = addressRepository.save(address);

        Plan plan = new Plan();
        plan.setName("Test Plan");
        plan.setSlug("test-plan-payment");
        plan.setPrice(new BigDecimal("1000.00"));
        plan.setDuration(1);
        plan.setDurationUnit("MONTH");
        plan.setMealCount(30);
        plan.setMealsPerDay(1);
        plan.setServingsPerMeal(1);
        plan.setStatus("ACTIVE");
        plan = planRepository.save(plan);

        Subscription subscription = new Subscription();
        subscription.setCustomer(customer);
        subscription.setPlan(plan);
        subscription.setAddress(address);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setPrice(new BigDecimal("1000.00"));
        subscription.setFinalAmount(new BigDecimal("1100.00"));
        subscription = subscriptionRepository.save(subscription);

        payment = new Payment();
        payment.setCustomer(customer);
        payment.setSubscription(subscription);
        payment.setAmount(new BigDecimal("1100.00"));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProvider("mock");
        payment.setProviderPaymentId("mock_pi_12345");
        payment = paymentRepository.save(payment);
    }

    @Test
    void testConfirmPaymentSuccess() throws Exception {
        PaymentConfirmationRequest request = new PaymentConfirmationRequest();
        request.setProviderPaymentId("mock_pi_12345");

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/confirm")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.subscriptionStatus").value("ACTIVE"));
    }

    @Test
    void testConfirmPaymentMismatchId() throws Exception {
        PaymentConfirmationRequest request = new PaymentConfirmationRequest();
        request.setProviderPaymentId("wrong_id");

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/confirm")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testConfirmPaymentDuplicate() throws Exception {
        // Confirm first time
        PaymentConfirmationRequest request = new PaymentConfirmationRequest();
        request.setProviderPaymentId("mock_pi_12345");

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/confirm")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Confirm second time should be idempotent
        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/confirm")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
    }
}
