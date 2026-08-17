package com.ofood.delivery.service;

import com.ofood.delivery.dto.DeliveryPersonRequest;
import com.ofood.delivery.dto.DeliveryPersonResponse;
import com.ofood.delivery.model.DeliveryPerson;
import com.ofood.delivery.repository.DeliveryPersonRepository;
import com.ofood.location.model.ServicePincode;
import com.ofood.location.repository.ServicePincodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeliveryPersonService {
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final ServicePincodeRepository pincodeRepository;

    public DeliveryPersonService(DeliveryPersonRepository deliveryPersonRepository, ServicePincodeRepository pincodeRepository) {
        this.deliveryPersonRepository = deliveryPersonRepository;
        this.pincodeRepository = pincodeRepository;
    }

    @Transactional(readOnly = true)
    public List<DeliveryPersonResponse> getAllDeliveryPersons() {
        return deliveryPersonRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeliveryPersonResponse getDeliveryPersonById(UUID id) {
        return deliveryPersonRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Delivery person not found"));
    }

    @Transactional
    public DeliveryPersonResponse createDeliveryPerson(DeliveryPersonRequest request) {
        if (deliveryPersonRepository.existsByMobile(request.getMobile())) {
            throw new IllegalArgumentException("Mobile number already registered for another delivery person");
        }

        DeliveryPerson dp = new DeliveryPerson();
        dp.setFirstName(request.getFirstName());
        dp.setLastName(request.getLastName());
        dp.setMobile(request.getMobile());
        dp.setVehicleType(request.getVehicleType());
        dp.setVehicleNumber(request.getVehicleNumber());
        dp.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        if (request.getPincodeIds() != null && !request.getPincodeIds().isEmpty()) {
            List<ServicePincode> pincodes = pincodeRepository.findAllById(request.getPincodeIds());
            if (pincodes.size() != request.getPincodeIds().size()) {
                throw new IllegalArgumentException("One or more pincode IDs are invalid");
            }
            dp.getServicePincodes().addAll(pincodes);
        }

        dp = deliveryPersonRepository.save(dp);
        return mapToResponse(dp);
    }

    @Transactional
    public DeliveryPersonResponse updateDeliveryPerson(UUID id, DeliveryPersonRequest request) {
        DeliveryPerson dp = deliveryPersonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Delivery person not found"));

        if (!dp.getMobile().equals(request.getMobile()) && deliveryPersonRepository.existsByMobile(request.getMobile())) {
            throw new IllegalArgumentException("Mobile number already registered for another delivery person");
        }

        dp.setFirstName(request.getFirstName());
        dp.setLastName(request.getLastName());
        dp.setMobile(request.getMobile());
        dp.setVehicleType(request.getVehicleType());
        dp.setVehicleNumber(request.getVehicleNumber());
        if (request.getStatus() != null) {
            dp.setStatus(request.getStatus());
        }

        if (request.getPincodeIds() != null) {
            List<ServicePincode> pincodes = pincodeRepository.findAllById(request.getPincodeIds());
            if (pincodes.size() != request.getPincodeIds().size()) {
                throw new IllegalArgumentException("One or more pincode IDs are invalid");
            }
            dp.getServicePincodes().clear();
            dp.getServicePincodes().addAll(pincodes);
        }
        
        dp.setUpdatedAt(Instant.now());

        dp = deliveryPersonRepository.save(dp);
        return mapToResponse(dp);
    }

    @Transactional
    public void deleteDeliveryPerson(UUID id) {
        if (!deliveryPersonRepository.existsById(id)) {
            throw new IllegalArgumentException("Delivery person not found");
        }
        deliveryPersonRepository.deleteById(id);
    }

    private DeliveryPersonResponse mapToResponse(DeliveryPerson dp) {
        DeliveryPersonResponse response = new DeliveryPersonResponse();
        response.setId(dp.getId());
        response.setFirstName(dp.getFirstName());
        response.setLastName(dp.getLastName());
        response.setMobile(dp.getMobile());
        response.setVehicleType(dp.getVehicleType());
        response.setVehicleNumber(dp.getVehicleNumber());
        response.setStatus(dp.getStatus());
        
        List<String> pincodes = dp.getServicePincodes().stream()
                .map(ServicePincode::getPincode)
                .collect(Collectors.toList());
        response.setServicePincodes(pincodes);
        
        response.setCreatedAt(dp.getCreatedAt());
        response.setUpdatedAt(dp.getUpdatedAt());
        return response;
    }
}
