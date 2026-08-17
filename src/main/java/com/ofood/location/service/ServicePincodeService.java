package com.ofood.location.service;

import com.ofood.location.dto.ServicePincodeRequest;
import com.ofood.location.dto.ServicePincodeResponse;
import com.ofood.location.model.City;
import com.ofood.location.model.ServicePincode;
import com.ofood.location.repository.CityRepository;
import com.ofood.location.repository.ServicePincodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServicePincodeService {
    private final ServicePincodeRepository pincodeRepository;
    private final CityRepository cityRepository;

    public ServicePincodeService(ServicePincodeRepository pincodeRepository, CityRepository cityRepository) {
        this.pincodeRepository = pincodeRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public List<ServicePincodeResponse> getAllPincodes() {
        return pincodeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServicePincodeResponse> getActivePincodes() {
        return pincodeRepository.findByStatus("ACTIVE").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServicePincodeResponse getPincodeById(UUID id) {
        return pincodeRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Pincode not found"));
    }

    @Transactional
    public ServicePincodeResponse createPincode(ServicePincodeRequest request) {
        if (pincodeRepository.existsByPincode(request.getPincode())) {
            throw new IllegalArgumentException("Pincode already exists");
        }
        
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new IllegalArgumentException("City not found"));

        ServicePincode pincode = new ServicePincode();
        pincode.setPincode(request.getPincode());
        pincode.setCity(city);
        pincode.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        
        pincode = pincodeRepository.save(pincode);
        return mapToResponse(pincode);
    }

    @Transactional
    public ServicePincodeResponse updatePincode(UUID id, ServicePincodeRequest request) {
        ServicePincode pincode = pincodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pincode not found"));

        if (!pincode.getPincode().equals(request.getPincode()) && pincodeRepository.existsByPincode(request.getPincode())) {
            throw new IllegalArgumentException("Pincode already exists");
        }

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new IllegalArgumentException("City not found"));

        pincode.setPincode(request.getPincode());
        pincode.setCity(city);
        if (request.getStatus() != null) {
            pincode.setStatus(request.getStatus());
        }
        pincode.setUpdatedAt(Instant.now());

        pincode = pincodeRepository.save(pincode);
        return mapToResponse(pincode);
    }

    @Transactional
    public void deletePincode(UUID id) {
        if (!pincodeRepository.existsById(id)) {
            throw new IllegalArgumentException("Pincode not found");
        }
        pincodeRepository.deleteById(id);
    }

    private ServicePincodeResponse mapToResponse(ServicePincode pincode) {
        ServicePincodeResponse response = new ServicePincodeResponse();
        response.setId(pincode.getId());
        response.setPincode(pincode.getPincode());
        response.setCityId(pincode.getCity().getId());
        response.setCityName(pincode.getCity().getName());
        response.setStatus(pincode.getStatus());
        response.setCreatedAt(pincode.getCreatedAt());
        response.setUpdatedAt(pincode.getUpdatedAt());
        return response;
    }
}
