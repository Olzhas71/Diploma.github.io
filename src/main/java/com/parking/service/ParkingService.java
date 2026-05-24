package com.parking.service;

import com.parking.dto.parking.*;
import com.parking.entity.*;
import com.parking.exception.ResourceNotFoundException;
import com.parking.mapper.ParkingMapper;
import com.parking.repository.ParkingRepository;
import com.parking.repository.ParkingSpotRepository;
import com.parking.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private final ParkingRepository parkingRepository;
    private final ParkingSpotRepository spotRepository;
    private final TariffRepository tariffRepository;
    private final ParkingMapper parkingMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "parkings", key = "#id")
    public ParkingResponse getById(Long id) {
        Parking p = loadParking(id);
        return enrich(p);
    }

    @Transactional(readOnly = true)
    public List<ParkingResponse> listAll() {
        return parkingRepository.findAll().stream().map(this::enrich).toList();
    }

    @Transactional(readOnly = true)
    public List<ParkingResponse> findNearby(double lat, double lon, double radiusKm) {
        return parkingRepository.findNearby(lat, lon, radiusKm).stream()
                .map(this::enrich)
                .toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "parkings", allEntries = true),
            @CacheEvict(value = "statsOverview", allEntries = true),
            @CacheEvict(value = "occupancyForecast", allEntries = true)
    })
    public ParkingResponse create(ParkingRequest request) {
        Parking parking = parkingMapper.toEntity(request);
        parking = parkingRepository.save(parking);
        // Auto-generate the requested number of REGULAR spots so the parking is
        // immediately bookable. Admin can later edit individual spots, change
        // their type to DISABLED/ELECTRIC, or add more via the bulk endpoint.
        generateSpots(parking, parking.getTotalSpots(), "S");
        // Auto-create a default tariff so the pricing engine has something to fall back on.
        Tariff defaultTariff = Tariff.builder()
                .parking(parking)
                .name("Standard hourly")
                .pricePerHour(new BigDecimal("5.00"))
                .currency("USD")
                .dynamicMultiplier(BigDecimal.ONE)
                .build();
        tariffRepository.save(defaultTariff);
        return enrich(parking);
    }

    /**
     * Bulk-creates {@code count} spots starting from the next free index for the
     * given prefix. Useful for retrofitting parkings that were created before
     * auto-generation was added, or for expanding capacity.
     *
     * @return the freshly created spots (in insertion order).
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "parkings", allEntries = true),
            @CacheEvict(value = "statsOverview", allEntries = true)
    })
    public List<SpotResponse> bulkCreateSpots(Long parkingId, int count, String prefix) {
        Parking parking = loadParking(parkingId);
        if (count <= 0 || count > 10_000) {
            throw new com.parking.exception.BadRequestException("count must be between 1 and 10000");
        }
        String safePrefix = (prefix == null || prefix.isBlank()) ? "S" : prefix.trim();
        List<ParkingSpot> created = generateSpots(parking, count, safePrefix);
        // Bump totalSpots so the dashboard count reflects reality.
        parking.setTotalSpots(parking.getTotalSpots() == null
                ? created.size()
                : parking.getTotalSpots() + created.size());
        parkingRepository.save(parking);
        return created.stream().map(parkingMapper::toSpotResponse).toList();
    }

    private List<ParkingSpot> generateSpots(Parking parking, int count, String prefix) {
        // Find the next free index for the prefix to avoid colliding with existing spots.
        List<ParkingSpot> existing = spotRepository.findByParkingId(parking.getId());
        int start = 1;
        for (ParkingSpot s : existing) {
            String n = s.getSpotNumber();
            if (n != null && n.startsWith(prefix)) {
                try {
                    int v = Integer.parseInt(n.substring(prefix.length()));
                    if (v >= start) start = v + 1;
                } catch (NumberFormatException ignored) { /* not a numeric suffix */ }
            }
        }
        List<ParkingSpot> spots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            spots.add(ParkingSpot.builder()
                    .parking(parking)
                    .spotNumber(prefix + (start + i))
                    .level(1)
                    .type(SpotType.REGULAR)
                    .status(SpotStatus.FREE)
                    .build());
        }
        return spotRepository.saveAll(spots);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "parkings", key = "#id"),
            @CacheEvict(value = "statsOverview", allEntries = true)
    })
    public ParkingResponse update(Long id, ParkingRequest request) {
        Parking parking = loadParking(id);
        parkingMapper.update(request, parking);
        return enrich(parkingRepository.save(parking));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "parkings", allEntries = true),
            @CacheEvict(value = "statsOverview", allEntries = true),
            @CacheEvict(value = "occupancyForecast", allEntries = true)
    })
    public void delete(Long id) {
        Parking parking = loadParking(id);
        parkingRepository.delete(parking);
    }

    @Transactional(readOnly = true)
    public List<SpotResponse> listSpots(Long parkingId) {
        loadParking(parkingId);
        return spotRepository.findByParkingId(parkingId).stream()
                .map(parkingMapper::toSpotResponse)
                .toList();
    }

    @Transactional
    public SpotResponse addSpot(Long parkingId, SpotRequest request) {
        Parking parking = loadParking(parkingId);
        ParkingSpot spot = parkingMapper.toSpotEntity(request);
        spot.setParking(parking);
        if (spot.getStatus() == null) spot.setStatus(SpotStatus.FREE);
        return parkingMapper.toSpotResponse(spotRepository.save(spot));
    }

    @Transactional
    public SpotResponse updateSpot(Long parkingId, Long spotId, SpotRequest request) {
        ParkingSpot spot = loadSpot(parkingId, spotId);
        parkingMapper.updateSpot(request, spot);
        return parkingMapper.toSpotResponse(spotRepository.save(spot));
    }

    @Transactional
    public void deleteSpot(Long parkingId, Long spotId) {
        ParkingSpot spot = loadSpot(parkingId, spotId);
        spotRepository.delete(spot);
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> listTariffs(Long parkingId) {
        loadParking(parkingId);
        return tariffRepository.findByParkingId(parkingId).stream()
                .map(parkingMapper::toTariffResponse)
                .toList();
    }

    @Transactional
    public TariffResponse addTariff(Long parkingId, TariffRequest request) {
        Parking parking = loadParking(parkingId);
        Tariff tariff = parkingMapper.toTariffEntity(request);
        tariff.setParking(parking);
        return parkingMapper.toTariffResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public void deleteTariff(Long tariffId) {
        if (!tariffRepository.existsById(tariffId)) {
            throw ResourceNotFoundException.of("Tariff", tariffId);
        }
        tariffRepository.deleteById(tariffId);
    }

    Parking loadParking(Long id) {
        return parkingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Parking", id));
    }

    private ParkingSpot loadSpot(Long parkingId, Long spotId) {
        ParkingSpot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> ResourceNotFoundException.of("Spot", spotId));
        if (!spot.getParking().getId().equals(parkingId)) {
            throw ResourceNotFoundException.of("Spot", spotId);
        }
        return spot;
    }

    private ParkingResponse enrich(Parking parking) {
        long free = spotRepository.countByParkingIdAndStatus(parking.getId(), SpotStatus.FREE);
        ParkingResponse base = parkingMapper.toResponse(parking);
        return new ParkingResponse(base.id(), base.name(), base.address(),
                base.latitude(), base.longitude(), base.type(), base.totalSpots(),
                free, base.workingHoursFrom(), base.workingHoursTo());
    }
}
