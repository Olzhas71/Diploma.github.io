package com.parking.controller;

import com.parking.dto.ml.ModelInfo;
import com.parking.dto.ml.OccupancyForecast;
import com.parking.ml.OccupancyModelService;
import com.parking.ml.OccupancyPredictionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ML / Forecasts")
@RestController
@RequestMapping("/ml")
@RequiredArgsConstructor
public class MlController {

    private final OccupancyPredictionService predictionService;
    private final OccupancyModelService modelService;

    @GetMapping("/parkings/{parkingId}/forecast")
    public ResponseEntity<OccupancyForecast> forecast(@PathVariable Long parkingId) {
        return ResponseEntity.ok(predictionService.forecast(parkingId));
    }

    @GetMapping("/model")
    public ResponseEntity<ModelInfo> modelInfo() {
        return ResponseEntity.ok(modelService.info());
    }

    /**
     * Force a retrain. Useful for the demo so the committee can watch metrics
     * change live. Restricted to admins/operators because retraining is CPU-bound.
     */
    @PostMapping("/model/retrain")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ModelInfo> retrain() {
        return ResponseEntity.ok(modelService.retrain());
    }
}
