package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sensor_readings", indexes = {
        @Index(name = "idx_sensor_spot_ts", columnList = "spot_id, timestamp"),
        @Index(name = "idx_sensor_ts", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private ParkingSpot spot;

    @Column(nullable = false)
    private boolean occupied;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "sensor_id", length = 64)
    private String sensorId;
}
