package com.parking.controller;

import com.parking.dto.stats.HourlyOccupancyPoint;
import com.parking.dto.stats.PopularParking;
import com.parking.dto.stats.RevenuePoint;
import com.parking.dto.stats.StatsOverview;
import com.parking.service.StatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Statistics")
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public ResponseEntity<StatsOverview> overview() {
        return ResponseEntity.ok(statsService.overview());
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenuePoint>> revenue(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(statsService.revenueByDay(days));
    }

    @GetMapping("/popular-parkings")
    public ResponseEntity<List<PopularParking>> popular(@RequestParam(defaultValue = "30") int days,
                                                        @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(statsService.popularParkings(days, limit));
    }

    @GetMapping("/occupancy-by-hour")
    public ResponseEntity<List<HourlyOccupancyPoint>> occupancyByHour(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(statsService.occupancyByHour(days));
    }
}
