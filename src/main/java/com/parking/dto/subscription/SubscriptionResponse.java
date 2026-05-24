package com.parking.dto.subscription;

import com.parking.entity.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record SubscriptionResponse(
        Long id,
        Long userId,
        Long parkingId,
        String parkingName,
        Instant validFrom,
        Instant validTo,
        BigDecimal price,
        String currency,
        SubscriptionStatus status
) {}
