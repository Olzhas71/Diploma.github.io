package com.parking.dto.booking;

import com.parking.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingResponse(
        Long id,
        Long userId,
        Long spotId,
        String spotNumber,
        Long parkingId,
        String parkingName,
        Long vehicleId,
        Instant startTime,
        Instant endTime,
        BookingStatus status,
        BigDecimal totalAmount,
        String currency,
        boolean coveredBySubscription
) {}
