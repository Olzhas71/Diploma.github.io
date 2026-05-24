package com.parking.repository;

import com.parking.entity.Booking;
import com.parking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUserIdOrderByStartTimeDesc(Long userId, Pageable pageable);

    /**
     * Returns active or upcoming bookings overlapping the given time window for a specific spot.
     * Used to detect double-booking conflicts.
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.spot.id = :spotId
              AND b.status IN (com.parking.entity.BookingStatus.PENDING,
                               com.parking.entity.BookingStatus.CONFIRMED,
                               com.parking.entity.BookingStatus.ACTIVE)
              AND b.startTime < :end
              AND b.endTime   > :start
            """)
    List<Booking> findOverlapping(@Param("spotId") Long spotId,
                                  @Param("start") Instant start,
                                  @Param("end") Instant end);

    List<Booking> findByStatusAndEndTimeBefore(BookingStatus status, Instant cutoff);

    long countByStatus(BookingStatus status);
}
