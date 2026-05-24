package com.parking.controller;

import com.parking.dto.vehicle.VehicleRequest;
import com.parking.dto.vehicle.VehicleResponse;
import com.parking.service.VehicleService;
import com.parking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Vehicles")
@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> list() {
        return ResponseEntity.ok(vehicleService.listForUser(SecurityUtils.requireUserId()));
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.create(SecurityUtils.requireUserId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.update(SecurityUtils.requireUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleService.delete(SecurityUtils.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
