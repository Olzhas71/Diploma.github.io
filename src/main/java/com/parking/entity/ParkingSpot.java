package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parking_spots",
        uniqueConstraints = @UniqueConstraint(name = "uk_spot_per_parking", columnNames = {"parking_id", "spot_number"}),
        indexes = {
                @Index(name = "idx_spots_parking", columnList = "parking_id"),
                @Index(name = "idx_spots_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_id", nullable = false)
    private Parking parking;

    @Column(name = "spot_number", nullable = false, length = 16)
    private String spotNumber;

    @Column(name = "level")
    private Integer level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private SpotType type = SpotType.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private SpotStatus status = SpotStatus.FREE;

    @Version
    private Long version;
}
