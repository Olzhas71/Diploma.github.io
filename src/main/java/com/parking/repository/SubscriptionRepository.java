package com.parking.repository;

import com.parking.entity.Subscription;
import com.parking.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<Subscription> findByUserIdOrderByValidToDesc(Long userId);

    List<Subscription> findByStatusAndValidToBefore(SubscriptionStatus status, Instant cutoff);

    long countByStatus(SubscriptionStatus status);

    /**
     * Returns the active subscription that currently covers the given user/parking,
     * if any. Used by booking flow to skip payment.
     */
    @Query("""
            SELECT s FROM Subscription s
            WHERE s.user.id = :userId
              AND s.parking.id = :parkingId
              AND s.status = com.parking.entity.SubscriptionStatus.ACTIVE
              AND s.validFrom <= :now
              AND s.validTo   >  :now
            """)
    Optional<Subscription> findActiveCoverage(@Param("userId") Long userId,
                                              @Param("parkingId") Long parkingId,
                                              @Param("now") Instant now);
}
