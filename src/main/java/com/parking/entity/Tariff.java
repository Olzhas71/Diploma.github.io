package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "tariffs", indexes = {
        @Index(name = "idx_tariff_parking", columnList = "parking_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tariff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_id", nullable = false)
    private Parking parking;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "price_per_hour", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false, length = 8)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 16)
    private DayOfWeek dayOfWeek;

    @Column(name = "hour_from")
    private LocalTime hourFrom;

    @Column(name = "hour_to")
    private LocalTime hourTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 32)
    private SpotType vehicleType;

    @Column(name = "dynamic_multiplier", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal dynamicMultiplier = BigDecimal.ONE;
}
