package com.parking.dto.parking;

import com.parking.entity.SpotStatus;
import com.parking.entity.SpotType;

public record SpotResponse(
        Long id,
        Long parkingId,
        String spotNumber,
        Integer level,
        SpotType type,
        SpotStatus status
) {}
