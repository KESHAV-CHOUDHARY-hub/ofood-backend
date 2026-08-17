package com.ofood.location.service;

import com.ofood.location.dto.CityRequest;
import com.ofood.location.dto.CityResponse;
import com.ofood.location.model.City;
import com.ofood.location.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CityService {
    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public List<CityResponse> getAllCities() {
        return cityRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CityResponse> getActiveCities() {
        return cityRepository.findByStatus("ACTIVE").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CityResponse getCityById(UUID id) {
        return cityRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("City not found"));
    }

    @Transactional
    public CityResponse createCity(CityRequest request) {
        if (cityRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("City slug already exists");
        }
        City city = new City();
        city.setName(request.getName());
        city.setSlug(request.getSlug());
        city.setState(request.getState());
        city.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        
        city = cityRepository.save(city);
        return mapToResponse(city);
    }

    @Transactional
    public CityResponse updateCity(UUID id, CityRequest request) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("City not found"));

        if (!city.getSlug().equals(request.getSlug()) && cityRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("City slug already exists");
        }

        city.setName(request.getName());
        city.setSlug(request.getSlug());
        city.setState(request.getState());
        if (request.getStatus() != null) {
            city.setStatus(request.getStatus());
        }
        city.setUpdatedAt(Instant.now());

        city = cityRepository.save(city);
        return mapToResponse(city);
    }

    @Transactional
    public void deleteCity(UUID id) {
        if (!cityRepository.existsById(id)) {
            throw new IllegalArgumentException("City not found");
        }
        cityRepository.deleteById(id);
    }

    private CityResponse mapToResponse(City city) {
        CityResponse response = new CityResponse();
        response.setId(city.getId());
        response.setName(city.getName());
        response.setSlug(city.getSlug());
        response.setState(city.getState());
        response.setStatus(city.getStatus());
        response.setCreatedAt(city.getCreatedAt());
        response.setUpdatedAt(city.getUpdatedAt());
        return response;
    }
}
