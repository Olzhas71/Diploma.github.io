package com.parking.dto.subscription;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubscriptionRequest(
        @NotNull Long parkingId,
        @NotNull @Min(1) Integer durationDays
) {}
