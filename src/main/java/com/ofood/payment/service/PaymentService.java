package com.ofood.payment.service;

import com.ofood.auth.model.User;
import com.ofood.payment.dto.PaymentConfirmationRequest;
import com.ofood.payment.dto.PaymentConfirmationResponse;
import com.ofood.payment.model.Payment;
import com.ofood.payment.model.PaymentStatus;
import com.ofood.payment.model.PaymentTransaction;
import com.ofood.payment.repository.PaymentRepository;
import com.ofood.payment.repository.PaymentTransactionRepository;
import com.ofood.subscription.model.Subscription;
import com.ofood.subscription.model.SubscriptionStatus;
import com.ofood.subscription.repository.SubscriptionRepository;
import com.ofood.voucher.model.VoucherUsage;
import com.ofood.voucher.repository.VoucherRepository;
import com.ofood.voucher.repository.VoucherUsageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentProvider paymentProvider;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    public PaymentService(PaymentRepository paymentRepository, PaymentTransactionRepository paymentTransactionRepository,
                          SubscriptionRepository subscriptionRepository, PaymentProvider paymentProvider,
                          VoucherRepository voucherRepository, VoucherUsageRepository voucherUsageRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentProvider = paymentProvider;
        this.voucherRepository = voucherRepository;
        this.voucherUsageRepository = voucherUsageRepository;
    }

    @Transactional
    public PaymentConfirmationResponse confirmPayment(UUID paymentId, PaymentConfirmationRequest request, User customer) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (!payment.getCustomer().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment does not belong to the authenticated user");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            // Idempotent success return
            return buildResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is in invalid state for confirmation: " + payment.getStatus());
        }

        if (!request.getProviderPaymentId().equals(payment.getProviderPaymentId())) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider payment ID mismatch");
        }

        boolean verified = paymentProvider.verifyPayment(request.getProviderPaymentId());

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPayment(payment);
        transaction.setType("CONFIRMATION");
        
        if (verified) {
            transaction.setStatus("SUCCESS");
            payment.setStatus(PaymentStatus.SUCCESS);

            Subscription subscription = payment.getSubscription();
            if (subscription.getStatus() == SubscriptionStatus.PENDING) {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setStartDate(Instant.now());
                subscriptionRepository.save(subscription);
                
                // Enforce Voucher Concurrency
                if (subscription.getVoucher() != null) {
                    // Check per-customer usage
                    if (subscription.getVoucher().getUsagePerCustomer() != null) {
                        long usages = voucherUsageRepository.countByVoucherIdAndCustomerId(subscription.getVoucher().getId(), customer.getId());
                        if (usages >= subscription.getVoucher().getUsagePerCustomer()) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher usage per customer limit exceeded");
                        }
                    }

                    // Attempt atomic global increment
                    int updated = voucherRepository.incrementUsedCountIfAllowed(subscription.getVoucher().getId());
                    if (updated == 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher global usage limit exceeded");
                    }

                    // Record usage
                    VoucherUsage usage = new VoucherUsage();
                    usage.setVoucher(subscription.getVoucher());
                    usage.setCustomer(customer);
                    usage.setSubscription(subscription);
                    voucherUsageRepository.save(usage);
                }
            }
        } else {
            transaction.setStatus("FAILED");
            payment.setStatus(PaymentStatus.FAILED);
            // Optionally set Subscription to CANCELLED or leave PENDING to allow retry
        }

        paymentRepository.save(payment);
        paymentTransactionRepository.save(transaction);

        if (!verified) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed with provider");
        }

        return buildResponse(payment);
    }

    private PaymentConfirmationResponse buildResponse(Payment payment) {
        PaymentConfirmationResponse response = new PaymentConfirmationResponse();
        response.setPaymentId(payment.getId());
        response.setPaymentStatus(payment.getStatus().name());
        response.setSubscriptionId(payment.getSubscription().getId());
        response.setSubscriptionStatus(payment.getSubscription().getStatus().name());
        return response;
    }
}
