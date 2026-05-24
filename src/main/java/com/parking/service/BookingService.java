package com.parking.service;

import com.parking.dto.booking.BookingRequest;
import com.parking.dto.booking.BookingResponse;
import com.parking.entity.*;
import com.parking.exception.BadRequestException;
import com.parking.exception.ConflictException;
import com.parking.exception.ForbiddenException;
import com.parking.exception.ResourceNotFoundException;
import com.parking.mapper.BookingMapper;
import com.parking.repository.BookingRepository;
import com.parking.repository.ParkingSpotRepository;
import com.parking.repository.SubscriptionRepository;
import com.parking.repository.VehicleRepository;
import com.parking.websocket.OccupancyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ParkingSpotRepository spotRepository;
    private final VehicleRepository vehicleRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;
    private final PricingService pricingService;
    private final BookingMapper bookingMapper;
    private final OccupancyWebSocketHandler webSocketHandler;

    @Transactional
    @CacheEvict(value = "statsOverview", allEntries = true)
    public BookingResponse create(Long userId, BookingRequest request) {
        validateTimes(request.startTime(), request.endTime());
        ParkingSpot spot = spotRepository.findByIdForUpdate(request.spotId())
                .orElseThrow(() -> ResourceNotFoundException.of("Spot", request.spotId()));
        if (spot.getStatus() == SpotStatus.MAINTENANCE) {
            throw new ConflictException("Spot is under maintenance");
        }
        List<Booking> overlap = bookingRepository.findOverlapping(spot.getId(), request.startTime(), request.endTime());
        if (!overlap.isEmpty()) {
            throw new ConflictException("Spot is already booked for the selected time window");
        }
        User user = userService.loadById(userId);
        Vehicle vehicle = null;
        if (request.vehicleId() != null) {
            vehicle = vehicleRepository.findById(request.vehicleId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", request.vehicleId()));
            if (!vehicle.getOwner().getId().equals(userId)) {
                throw new ForbiddenException("Vehicle does not belong to the current user");
            }
        }
        // If the user holds an active subscription that covers the booking
        // window for this parking, the booking is free and pre-paid.
        Long parkingId = spot.getParking().getId();
        boolean covered = subscriptionRepository
                .findActiveCoverage(userId, parkingId, request.startTime())
                .filter(sub -> !request.endTime().isAfter(sub.getValidTo()))
                .isPresent();
        BigDecimal total = covered
                ? BigDecimal.ZERO
                : pricingService.calculate(spot, request.startTime(), request.endTime());
        Booking booking = Booking.builder()
                .user(user)
                .spot(spot)
                .vehicle(vehicle)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(BookingStatus.CONFIRMED)
                .totalAmount(total)
                .currency("USD")
                .coveredBySubscription(covered)
                .build();
        booking = bookingRepository.save(booking);
        if (covered) {
            log.info("Booking {} covered by active subscription — payment skipped", booking.getId());
        }

        spot.setStatus(SpotStatus.RESERVED);
        spotRepository.save(spot);
        webSocketHandler.broadcastSpotUpdate(spot);

        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listForUser(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByStartTimeDesc(userId, pageable)
                .map(bookingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(Long userId, Long bookingId) {
        Booking b = loadOwned(userId, bookingId);
        return bookingMapper.toResponse(b);
    }

    @Transactional
    @CacheEvict(value = "statsOverview", allEntries = true)
    public BookingResponse cancel(Long userId, Long bookingId) {
        Booking booking = loadOwned(userId, bookingId);
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking cannot be cancelled in its current state");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        ParkingSpot spot = booking.getSpot();
        if (spot.getStatus() == SpotStatus.RESERVED || spot.getStatus() == SpotStatus.OCCUPIED) {
            spot.setStatus(SpotStatus.FREE);
            spotRepository.save(spot);
            webSocketHandler.broadcastSpotUpdate(spot);
        }
        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse activate(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed bookings can be activated");
        }
        booking.setStatus(BookingStatus.ACTIVE);
        ParkingSpot spot = booking.getSpot();
        spot.setStatus(SpotStatus.OCCUPIED);
        spotRepository.save(spot);
        webSocketHandler.broadcastSpotUpdate(spot);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse complete(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
        booking.setStatus(BookingStatus.COMPLETED);
        ParkingSpot spot = booking.getSpot();
        spot.setStatus(SpotStatus.FREE);
        spotRepository.save(spot);
        webSocketHandler.broadcastSpotUpdate(spot);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    /**
     * Periodically expires bookings whose end time has passed and frees their spots.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireFinishedBookings() {
        Instant now = Instant.now();
        List<Booking> expired = bookingRepository.findByStatusAndEndTimeBefore(BookingStatus.ACTIVE, now);
        expired.addAll(bookingRepository.findByStatusAndEndTimeBefore(BookingStatus.CONFIRMED, now));
        for (Booking b : expired) {
            b.setStatus(BookingStatus.COMPLETED);
            ParkingSpot spot = b.getSpot();
            if (spot.getStatus() != SpotStatus.MAINTENANCE) {
                spot.setStatus(SpotStatus.FREE);
                spotRepository.save(spot);
                webSocketHandler.broadcastSpotUpdate(spot);
            }
            bookingRepository.save(b);
        }
        if (!expired.isEmpty()) {
            log.info("Auto-completed {} expired bookings", expired.size());
        }
    }

    private Booking loadOwned(Long userId, Long bookingId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
        if (!b.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Booking does not belong to the current user");
        }
        return b;
    }

    private void validateTimes(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw new BadRequestException("endTime must be after startTime");
        }
        if (Duration.between(start, end).toMinutes() < 15) {
            throw new BadRequestException("Minimum booking duration is 15 minutes");
        }
        if (Duration.between(start, end).toDays() > 30) {
            throw new BadRequestException("Maximum booking duration is 30 days");
        }
    }
}
