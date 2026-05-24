package com.parking.service;

import com.parking.dto.subscription.SubscriptionRequest;
import com.parking.dto.subscription.SubscriptionResponse;
import com.parking.entity.Parking;
import com.parking.entity.Subscription;
import com.parking.entity.SubscriptionStatus;
import com.parking.entity.User;
import com.parking.exception.BadRequestException;
import com.parking.exception.ForbiddenException;
import com.parking.exception.ResourceNotFoundException;
import com.parking.repository.ParkingRepository;
import com.parking.repository.SubscriptionRepository;
import com.parking.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Period-based subscriptions: a driver pays once for unlimited access to a
 * specific parking. Active subscriptions automatically zero out the price of
 * any new booking made for the covered parking (see {@code BookingService}).
 *
 * Pricing model: cheapest hourly tariff × (3 hours/day average usage) × N days
 * × 0.65 (35% volume discount).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ParkingRepository parkingRepository;
    private final TariffRepository tariffRepository;
    private final UserService userService;

    private static final BigDecimal DISCOUNT_FACTOR = new BigDecimal("0.65"); // 35% off

    @Transactional(readOnly = true)
    public BigDecimal quote(Long parkingId, int durationDays) {
        if (!parkingRepository.existsById(parkingId)) {
            throw ResourceNotFoundException.of("Parking", parkingId);
        }
        BigDecimal hourly = tariffRepository.findByParkingId(parkingId).stream()
                .map(t -> t.getPricePerHour())
                .min(BigDecimal::compareTo)
                .orElse(new BigDecimal("5.00"));
        return hourly.multiply(BigDecimal.valueOf(3L * durationDays))
                .multiply(DISCOUNT_FACTOR)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public SubscriptionResponse create(Long userId, SubscriptionRequest request) {
        User user = userService.loadById(userId);
        Parking parking = parkingRepository.findById(request.parkingId())
                .orElseThrow(() -> ResourceNotFoundException.of("Parking", request.parkingId()));
        // Reject duplicate active subscriptions for the same parking — extends
        // the validTo of the existing one instead would be friendlier, but
        // keeping it simple for now.
        Instant now = Instant.now();
        Optional<Subscription> existing = subscriptionRepository.findActiveCoverage(userId, parking.getId(), now);
        if (existing.isPresent()) {
            throw new BadRequestException(
                    "У вас уже есть активная подписка на эту парковку до " + existing.get().getValidTo());
        }
        BigDecimal price = quote(parking.getId(), request.durationDays());
        Subscription sub = Subscription.builder()
                .user(user)
                .parking(parking)
                .validFrom(now)
                .validTo(now.plus(Duration.ofDays(request.durationDays())))
                .price(price)
                .currency("USD")
                .status(SubscriptionStatus.ACTIVE)
                .build();
        sub = subscriptionRepository.save(sub);
        log.info("Subscription {} created: user={} parking={} price={} USD days={}",
                sub.getId(), userId, parking.getId(), price, request.durationDays());
        return toResponse(sub);
    }

    /**
     * Renew (or re-buy) the same kind of subscription as a previous one.
     * Re-uses the parking and original duration; resets validity to "now".
     */
    @Transactional
    public SubscriptionResponse renew(Long userId, Long previousId) {
        Subscription prev = subscriptionRepository.findById(previousId)
                .orElseThrow(() -> ResourceNotFoundException.of("Subscription", previousId));
        if (!prev.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Subscription does not belong to the current user");
        }
        long days = Math.max(1, ChronoUnit.DAYS.between(prev.getValidFrom(), prev.getValidTo()));
        return create(userId, new SubscriptionRequest(prev.getParking().getId(), (int) days));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> list(Long userId, SubscriptionStatus status) {
        List<Subscription> subs = status == null
                ? subscriptionRepository.findByUserIdOrderByValidToDesc(userId)
                : subscriptionRepository.findByUserIdAndStatus(userId, status);
        return subs.stream()
                .sorted(Comparator.comparing(Subscription::getValidTo).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> listAll() {
        return subscriptionRepository.findAll().stream()
                .sorted(Comparator.comparing(Subscription::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubscriptionResponse cancel(Long userId, Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Subscription", subscriptionId));
        if (!sub.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Subscription does not belong to the current user");
        }
        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Только активная подписка может быть отменена");
        }
        sub.setStatus(SubscriptionStatus.CANCELLED);
        return toResponse(subscriptionRepository.save(sub));
    }

    /**
     * Returns whether the user has an active subscription that covers the
     * given parking right now. Used by booking and parking detail UIs.
     */
    @Transactional(readOnly = true)
    public Optional<SubscriptionResponse> activeCoverage(Long userId, Long parkingId) {
        return subscriptionRepository.findActiveCoverage(userId, parkingId, Instant.now())
                .map(this::toResponse);
    }

    /**
     * Long-lived counter for the analytics dashboard.
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
    }

    /** Periodically expire subscriptions whose validTo has passed. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOutdated() {
        List<Subscription> expired = subscriptionRepository.findByStatusAndValidToBefore(
                SubscriptionStatus.ACTIVE, Instant.now());
        for (Subscription s : expired) {
            s.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(s);
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} subscriptions", expired.size());
        }
    }

    private SubscriptionResponse toResponse(Subscription sub) {
        return new SubscriptionResponse(
                sub.getId(),
                sub.getUser().getId(),
                sub.getParking().getId(),
                sub.getParking().getName(),
                sub.getValidFrom(),
                sub.getValidTo(),
                sub.getPrice(),
                sub.getCurrency(),
                sub.getStatus()
        );
    }
}
