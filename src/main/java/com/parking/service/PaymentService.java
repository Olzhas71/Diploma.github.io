package com.parking.service;

import com.parking.dto.payment.PaymentRequest;
import com.parking.dto.payment.PaymentResponse;
import com.parking.entity.*;
import com.parking.exception.BadRequestException;
import com.parking.exception.ConflictException;
import com.parking.exception.ResourceNotFoundException;
import com.parking.mapper.PaymentMapper;
import com.parking.repository.BookingRepository;
import com.parking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentResponse pay(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", request.bookingId()));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled booking");
        }
        if (booking.isCoveredBySubscription()) {
            throw new BadRequestException("Booking is covered by an active subscription — no payment needed");
        }
        paymentRepository.findByBookingId(booking.getId()).ifPresent(p -> {
            if (p.getStatus() == PaymentStatus.SUCCESS) {
                throw new ConflictException("Booking already paid");
            }
        });

        // In production: call out to a payment gateway (Stripe / Adyen / local PSP).
        // Here we simulate a synchronous successful charge.
        String externalId = "sim-" + UUID.randomUUID();
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .method(request.method())
                .status(PaymentStatus.SUCCESS)
                .externalTransactionId(externalId)
                .paidAt(Instant.now())
                .build();
        payment = paymentRepository.save(payment);
        log.info("Payment {} captured for booking {}: {} {}", externalId,
                booking.getId(), payment.getAmount(), payment.getCurrency());
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment for booking", bookingId));
    }

    @Transactional
    public PaymentResponse refund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", paymentId));
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Only successful payments can be refunded");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }
}
