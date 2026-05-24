package com.parking.repository;

import com.parking.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByOwnerId(Long ownerId);
    Optional<Vehicle> findByLicensePlateIgnoreCase(String licensePlate);
    boolean existsByLicensePlateIgnoreCase(String licensePlate);
}
