package com.parking.repository;

import com.parking.entity.Parking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingRepository extends JpaRepository<Parking, Long> {

    /**
     * Approximate radius search using Haversine formula in degrees.
     * Distance is in kilometers.
     */
    @Query(value = """
            SELECT p.* FROM parkings p
            WHERE (6371 * acos(
                cos(radians(:lat)) * cos(radians(p.latitude)) *
                cos(radians(p.longitude) - radians(:lon)) +
                sin(radians(:lat)) * sin(radians(p.latitude))
            )) <= :radiusKm
            ORDER BY (6371 * acos(
                cos(radians(:lat)) * cos(radians(p.latitude)) *
                cos(radians(p.longitude) - radians(:lon)) +
                sin(radians(:lat)) * sin(radians(p.latitude))
            )) ASC
            """, nativeQuery = true)
    List<Parking> findNearby(@Param("lat") double lat,
                             @Param("lon") double lon,
                             @Param("radiusKm") double radiusKm);
}
