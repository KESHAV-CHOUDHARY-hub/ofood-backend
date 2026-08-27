package com.ofood.location.service;

import com.ofood.location.dto.ServicePincodeRequest;
import com.ofood.location.dto.ServicePincodeResponse;
import com.ofood.location.model.City;
import com.ofood.location.model.ServiceArea;
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
        if (pincodeRepository.existsByCityIdAndPincodeAndAreaName(request.getCityId(), request.getPincode(), request.getAreaName())) {
            throw new IllegalArgumentException("Service area already exists for this city and pincode");
        }
        
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new IllegalArgumentException("City not found"));

        validateServiceArea(request.getServiceArea());

        ServicePincode pincode = new ServicePincode();
        pincode.setPincode(request.getPincode());
        pincode.setAreaName(request.getAreaName().trim());
        pincode.setServiceArea(request.getServiceArea());
        pincode.setCity(city);
        
        if (request.getIsActive() != null) {
            pincode.setStatus(request.getIsActive() ? "ACTIVE" : "INACTIVE");
        } else {
            pincode.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        }
        
        pincode = pincodeRepository.save(pincode);
        return mapToResponse(pincode);
    }

    @Transactional
    public ServicePincodeResponse updatePincode(UUID id, ServicePincodeRequest request) {
        ServicePincode pincode = pincodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pincode not found"));

        boolean uniquenessChanged = !pincode.getCity().getId().equals(request.getCityId())
                || !pincode.getPincode().equals(request.getPincode())
                || !pincode.getAreaName().equals(request.getAreaName());

        if (uniquenessChanged && pincodeRepository.existsByCityIdAndPincodeAndAreaName(request.getCityId(), request.getPincode(), request.getAreaName())) {
            throw new IllegalArgumentException("Service area already exists for this city and pincode");
        }

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new IllegalArgumentException("City not found"));
                
        validateServiceArea(request.getServiceArea());

        pincode.setPincode(request.getPincode());
        pincode.setAreaName(request.getAreaName().trim());
        pincode.setServiceArea(request.getServiceArea());
        pincode.setCity(city);
        
        if (request.getIsActive() != null) {
            pincode.setStatus(request.getIsActive() ? "ACTIVE" : "INACTIVE");
        } else if (request.getStatus() != null) {
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
        response.setAreaName(pincode.getAreaName());
        response.setServiceArea(pincode.getServiceArea());
        response.setCityId(pincode.getCity().getId());
        response.setCityName(pincode.getCity().getName());
        response.setStatus(pincode.getStatus());
        response.setCreatedAt(pincode.getCreatedAt());
        response.setUpdatedAt(pincode.getUpdatedAt());
        return response;
    }

    private void validateServiceArea(ServiceArea serviceArea) {
        if (serviceArea == null || serviceArea.getRing() == null) {
            throw new IllegalArgumentException("Service area ring is required");
        }
        List<List<Double>> ring = serviceArea.getRing();
        if (ring.size() < 3) {
            throw new IllegalArgumentException("Service area must have at least 3 points to form a polygon");
        }
        for (List<Double> point : ring) {
            if (point == null || point.size() != 2) {
                throw new IllegalArgumentException("Each point in the service area must contain exactly 2 coordinates [latitude, longitude]");
            }
            double lat = point.get(0);
            double lng = point.get(1);
            if (lat < -90 || lat > 90) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90");
            }
            if (lng < -180 || lng > 180) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180");
            }
        }
    }
}
