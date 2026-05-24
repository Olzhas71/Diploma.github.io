package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles", indexes = {
        @Index(name = "idx_vehicles_plate", columnList = "license_plate", unique = true),
        @Index(name = "idx_vehicles_owner", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false, unique = true, length = 32)
    private String licensePlate;

    @Column(length = 64)
    private String make;

    @Column(length = 64)
    private String model;

    @Column(length = 32)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
