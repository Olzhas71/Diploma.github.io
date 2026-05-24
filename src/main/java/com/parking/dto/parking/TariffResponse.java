package com.parking.dto.parking;

import com.parking.entity.SpotType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record TariffResponse(
        Long id,
        Long parkingId,
        String name,
        BigDecimal pricePerHour,
        String currency,
        DayOfWeek dayOfWeek,
        LocalTime hourFrom,
        LocalTime hourTo,
        SpotType vehicleType,
        BigDecimal dynamicMultiplier
) {}
