package com.parking.service;

import com.parking.dto.access.AccessEventRequest;
import com.parking.dto.access.AccessEventResponse;
import com.parking.entity.AccessEvent;
import com.parking.entity.AccessEventType;
import com.parking.entity.Parking;
import com.parking.entity.Vehicle;
import com.parking.exception.ResourceNotFoundException;
import com.parking.repository.AccessEventRepository;
import com.parking.repository.ParkingRepository;
import com.parking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Records vehicle entry/exit events at a parking — the "smart gate" / camera
 * trigger of an intelligent parking system. Plate is matched against
 * registered vehicles when possible.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessEventService {

    private final AccessEventRepository accessEventRepository;
    private final ParkingRepository parkingRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public AccessEventResponse record(AccessEventRequest request) {
        Parking parking = parkingRepository.findById(request.parkingId())
                .orElseThrow(() -> ResourceNotFoundException.of("Parking", request.parkingId()));
        String plate = request.licensePlate().trim().toUpperCase();
        Vehicle vehicle = vehicleRepository.findByLicensePlateIgnoreCase(plate).orElse(null);

        AccessEvent event = AccessEvent.builder()
                .parking(parking)
                .vehicle(vehicle)
                .licensePlateRecognized(plate)
                .eventType(request.eventType())
                .timestamp(Instant.now())
                .photoUrl(request.photoUrl())
                .build();
        event = accessEventRepository.save(event);
        log.info("Access event {}: parking={} plate={} type={} matchedVehicle={}",
                event.getId(), parking.getId(), plate, request.eventType(),
                vehicle == null ? "no" : vehicle.getId());
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public Page<AccessEventResponse> list(Long parkingId, Pageable pageable) {
        Page<AccessEvent> page = parkingId == null
                ? accessEventRepository.findAllByOrderByTimestampDesc(pageable)
                : accessEventRepository.findByParkingIdOrderByTimestampDesc(parkingId, pageable);
        return page.map(this::toResponse);
    }

    private AccessEventResponse toResponse(AccessEvent e) {
        return new AccessEventResponse(
                e.getId(),
                e.getParking().getId(),
                e.getParking().getName(),
                e.getVehicle() == null ? null : e.getVehicle().getId(),
                e.getLicensePlateRecognized(),
                e.getEventType(),
                e.getTimestamp(),
                e.getPhotoUrl()
        );
    }
}
