package com.parking.controller;

import com.parking.dto.booking.BookingRequest;
import com.parking.dto.booking.BookingResponse;
import com.parking.service.BookingService;
import com.parking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Bookings")
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<Page<BookingResponse>> myBookings(Pageable pageable) {
        return ResponseEntity.ok(bookingService.listForUser(SecurityUtils.requireUserId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getById(SecurityUtils.requireUserId(), id));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.create(SecurityUtils.requireUserId(), request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancel(SecurityUtils.requireUserId(), id));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<BookingResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.activate(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<BookingResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.complete(id));
    }
}
