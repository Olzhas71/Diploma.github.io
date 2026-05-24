package com.parking.dto.payment;

import com.parking.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull Long bookingId,
        @NotNull PaymentMethod method
) {}
