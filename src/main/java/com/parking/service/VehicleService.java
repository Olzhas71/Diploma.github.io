package com.parking.service;

import com.parking.dto.vehicle.VehicleRequest;
import com.parking.dto.vehicle.VehicleResponse;
import com.parking.entity.User;
import com.parking.entity.Vehicle;
import com.parking.exception.ConflictException;
import com.parking.exception.ForbiddenException;
import com.parking.exception.ResourceNotFoundException;
import com.parking.mapper.VehicleMapper;
import com.parking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserService userService;
    private final VehicleMapper vehicleMapper;

    @Transactional(readOnly = true)
    public List<VehicleResponse> listForUser(Long userId) {
        return vehicleRepository.findByOwnerId(userId).stream()
                .map(vehicleMapper::toResponse)
                .toList();
    }

    @Transactional
    public VehicleResponse create(Long userId, VehicleRequest request) {
        String plate = request.licensePlate().trim().toUpperCase();
        if (vehicleRepository.existsByLicensePlateIgnoreCase(plate)) {
            throw new ConflictException("Vehicle with this license plate already exists");
        }
        User owner = userService.loadById(userId);
        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setLicensePlate(plate);
        vehicle.setOwner(owner);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse update(Long userId, Long vehicleId, VehicleRequest request) {
        Vehicle vehicle = loadOwned(userId, vehicleId);
        vehicleMapper.update(request, vehicle);
        vehicle.setLicensePlate(request.licensePlate().trim().toUpperCase());
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Long userId, Long vehicleId) {
        Vehicle vehicle = loadOwned(userId, vehicleId);
        vehicleRepository.delete(vehicle);
    }

    private Vehicle loadOwned(Long userId, Long vehicleId) {
        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", vehicleId));
        if (!v.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Vehicle does not belong to the current user");
        }
        return v;
    }
}
