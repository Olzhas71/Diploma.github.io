package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "access_events", indexes = {
        @Index(name = "idx_access_parking_ts", columnList = "parking_id, timestamp"),
        @Index(name = "idx_access_plate", columnList = "license_plate_recognized")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_id", nullable = false)
    private Parking parking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "license_plate_recognized", length = 32)
    private String licensePlateRecognized;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private AccessEventType eventType;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;
}
