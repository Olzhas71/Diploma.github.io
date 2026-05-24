package com.parking.controller;

import com.parking.dto.parking.*;
import com.parking.service.ParkingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Parkings")
@RestController
@RequestMapping("/parkings")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    @GetMapping
    public ResponseEntity<List<ParkingResponse>> list() {
        return ResponseEntity.ok(parkingService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(parkingService.getById(id));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<ParkingResponse>> nearby(@RequestParam double lat,
                                                        @RequestParam double lon,
                                                        @RequestParam(defaultValue = "5.0") double radiusKm) {
        return ResponseEntity.ok(parkingService.findNearby(lat, lon, radiusKm));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ParkingResponse> create(@Valid @RequestBody ParkingRequest request) {
        return ResponseEntity.ok(parkingService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ParkingResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ParkingRequest request) {
        return ResponseEntity.ok(parkingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        parkingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Spots ----

    @GetMapping("/{id}/spots")
    public ResponseEntity<List<SpotResponse>> spots(@PathVariable Long id) {
        return ResponseEntity.ok(parkingService.listSpots(id));
    }

    @PostMapping("/{id}/spots")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<SpotResponse> addSpot(@PathVariable Long id,
                                                @Valid @RequestBody SpotRequest request) {
        return ResponseEntity.ok(parkingService.addSpot(id, request));
    }

    @PostMapping("/{id}/spots/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<List<SpotResponse>> bulkAddSpots(@PathVariable Long id,
                                                           @RequestParam int count,
                                                           @RequestParam(defaultValue = "S") String prefix) {
        return ResponseEntity.ok(parkingService.bulkCreateSpots(id, count, prefix));
    }

    @PutMapping("/{id}/spots/{spotId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<SpotResponse> updateSpot(@PathVariable Long id,
                                                   @PathVariable Long spotId,
                                                   @Valid @RequestBody SpotRequest request) {
        return ResponseEntity.ok(parkingService.updateSpot(id, spotId, request));
    }

    @DeleteMapping("/{id}/spots/{spotId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Void> deleteSpot(@PathVariable Long id, @PathVariable Long spotId) {
        parkingService.deleteSpot(id, spotId);
        return ResponseEntity.noContent().build();
    }

    // ---- Tariffs ----

    @GetMapping("/{id}/tariffs")
    public ResponseEntity<List<TariffResponse>> tariffs(@PathVariable Long id) {
        return ResponseEntity.ok(parkingService.listTariffs(id));
    }

    @PostMapping("/{id}/tariffs")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<TariffResponse> addTariff(@PathVariable Long id,
                                                    @Valid @RequestBody TariffRequest request) {
        return ResponseEntity.ok(parkingService.addTariff(id, request));
    }

    @DeleteMapping("/tariffs/{tariffId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Void> deleteTariff(@PathVariable Long tariffId) {
        parkingService.deleteTariff(tariffId);
        return ResponseEntity.noContent().build();
    }
}
