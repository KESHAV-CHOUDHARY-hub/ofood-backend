package com.ofood.customer.service;

import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
import com.ofood.customer.dto.AddressRequest;
import com.ofood.customer.dto.AddressResponse;
import com.ofood.customer.model.Address;
import com.ofood.customer.repository.AddressRepository;
import com.ofood.location.model.City;
import com.ofood.location.model.ServicePincode;
import com.ofood.location.repository.ServicePincodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ServicePincodeRepository pincodeRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository, ServicePincodeRepository pincodeRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.pincodeRepository = pincodeRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getCustomerAddresses(UUID customerId) {
        return addressRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddressById(UUID addressId, UUID customerId) {
        return addressRepository.findByIdAndCustomerId(addressId, customerId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Address not found or does not belong to you"));
    }

    @Transactional
    public AddressResponse createAddress(UUID customerId, AddressRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        ServicePincode sp = validateServiceability(request.getPincode());

        Address address = new Address();
        address.setCustomer(customer);
        mapRequestToAddress(request, address, sp.getCity());

        boolean isFirstAddress = !addressRepository.existsByCustomerId(customerId);
        if (isFirstAddress || (request.getIsDefault() != null && request.getIsDefault())) {
            if (!isFirstAddress) {
                addressRepository.clearDefaultAddressForCustomer(customerId);
            }
            address.setIsDefault(true);
        } else {
            address.setIsDefault(false);
        }

        address = addressRepository.save(address);
        return mapToResponse(address);
    }

    @Transactional
    public AddressResponse updateAddress(UUID addressId, UUID customerId, AddressRequest request) {
        Address address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found or does not belong to you"));

        ServicePincode sp = validateServiceability(request.getPincode());

        mapRequestToAddress(request, address, sp.getCity());

        if (request.getIsDefault() != null && request.getIsDefault() && !address.getIsDefault()) {
            addressRepository.clearDefaultAddressForCustomer(customerId);
            address.setIsDefault(true);
        }

        address.setUpdatedAt(Instant.now());
        address = addressRepository.save(address);
        return mapToResponse(address);
    }

    @Transactional
    public void deleteAddress(UUID addressId, UUID customerId) {
        Address address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found or does not belong to you"));
        
        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse markAsDefault(UUID addressId, UUID customerId) {
        Address address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found or does not belong to you"));
        
        if (!address.getIsDefault()) {
            addressRepository.clearDefaultAddressForCustomer(customerId);
            address.setIsDefault(true);
            address.setUpdatedAt(Instant.now());
            address = addressRepository.save(address);
        }
        return mapToResponse(address);
    }

    private ServicePincode validateServiceability(String pincodeStr) {
        List<ServicePincode> pincodes = pincodeRepository.findByPincode(pincodeStr);
        if (pincodes.isEmpty()) {
            throw new IllegalArgumentException("Pincode is not serviceable");
        }
        for (ServicePincode sp : pincodes) {
            if ("ACTIVE".equalsIgnoreCase(sp.getStatus()) && "ACTIVE".equalsIgnoreCase(sp.getCity().getStatus())) {
                return sp;
            }
        }
        throw new IllegalArgumentException("Pincode is temporarily unserviceable");
    }

    private void mapRequestToAddress(AddressRequest request, Address address, City city) {
        address.setFullName(request.getFullName());
        address.setMobile(request.getMobile());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setLandmark(request.getLandmark());
        address.setArea(request.getArea());
        
        if (city != null) {
            address.setCity(city);
            address.setCityStr(city.getName());
            address.setState(city.getState());
        } else {
            address.setCityStr(request.getCity());
            address.setState(request.getState());
        }
        
        address.setPincode(request.getPincode());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setAddressType(request.getAddressType());
    }

    private AddressResponse mapToResponse(Address address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setFullName(address.getFullName());
        response.setMobile(address.getMobile());
        response.setAddressLine1(address.getAddressLine1());
        response.setAddressLine2(address.getAddressLine2());
        response.setLandmark(address.getLandmark());
        response.setArea(address.getArea());
        response.setCity(address.getCityStr());
        response.setState(address.getState());
        response.setPincode(address.getPincode());
        response.setLatitude(address.getLatitude());
        response.setLongitude(address.getLongitude());
        response.setAddressType(address.getAddressType());
        response.setIsDefault(address.getIsDefault());
        response.setCreatedAt(address.getCreatedAt());
        response.setUpdatedAt(address.getUpdatedAt());
        return response;
    }
}
