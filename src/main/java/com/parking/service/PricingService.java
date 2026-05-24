package com.parking.service;

import com.parking.entity.ParkingSpot;
import com.parking.entity.Tariff;
import com.parking.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final TariffRepository tariffRepository;

    @Transactional(readOnly = true)
    public BigDecimal calculate(ParkingSpot spot, Instant start, Instant end) {
        if (!end.isAfter(start)) {
            return BigDecimal.ZERO;
        }
        long minutes = Duration.between(start, end).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        Tariff tariff = chooseTariff(spot, start);
        BigDecimal multiplier = tariff.getDynamicMultiplier() == null ? BigDecimal.ONE : tariff.getDynamicMultiplier();
        return tariff.getPricePerHour()
                .multiply(hours)
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Tariff chooseTariff(ParkingSpot spot, Instant start) {
        Long parkingId = spot.getParking().getId();
        List<Tariff> tariffs = tariffRepository.findByParkingId(parkingId);
        if (tariffs.isEmpty()) {
            throw new IllegalStateException("No tariff configured for parking " + parkingId);
        }
        DayOfWeek dow = start.atZone(ZoneOffset.UTC).getDayOfWeek();
        LocalTime time = start.atZone(ZoneOffset.UTC).toLocalTime();
        // Pick the most specific tariff that applies; specificity is scored by number of matching constraints.
        return tariffs.stream()
                .filter(t -> t.getVehicleType() == null || t.getVehicleType() == spot.getType())
                .filter(t -> t.getDayOfWeek() == null || t.getDayOfWeek() == dow)
                .filter(t -> t.getHourFrom() == null || !time.isBefore(t.getHourFrom()))
                .filter(t -> t.getHourTo() == null   ||  time.isBefore(t.getHourTo()))
                .max(Comparator.comparingInt(this::specificity))
                .orElse(tariffs.get(0));
    }

    private int specificity(Tariff t) {
        int s = 0;
        if (t.getVehicleType() != null) s++;
        if (t.getDayOfWeek() != null)   s++;
        if (t.getHourFrom() != null)    s++;
        if (t.getHourTo() != null)      s++;
        return s;
    }
}
