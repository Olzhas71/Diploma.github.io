package com.parking.controller;

import com.parking.dto.payment.PaymentRequest;
import com.parking.dto.payment.PaymentResponse;
import com.parking.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.pay(request));
    }

    @GetMapping("/by-booking/{bookingId}")
    public ResponseEntity<PaymentResponse> byBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getByBooking(bookingId));
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<PaymentResponse> refund(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.refund(paymentId));
    }
}
