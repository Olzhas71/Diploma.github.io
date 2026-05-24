package com.parking.service;

import com.parking.entity.*;
import com.parking.repository.TariffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock TariffRepository tariffRepository;
    @InjectMocks PricingService pricingService;

    private ParkingSpot spot;
    private Tariff baseTariff;

    @BeforeEach
    void setup() {
        Parking parking = Parking.builder().id(1L).name("P").build();
        spot = ParkingSpot.builder().id(1L).parking(parking).type(SpotType.REGULAR).build();
        baseTariff = Tariff.builder()
                .id(1L)
                .parking(parking)
                .name("std")
                .pricePerHour(new BigDecimal("5.00"))
                .currency("USD")
                .dynamicMultiplier(BigDecimal.ONE)
                .build();
    }

    @Test
    void calculatesPriceForFullHour() {
        when(tariffRepository.findByParkingId(any())).thenReturn(List.of(baseTariff));
        Instant start = Instant.parse("2025-01-15T10:00:00Z");
        Instant end = start.plus(1, ChronoUnit.HOURS);

        BigDecimal price = pricingService.calculate(spot, start, end);

        assertThat(price).isEqualByComparingTo("5.00");
    }

    @Test
    void calculatesPriceForFractionalHour() {
        when(tariffRepository.findByParkingId(any())).thenReturn(List.of(baseTariff));
        Instant start = Instant.parse("2025-01-15T10:00:00Z");
        Instant end = start.plus(90, ChronoUnit.MINUTES);

        BigDecimal price = pricingService.calculate(spot, start, end);

        assertThat(price).isEqualByComparingTo("7.50");
    }

    @Test
    void appliesDynamicMultiplier() {
        baseTariff.setDynamicMultiplier(new BigDecimal("1.50"));
        when(tariffRepository.findByParkingId(any())).thenReturn(List.of(baseTariff));
        Instant start = Instant.parse("2025-01-15T10:00:00Z");
        Instant end = start.plus(2, ChronoUnit.HOURS);

        BigDecimal price = pricingService.calculate(spot, start, end);

        assertThat(price).isEqualByComparingTo("15.00");
    }

    @Test
    void picksMostSpecificTariff() {
        Tariff weekendNight = Tariff.builder()
                .id(2L)
                .parking(spot.getParking())
                .name("weekend-night")
                .pricePerHour(new BigDecimal("2.00"))
                .currency("USD")
                .dynamicMultiplier(BigDecimal.ONE)
                .dayOfWeek(DayOfWeek.SATURDAY)
                .hourFrom(LocalTime.of(0, 0))
                .hourTo(LocalTime.of(6, 0))
                .build();
        when(tariffRepository.findByParkingId(any())).thenReturn(List.of(baseTariff, weekendNight));

        // Saturday 02:00 UTC
        Instant start = Instant.parse("2025-01-18T02:00:00Z");
        Instant end = start.plus(1, ChronoUnit.HOURS);

        BigDecimal price = pricingService.calculate(spot, start, end);

        assertThat(price).isEqualByComparingTo("2.00");
    }

    @Test
    void zeroForInvertedRange() {
        Instant start = Instant.parse("2025-01-15T10:00:00Z");
        BigDecimal price = pricingService.calculate(spot, start, start);
        assertThat(price).isEqualByComparingTo("0");
    }
}
