package com.parking.dto.stats;

import java.math.BigDecimal;

public record PopularParking(
        Long parkingId,
        String name,
        long bookings,
        BigDecimal revenue
) {}
