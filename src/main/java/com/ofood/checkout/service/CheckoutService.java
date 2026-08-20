package com.ofood.checkout.service;

import com.ofood.auth.model.User;
import com.ofood.catalog.model.Plan;
import com.ofood.catalog.repository.PlanRepository;
import com.ofood.checkout.dto.CheckoutPreviewRequest;
import com.ofood.checkout.dto.CheckoutPreviewResponse;
import com.ofood.checkout.dto.CheckoutResponse;
import com.ofood.customer.model.Address;
import com.ofood.customer.repository.AddressRepository;
import com.ofood.payment.model.Payment;
import com.ofood.payment.model.PaymentStatus;
import com.ofood.payment.repository.PaymentRepository;
import com.ofood.payment.service.PaymentProvider;
import com.ofood.subscription.model.Subscription;
import com.ofood.subscription.model.SubscriptionStatus;
import com.ofood.subscription.repository.SubscriptionRepository;
import com.ofood.voucher.model.Voucher;
import com.ofood.voucher.repository.VoucherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutService {

    private final PlanRepository planRepository;
    private final AddressRepository addressRepository;
    private final VoucherRepository voucherRepository;
    private final PricingEngineService pricingEngineService;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;

    public CheckoutService(PlanRepository planRepository, AddressRepository addressRepository,
                           VoucherRepository voucherRepository, PricingEngineService pricingEngineService,
                           SubscriptionRepository subscriptionRepository, PaymentRepository paymentRepository,
                           PaymentProvider paymentProvider) {
        this.planRepository = planRepository;
        this.addressRepository = addressRepository;
        this.voucherRepository = voucherRepository;
        this.pricingEngineService = pricingEngineService;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse previewCheckout(CheckoutPreviewRequest request, User customer) {
        Plan plan = validatePlan(request);
        Address address = validateAddress(request, customer);
        Voucher voucher = validateVoucher(request);

        return pricingEngineService.calculatePricing(plan, voucher);
    }

    @Transactional
    public CheckoutResponse checkout(CheckoutPreviewRequest request, User customer) {
        Plan plan = validatePlan(request);
        Address address = validateAddress(request, customer);
        Voucher voucher = validateVoucher(request);

        CheckoutPreviewResponse pricing = pricingEngineService.calculatePricing(plan, voucher);

        // Create PENDING Subscription
        Subscription subscription = new Subscription();
        subscription.setCustomer(customer);
        subscription.setPlan(plan);
        subscription.setAddress(address);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setPrice(pricing.getPlanPrice());
        subscription.setPlanDiscount(pricing.getPlanDiscount());
        subscription.setVoucherDiscount(pricing.getVoucherDiscount());
        subscription.setTax(pricing.getTax());
        subscription.setDeliveryFee(pricing.getDeliveryFee());
        subscription.setFinalAmount(pricing.getFinalAmount());
        if (voucher != null) {
            subscription.setVoucher(voucher);
        }
        subscription = subscriptionRepository.save(subscription);

        // Create PENDING Payment
        Payment payment = new Payment();
        payment.setCustomer(customer);
        payment.setSubscription(subscription);
        payment.setAmount(pricing.getFinalAmount());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProvider("mock");
        payment = paymentRepository.save(payment);

        // Invoke PaymentProvider
        String providerPaymentId = paymentProvider.createPaymentIntent(payment.getAmount(), payment.getCurrency(), payment.getId().toString());
        payment.setProviderPaymentId(providerPaymentId);
        paymentRepository.save(payment);

        // Build Response
        CheckoutResponse response = new CheckoutResponse();
        response.setSubscriptionId(subscription.getId());
        response.setSubscriptionStatus(subscription.getStatus().name());
        response.setPaymentId(payment.getId());
        response.setPaymentStatus(payment.getStatus().name());
        response.setProvider(payment.getProvider());
        response.setProviderPaymentId(payment.getProviderPaymentId());
        response.setPricingDetails(pricing);

        return response;
    }

    private Plan validatePlan(CheckoutPreviewRequest request) {
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        
        if (!"ACTIVE".equals(plan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan is not active");
        }
        return plan;
    }

    private Address validateAddress(CheckoutPreviewRequest request, User customer) {
        Address address = addressRepository.findByIdAndCustomerIdWithCity(request.getAddressId(), customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Address not found or does not belong to the authenticated user"));

        // TODO: Validate serviceability (e.g. check if city/pincode is active)
        // Currently relying on existing Address model which links to City. 
        if (address.getCity() != null && !"ACTIVE".equals(address.getCity().getStatus())) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "City is not serviceable");
        }
        
        return address;
    }

    private Voucher validateVoucher(CheckoutPreviewRequest request) {
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            return voucherRepository.findByCode(request.getVoucherCode())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));
        }
        return null;
    }
}
