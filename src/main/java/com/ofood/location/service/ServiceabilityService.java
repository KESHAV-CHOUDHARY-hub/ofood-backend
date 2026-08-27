package com.ofood.location.service;

import com.ofood.location.dto.ServicePincodeResponse;
import com.ofood.location.model.ServicePincode;
import com.ofood.location.repository.ServicePincodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceabilityService {
    private final ServicePincodeRepository pincodeRepository;

    public ServiceabilityService(ServicePincodeRepository pincodeRepository) {
        this.pincodeRepository = pincodeRepository;
    }

    @Transactional(readOnly = true)
    public ServiceabilityResponse checkServiceability(String pincode) {
        List<ServicePincode> pincodes = pincodeRepository.findByPincode(pincode);
        for (ServicePincode sp : pincodes) {
            if ("ACTIVE".equalsIgnoreCase(sp.getStatus()) && "ACTIVE".equalsIgnoreCase(sp.getCity().getStatus())) {
                return new ServiceabilityResponse(
                        true,
                        pincode,
                        sp.getCity().getName(),
                        sp.getCity().getState()
                );
            }
        }
        if (!pincodes.isEmpty()) {
            ServicePincode sp = pincodes.get(0);
            return new ServiceabilityResponse(false, pincode, sp.getCity().getName(), sp.getCity().getState());
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
