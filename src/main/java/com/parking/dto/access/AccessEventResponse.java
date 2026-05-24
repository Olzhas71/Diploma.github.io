package com.parking.dto.access;

import com.parking.entity.AccessEventType;

import java.time.Instant;

public record AccessEventResponse(
        Long id,
        Long parkingId,
        String parkingName,
        Long vehicleId,
        String licensePlateRecognized,
        AccessEventType eventType,
        Instant timestamp,
        String photoUrl
) {}
