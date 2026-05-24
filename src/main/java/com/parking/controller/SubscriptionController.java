package com.parking.controller;

import com.parking.dto.subscription.SubscriptionRequest;
import com.parking.dto.subscription.SubscriptionResponse;
import com.parking.entity.SubscriptionStatus;
import com.parking.service.SubscriptionService;
import com.parking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "Subscriptions")
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /** List my subscriptions. Filterable by status (ACTIVE, EXPIRED, CANCELLED). */
    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> my(
            @RequestParam(required = false) SubscriptionStatus status) {
        return ResponseEntity.ok(subscriptionService.list(SecurityUtils.requireUserId(), status));
    }

    /** Returns the active subscription covering this parking, or 404. */
    @GetMapping("/coverage")
    public ResponseEntity<SubscriptionResponse> coverage(@RequestParam Long parkingId) {
        return subscriptionService.activeCoverage(SecurityUtils.requireUserId(), parkingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/quote")
    public ResponseEntity<Map<String, Object>> quote(@RequestParam Long parkingId,
                                                     @RequestParam(defaultValue = "30") int durationDays) {
        BigDecimal price = subscriptionService.quote(parkingId, durationDays);
        return ResponseEntity.ok(Map.of(
                "parkingId", parkingId,
                "durationDays", durationDays,
                "price", price,
                "currency", "USD"
        ));
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> buy(@Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.create(SecurityUtils.requireUserId(), request));
    }

    @PostMapping("/{id}/renew")
    public ResponseEntity<SubscriptionResponse> renew(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.renew(SecurityUtils.requireUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.cancel(SecurityUtils.requireUserId(), id));
    }

    /** Admin-only view of every subscription in the system. */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<List<SubscriptionResponse>> all() {
        return ResponseEntity.ok(subscriptionService.listAll());
    }
}
