package com.parking.repository;

import com.parking.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findBySpotIdAndTimestampBetweenOrderByTimestampAsc(
            Long spotId, Instant from, Instant to);

    @Query("SELECT DISTINCT sr.spot.id FROM SensorReading sr")
    Set<Long> findSpotIdsWithReadings();

    /**
     * Returns hourly occupancy ratios for a parking over the given period.
     * Used by the ML prediction service.
     */
    @Query(value = """
            SELECT CAST(EXTRACT(HOUR FROM sr.timestamp) AS INTEGER) AS hour_of_day,
                   AVG(CASE WHEN sr.occupied THEN 1.0 ELSE 0.0 END) AS occupancy_rate,
                   COUNT(*) AS samples
            FROM sensor_readings sr
            JOIN parking_spots ps ON ps.id = sr.spot_id
            WHERE ps.parking_id = :parkingId
              AND sr.timestamp >= :from
              AND sr.timestamp <  :to
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> findHourlyOccupancy(@Param("parkingId") Long parkingId,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);
}
