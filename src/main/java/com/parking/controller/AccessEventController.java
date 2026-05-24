package com.parking.controller;

import com.parking.dto.access.AccessEventRequest;
import com.parking.dto.access.AccessEventResponse;
import com.parking.service.AccessEventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Access Events")
@RestController
@RequestMapping("/access-events")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AccessEventController {

    private final AccessEventService accessEventService;

    @GetMapping
    public ResponseEntity<Page<AccessEventResponse>> list(@RequestParam(required = false) Long parkingId,
                                                          Pageable pageable) {
        return ResponseEntity.ok(accessEventService.list(parkingId, pageable));
    }

    @PostMapping
    public ResponseEntity<AccessEventResponse> record(@Valid @RequestBody AccessEventRequest request) {
        return ResponseEntity.ok(accessEventService.record(request));
    }
}
