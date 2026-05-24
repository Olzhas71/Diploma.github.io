package com.parking.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record BookingRequest(
        @NotNull Long spotId,
        Long vehicleId,
        @NotNull @Future Instant startTime,
        @NotNull @Future Instant endTime
) {}
