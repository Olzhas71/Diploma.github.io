package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parkings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 512)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ParkingType type;

    @Column(name = "total_spots", nullable = false)
    private Integer totalSpots;

    @Column(name = "working_hours_from")
    private LocalTime workingHoursFrom;

    @Column(name = "working_hours_to")
    private LocalTime workingHoursTo;

    @OneToMany(mappedBy = "parking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParkingSpot> spots = new ArrayList<>();

    @OneToMany(mappedBy = "parking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tariff> tariffs = new ArrayList<>();
}
