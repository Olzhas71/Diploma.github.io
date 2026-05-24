package com.parking.ml;

import com.parking.dto.ml.ModelInfo;
import com.parking.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tribuo.MutableDataset;
import org.tribuo.Prediction;
import org.tribuo.evaluation.TrainTestSplitter;
import org.tribuo.impl.ArrayExample;
import org.tribuo.regression.RegressionFactory;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.evaluation.RegressionEvaluation;
import org.tribuo.regression.evaluation.RegressionEvaluator;
import org.tribuo.regression.rtree.CARTRegressionTrainer;
import org.tribuo.regression.rtree.impurity.MeanSquaredError;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real-ML occupancy regressor: predicts occupancy rate (0..1) for a given
 * (parking, hour-of-day, day-of-week, is-weekend) tuple from history.
 *
 * Algorithm: CART (Classification and Regression Trees) regressor from Tribuo.
 * Trained on aggregated half-hourly bins from {@code sensor_readings} with a
 * 80/20 train/test split. Mean Squared Error as splitting impurity.
 *
 * Falls back to a sensible default (0.5) when the model is not yet trained.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OccupancyModelService {

    private static final String OUTPUT_NAME = "occupancyRate";
    private static final RegressionFactory REGRESSION_FACTORY = new RegressionFactory();

    private final SensorReadingRepository sensorReadingRepository;
    private final NamedParameterJdbcTemplate jdbc;

    private final AtomicReference<TrainedModel> currentModel = new AtomicReference<>();

    /** Train once at startup (async so it doesn't block). */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initialTrain() {
        retrain();
    }

    /** Retrain every 6 hours so the model adapts to new sensor data. */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000L, initialDelay = 6 * 60 * 60 * 1000L)
    public void scheduledRetrain() {
        retrain();
    }

    /**
     * Trains a fresh CART regressor on all available sensor data from the last
     * 30 days. Performs a deterministic 80/20 split, evaluates on the holdout,
     * and atomically swaps the trained model into {@link #currentModel}.
     */
    public synchronized ModelInfo retrain() {
        long count = sensorReadingRepository.count();
        if (count < 50) {
            log.info("Not enough sensor readings ({}) to train a model — keeping fallback", count);
            currentModel.set(null);
            return ModelInfo.notTrained();
        }

        List<Sample> samples = loadAggregatedSamples();
        if (samples.size() < 50) {
            log.info("Not enough aggregated samples ({}) to train", samples.size());
            currentModel.set(null);
            return ModelInfo.notTrained();
        }

        InMemoryDataSource source = new InMemoryDataSource(samples);
        TrainTestSplitter<Regressor> splitter = new TrainTestSplitter<Regressor>(source, 0.8, 1L);
        MutableDataset<Regressor> train = new MutableDataset<Regressor>(splitter.getTrain());
        MutableDataset<Regressor> test  = new MutableDataset<Regressor>(splitter.getTest());

        // CART regression tree, depth-limited to avoid overfitting on small datasets.
        CARTRegressionTrainer trainer = new CARTRegressionTrainer(
                /* maxDepth */ 6,
                /* minChildWeight */ 5.0f,
                /* minImpurityDecrease */ 0.0f,
                /* fractionFeaturesInSplit */ 1.0f,
                /* useRandomSplitPoints */ false,
                new MeanSquaredError(),
                Trainer.NEXT_SEED.getAndIncrement());

        org.tribuo.Model<Regressor> model = trainer.train(train);

        RegressionEvaluator evaluator = new RegressionEvaluator();
        RegressionEvaluation eval = evaluator.evaluate(model, test);
        // We have a single output dimension, so the average across dimensions = its value.
        double rmse = eval.averageRMSE();
        double mae  = eval.averageMAE();
        double r2   = eval.averageR2();

        TrainedModel snapshot = new TrainedModel(
                model, Instant.now(), train.size(), test.size(), rmse, mae, r2);
        currentModel.set(snapshot);
        log.info("Trained CART occupancy model: train={} test={} RMSE={} MAE={} R²={}",
                train.size(), test.size(),
                Math.round(rmse * 1000.0) / 1000.0,
                Math.round(mae  * 1000.0) / 1000.0,
                Math.round(r2   * 1000.0) / 1000.0);
        return info();
    }

    /** Predicts the occupancy rate for the given (parking, hour, dow). */
    public double predict(long parkingId, int hourOfDay, int dayOfWeek) {
        TrainedModel m = currentModel.get();
        if (m == null) return 0.5; // fallback when model is not trained
        boolean weekend = dayOfWeek == 6 || dayOfWeek == 7;
        ArrayExample<Regressor> example = new ArrayExample<>(
                new Regressor(OUTPUT_NAME, Double.NaN),
                new String[]{"hour_of_day", "day_of_week", "is_weekend", "parking_id"},
                new double[]{hourOfDay, dayOfWeek, weekend ? 1 : 0, parkingId});
        Prediction<Regressor> p = m.model.predict(example);
        double v = p.getOutput().getValues()[0];
        return Math.max(0.0, Math.min(1.0, v));
    }

    public ModelInfo info() {
        TrainedModel m = currentModel.get();
        if (m == null) return ModelInfo.notTrained();
        return new ModelInfo(
                true,
                "CART regression tree (Tribuo)",
                m.trainedAt,
                m.trainSamples,
                m.testSamples,
                round(m.rmse),
                round(m.mae),
                round(m.r2),
                List.of("hour_of_day", "day_of_week", "is_weekend", "parking_id"));
    }

    /**
     * Loads aggregated training samples from the last 30 days. Day-of-week is
     * derived from the raw timestamp in Java (avoids EXTRACT(ISODOW)
     * portability issues between Postgres and H2). Bucketing is by
     * {@code parking × CAST(timestamp AS DATE) × hour-of-day} — one row per
     * parking-day-hour with the average occupancy across all spots.
     */
    private List<Sample> loadAggregatedSamples() {
        Instant from = Instant.now().minus(Duration.ofDays(30));
        Instant to   = Instant.now();

        String sql = """
                SELECT ps.parking_id AS parking_id,
                       CAST(sr.timestamp AS DATE) AS d,
                       CAST(EXTRACT(HOUR FROM sr.timestamp) AS INTEGER) AS hour_of_day,
                       AVG(CASE WHEN sr.occupied THEN 1.0 ELSE 0.0 END) AS rate
                FROM sensor_readings sr
                JOIN parking_spots ps ON ps.id = sr.spot_id
                WHERE sr.timestamp >= :from AND sr.timestamp < :to
                GROUP BY 1, 2, 3
                """;

        var params = Map.of(
                "from", (Object) Timestamp.from(from),
                "to",   (Object) Timestamp.from(to));

        return jdbc.query(sql, params, (rs, i) -> {
            long parkingId = rs.getLong("parking_id");
            int hour = rs.getInt("hour_of_day");
            // ISO day-of-week: Monday=1 .. Sunday=7
            int dow = rs.getDate("d").toLocalDate().getDayOfWeek().getValue();
            double rate = rs.getDouble("rate");
            return new Sample(parkingId, hour, dow, rate);
        });
    }

    private static Double round(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return null;
        return Math.round(v * 10000.0) / 10000.0;
    }

    /** Snapshot of a successfully trained model. */
    private record TrainedModel(
            org.tribuo.Model<Regressor> model,
            Instant trainedAt,
            long trainSamples,
            long testSamples,
            double rmse,
            double mae,
            double r2
    ) {}

    /** Single training row. */
    record Sample(long parkingId, int hourOfDay, int dayOfWeek, double occupancyRate) {}

    /** Counter used to seed Tribuo trainers with deterministic but distinct seeds. */
    static final class Trainer {
        static final java.util.concurrent.atomic.AtomicLong NEXT_SEED = new java.util.concurrent.atomic.AtomicLong(42);
    }
}
