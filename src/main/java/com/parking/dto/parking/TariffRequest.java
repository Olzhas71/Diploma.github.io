package com.parking.dto.parking;

import com.parking.entity.SpotType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record TariffRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerHour,
        String currency,
        DayOfWeek dayOfWeek,
        LocalTime hourFrom,
        LocalTime hourTo,
        SpotType vehicleType,
        BigDecimal dynamicMultiplier
) {}
