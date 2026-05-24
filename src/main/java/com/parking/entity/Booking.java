package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_user", columnList = "user_id"),
        @Index(name = "idx_booking_spot_time", columnList = "spot_id, start_time, end_time"),
        @Index(name = "idx_booking_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private ParkingSpot spot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 8)
    @Builder.Default
    private String currency = "USD";

    /**
     * True if this booking is fully covered by an active subscription —
     * payment is then optional / skipped.
     */
    @Column(name = "covered_by_subscription", nullable = false)
    @Builder.Default
    private boolean coveredBySubscription = false;
}
