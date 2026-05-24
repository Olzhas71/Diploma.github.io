package com.parking.dto.payment;

import com.parking.entity.PaymentMethod;
import com.parking.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long bookingId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        String externalTransactionId,
        Instant paidAt
) {}
