package com.parking.ml;

import org.tribuo.DataSource;
import org.tribuo.Example;
import org.tribuo.OutputFactory;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.DataSourceProvenance;
import org.tribuo.provenance.SimpleDataSourceProvenance;
import org.tribuo.regression.RegressionFactory;
import org.tribuo.regression.Regressor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tribuo {@link DataSource} backed by a plain in-memory list of
 * {@link OccupancyModelService.Sample} rows. Each sample is converted into a
 * Tribuo example with the four features used by the model.
 */
final class InMemoryDataSource implements DataSource<Regressor> {

    private static final RegressionFactory FACTORY = new RegressionFactory();
    private static final String OUTPUT_NAME = "occupancyRate";
    private static final String[] FEATURE_NAMES = {
            "hour_of_day", "day_of_week", "is_weekend", "parking_id"
    };

    private final List<Example<Regressor>> examples;
    private final SimpleDataSourceProvenance provenance;

    InMemoryDataSource(List<OccupancyModelService.Sample> samples) {
        this.examples = new ArrayList<>(samples.size());
        for (OccupancyModelService.Sample s : samples) {
            boolean weekend = s.dayOfWeek() == 6 || s.dayOfWeek() == 7;
            this.examples.add(new ArrayExample<>(
                    new Regressor(OUTPUT_NAME, s.occupancyRate()),
                    FEATURE_NAMES,
                    new double[]{s.hourOfDay(), s.dayOfWeek(), weekend ? 1 : 0, s.parkingId()}));
        }
        this.provenance = new SimpleDataSourceProvenance(
                "occupancy-aggregated", OffsetDateTime.now(), FACTORY);
    }

    @Override
    public OutputFactory<Regressor> getOutputFactory() {
        return FACTORY;
    }

    @Override
    public DataSourceProvenance getProvenance() {
        return provenance;
    }

    @Override
    public Iterator<Example<Regressor>> iterator() {
        return examples.iterator();
    }
}
