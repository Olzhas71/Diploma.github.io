package com.parking.dto.parking;

import com.parking.entity.ParkingType;

import java.time.LocalTime;

public record ParkingResponse(
        Long id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        ParkingType type,
        Integer totalSpots,
        Long freeSpots,
        LocalTime workingHoursFrom,
        LocalTime workingHoursTo
) {}
