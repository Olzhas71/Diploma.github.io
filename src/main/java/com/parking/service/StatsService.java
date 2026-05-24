package com.parking.service;

import com.parking.dto.stats.HourlyOccupancyPoint;
import com.parking.dto.stats.PopularParking;
import com.parking.dto.stats.RevenuePoint;
import com.parking.dto.stats.StatsOverview;
import com.parking.entity.BookingStatus;
import com.parking.entity.SpotStatus;
import com.parking.repository.BookingRepository;
import com.parking.repository.ParkingSpotRepository;
import com.parking.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsRepository statsRepository;
    private final BookingRepository bookingRepository;
    private final ParkingSpotRepository spotRepository;
    private final SubscriptionService subscriptionService;

    @Transactional(readOnly = true)
    @Cacheable(value = "statsOverview", key = "'global'")
    public StatsOverview overview() {
        long parkings = statsRepository.count("parkings");
        long spots = statsRepository.count("parking_spots");
        long free = statsRepository.countByStatus("parking_spots", SpotStatus.FREE.name());
        long active = bookingRepository.countByStatus(BookingStatus.ACTIVE);
        long total = statsRepository.count("bookings");
        long activeSubs = subscriptionService.countActive();
        BigDecimal revenue = statsRepository.totalRevenue();
        double occupancy = spots == 0 ? 0.0 : 1.0 - ((double) free / spots);
        return new StatsOverview(
                parkings, spots, free, active, total, activeSubs,
                revenue, "USD",
                Math.round(occupancy * 1000.0) / 1000.0
        );
    }

    @Transactional(readOnly = true)
    public List<RevenuePoint> revenueByDay(int days) {
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofDays(Math.max(1, Math.min(365, days))));
        return statsRepository.revenueByDay(from, to).stream()
                .map(r -> new RevenuePoint(r.day(), r.revenue(), r.bookings()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PopularParking> popularParkings(int days, int limit) {
        Instant from = Instant.now().minus(Duration.ofDays(Math.max(1, Math.min(365, days))));
        return statsRepository.popularParkings(from, limit).stream()
                .map(r -> new PopularParking(r.parkingId(), r.name(), r.bookings(), r.revenue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HourlyOccupancyPoint> occupancyByHour(int days) {
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofDays(Math.max(1, Math.min(90, days))));
        return statsRepository.averageOccupancyByHour(from, to).stream()
                .map(r -> new HourlyOccupancyPoint(
                        r.hourOfDay(),
                        Math.round(r.rate() * 1000.0) / 1000.0,
                        r.samples()))
                .toList();
    }
}
