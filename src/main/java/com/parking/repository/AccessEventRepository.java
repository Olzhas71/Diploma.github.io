package com.parking.repository;

import com.parking.entity.AccessEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AccessEventRepository extends JpaRepository<AccessEvent, Long> {
    Page<AccessEvent> findByParkingIdAndTimestampBetween(Long parkingId, Instant from, Instant to, Pageable pageable);
    Page<AccessEvent> findByParkingIdOrderByTimestampDesc(Long parkingId, Pageable pageable);
    Page<AccessEvent> findAllByOrderByTimestampDesc(Pageable pageable);
}
