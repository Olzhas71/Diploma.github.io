package com.parking.ml;

import com.parking.entity.ParkingSpot;
import com.parking.entity.SensorReading;
import com.parking.entity.SpotStatus;
import com.parking.repository.ParkingSpotRepository;
import com.parking.repository.SensorReadingRepository;
import com.parking.websocket.OccupancyWebSocketHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates realistic occupancy data for the ML forecast and analytics demos.
 *
 * Two modes:
 *   - <b>Backfill</b>: on first startup (when sensor_readings is empty), seeds
 *     ~30 days of half-hourly readings across all spots, modulated by an
 *     hour-of-day curve (rush hour peaks) and a weekend factor.
 *   - <b>Live</b>: every 30 s flips the status of a few random spots, broadcasts
 *     it via WebSocket, and writes a sensor reading.
 *
 * Disabled by default; enable via {@code app.ml.simulate-sensors=true}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ml.simulate-sensors", havingValue = "true", matchIfMissing = false)
public class SensorSimulationService {

    private final ParkingSpotRepository spotRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final OccupancyWebSocketHandler webSocketHandler;
    private final OccupancyModelService modelService;
    private final Random rng = new Random(42);

    @Value("${app.ml.history-window-days:30}")
    private int historyDays;

    @PostConstruct
    void announce() {
        log.info("Sensor simulation enabled (history window={} days)", historyDays);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillIfEmpty() {
        List<ParkingSpot> allSpots = spotRepository.findAll();
        if (allSpots.isEmpty()) {
            log.info("no parking spots, skipping backfill");
            return;
        }
        Set<Long> alreadyHave = sensorReadingRepository.findSpotIdsWithReadings();
        List<ParkingSpot> spots = allSpots.stream()
                .filter(s -> !alreadyHave.contains(s.getId()))
                .toList();
        if (spots.isEmpty()) {
            log.info("All {} spots already have readings, skipping backfill", allSpots.size());
            return;
        }
        log.info("Backfilling history for {} of {} spots", spots.size(), allSpots.size());
        Instant now = Instant.now();
        List<SensorReading> batch = new ArrayList<>(8192);
        long writes = 0;
        // half-hourly readings per spot for the last N days
        for (int day = historyDays; day >= 1; day--) {
            for (int half = 0; half < 48; half++) {
                Instant ts = now.minus(Duration.ofDays(day)).plus(Duration.ofMinutes(30L * half));
                for (ParkingSpot spot : spots) {
                    boolean occupied = sampleOccupancy(ts, spot);
                    batch.add(SensorReading.builder()
                            .spot(spot)
                            .occupied(occupied)
                            .timestamp(ts)
                            .sensorId("sim-" + spot.getId())
                            .build());
                    if (batch.size() >= 1000) {
                        sensorReadingRepository.saveAll(batch);
                        writes += batch.size();
                        batch.clear();
                    }
                }
            }
        }
        if (!batch.isEmpty()) {
            sensorReadingRepository.saveAll(batch);
            writes += batch.size();
        }
        log.info("Backfilled {} sensor readings across {} spots over {} days",
                writes, spots.size(), historyDays);
        // Now that the dataset is populated, kick off model training. The
        // model's own ApplicationReadyEvent listener may have already run and
        // exited early because the table was empty — this call retrains it.
        try {
            modelService.retrain();
        } catch (Exception ex) {
            log.warn("Initial post-backfill model training failed: {}", ex.getMessage());
        }
    }

    /**
     * Live tick: occasionally toggle a few spots to simulate cars arriving/leaving.
     * Interval is configurable via {@code app.ml.sensor-tick-ms} (default 30s).
     */
    @Scheduled(fixedDelayString = "${app.ml.sensor-tick-ms:30000}")
    @Transactional
    public void liveTick() {
        List<ParkingSpot> spots = spotRepository.findAll();
        if (spots.isEmpty()) return;
        int toggles = Math.min(3, Math.max(1, spots.size() / 8));
        Instant now = Instant.now();
        for (int i = 0; i < toggles; i++) {
            ParkingSpot spot = spots.get(ThreadLocalRandom.current().nextInt(spots.size()));
            // Don't fight live bookings - skip RESERVED/MAINTENANCE
            if (spot.getStatus() == SpotStatus.RESERVED || spot.getStatus() == SpotStatus.MAINTENANCE) continue;
            boolean shouldOccupy = sampleOccupancy(now, spot);
            SpotStatus next = shouldOccupy ? SpotStatus.OCCUPIED : SpotStatus.FREE;
            if (next != spot.getStatus()) {
                spot.setStatus(next);
                spotRepository.save(spot);
                webSocketHandler.broadcastSpotUpdate(spot);
            }
            sensorReadingRepository.save(SensorReading.builder()
                    .spot(spot)
                    .occupied(shouldOccupy)
                    .timestamp(now)
                    .sensorId("sim-" + spot.getId())
                    .build());
        }
    }

    /**
     * Probability that a given spot is occupied at the given instant.
     * Blends a daily rush-hour curve, a weekend factor, and per-spot noise.
     */
    private boolean sampleOccupancy(Instant ts, ParkingSpot spot) {
        var local = ts.atZone(ZoneOffset.UTC);
        int hour = local.getHour();
        DayOfWeek dow = local.getDayOfWeek();

        // hour-of-day curve: low at night, peaks in rush hours
        double base;
        if      (hour < 6)  base = 0.15;
        else if (hour < 9)  base = 0.30 + (hour - 6) * 0.18;   // morning ramp
        else if (hour < 17) base = 0.70 + Math.sin((hour - 9) * Math.PI / 8) * 0.10;
        else if (hour < 20) base = 0.85 + (hour == 18 ? 0.05 : 0.0);
        else                base = 0.85 - (hour - 19) * 0.18;

        // weekend dampening
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) base *= 0.55;

        // small per-spot personality so each spot fluctuates differently
        double personality = (Math.abs(spot.getId().hashCode()) % 100) / 100.0;
        double prob = base + (personality - 0.5) * 0.15;
        prob += (rng.nextGaussian() * 0.08);
        prob = Math.max(0.02, Math.min(0.98, prob));
        return rng.nextDouble() < prob;
    }
}
