package com.parking.repository;

import com.parking.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {
    List<Tariff> findByParkingId(Long parkingId);
}
