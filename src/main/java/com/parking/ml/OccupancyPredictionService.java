package com.parking.ml;

import com.parking.dto.ml.OccupancyForecast;
import com.parking.entity.Parking;
import com.parking.entity.SpotStatus;
import com.parking.exception.ResourceNotFoundException;
import com.parking.repository.ParkingRepository;
import com.parking.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Occupancy forecaster: produces a 24-hour prediction for a parking lot.
 *
 * Uses {@link OccupancyModelService} (CART regression on Tribuo) when a
 * trained model is available, otherwise falls back to the current live
 * occupancy ratio so the API still returns sensible numbers on cold start.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OccupancyPredictionService {

    private final OccupancyModelService modelService;
    private final ParkingSpotRepository spotRepository;
    private final ParkingRepository parkingRepository;

    @Value("${app.ml.occupancy-prediction-window-hours:24}")
    private int predictionWindowHours;

    @Transactional(readOnly = true)
    @Cacheable(value = "occupancyForecast", key = "#parkingId")
    public OccupancyForecast forecast(Long parkingId) {
        Parking parking = parkingRepository.findById(parkingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Parking", parkingId));
        Instant now = Instant.now();
        boolean modelTrained = modelService.info().trained();

        List<OccupancyForecast.HourlyForecast> hourly = new ArrayList<>();
        for (int i = 0; i < predictionWindowHours; i++) {
            ZonedDateTime t = now.plusSeconds(i * 3600L).atZone(ZoneOffset.UTC);
            int hourOfDay = t.getHour();
            int dayOfWeek = t.getDayOfWeek().getValue(); // ISO: Mon=1..Sun=7

            double rate = modelTrained
                    ? modelService.predict(parkingId, hourOfDay, dayOfWeek)
                    : liveOccupancyRate(parking);

            int free = parking.getTotalSpots() == null ? 0
                    : (int) Math.round((1.0 - rate) * parking.getTotalSpots());
            hourly.add(new OccupancyForecast.HourlyForecast(hourOfDay, round(rate), Math.max(0, free)));
        }
        if (!modelTrained) {
            log.debug("Model not trained yet — forecast for parking {} uses live ratio", parkingId);
        }
        return new OccupancyForecast(parkingId, now, hourly);
    }

    private double liveOccupancyRate(Parking parking) {
        if (parking.getTotalSpots() == null || parking.getTotalSpots() == 0) return 0.0;
        long occupied = spotRepository.countByParkingIdAndStatus(parking.getId(), SpotStatus.OCCUPIED)
                + spotRepository.countByParkingIdAndStatus(parking.getId(), SpotStatus.RESERVED);
        return (double) occupied / parking.getTotalSpots();
    }

    private double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
