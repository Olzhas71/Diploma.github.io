package com.parking.dto.vehicle;

public record VehicleResponse(
        Long id,
        String licensePlate,
        String make,
        String model,
        String color,
        Long ownerId
) {}
