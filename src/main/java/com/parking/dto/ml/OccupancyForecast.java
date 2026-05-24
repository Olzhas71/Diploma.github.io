package com.parking.dto.ml;

import java.time.Instant;
import java.util.List;

public record OccupancyForecast(
        Long parkingId,
        Instant generatedAt,
        List<HourlyForecast> hourly
) {
    public record HourlyForecast(int hourOfDay, double predictedOccupancyRate, int predictedFreeSpots) {}
}
