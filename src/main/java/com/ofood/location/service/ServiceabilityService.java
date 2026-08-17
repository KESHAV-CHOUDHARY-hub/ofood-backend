package com.ofood.location.service;

import com.ofood.location.dto.ServicePincodeResponse;
import com.ofood.location.model.ServicePincode;
import com.ofood.location.repository.ServicePincodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ServiceabilityService {
    private final ServicePincodeRepository pincodeRepository;

    public ServiceabilityService(ServicePincodeRepository pincodeRepository) {
        this.pincodeRepository = pincodeRepository;
    }

    @Transactional(readOnly = true)
    public ServiceabilityResponse checkServiceability(String pincode) {
        Optional<ServicePincode> optionalPincode = pincodeRepository.findByPincode(pincode);
        if (optionalPincode.isPresent()) {
            ServicePincode sp = optionalPincode.get();
            boolean isServiceable = "ACTIVE".equalsIgnoreCase(sp.getStatus()) && "ACTIVE".equalsIgnoreCase(sp.getCity().getStatus());
            return new ServiceabilityResponse(
                    isServiceable,
                    pincode,
                    sp.getCity().getName(),
                    sp.getCity().getState()
            );
        }
        return new ServiceabilityResponse(false, pincode, null, null);
    }

    public record ServiceabilityResponse(
            boolean isServiceable,
            String pincode,
            String cityName,
            String state
    ) {}
}
