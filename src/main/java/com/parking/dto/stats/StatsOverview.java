package com.parking.dto.stats;

import java.math.BigDecimal;

public record StatsOverview(
        long totalParkings,
        long totalSpots,
        long freeSpots,
        long activeBookings,
        long totalBookings,
        long activeSubscriptions,
        BigDecimal totalRevenue,
        String currency,
        double averageOccupancyRate
) {}
