package com.parking.dto.ml;

import java.time.Instant;
import java.util.List;

public record ModelInfo(
        boolean trained,
        String algorithm,
        Instant trainedAt,
        long trainSamples,
        long testSamples,
        Double rmse,
        Double mae,
        Double r2,
        List<String> features
) {
    public static ModelInfo notTrained() {
        return new ModelInfo(false, "none", null, 0, 0, null, null, null,
                List.of("hour_of_day", "day_of_week", "is_weekend", "parking_id"));
    }
}
