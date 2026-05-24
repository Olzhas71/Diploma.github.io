package com.parking.dto.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotBlank @Size(max = 32) String licensePlate,
        @Size(max = 64) String make,
        @Size(max = 64) String model,
        @Size(max = 32) String color
) {}
