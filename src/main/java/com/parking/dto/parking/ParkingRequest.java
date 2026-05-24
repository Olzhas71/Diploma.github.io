package com.parking.dto.parking;

import com.parking.entity.ParkingType;
import jakarta.validation.constraints.*;

import java.time.LocalTime;

public record ParkingRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 512) String address,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull ParkingType type,
        @NotNull @Min(1) Integer totalSpots,
        LocalTime workingHoursFrom,
        LocalTime workingHoursTo
) {}
