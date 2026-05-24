package com.parking.dto.access;

import com.parking.entity.AccessEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccessEventRequest(
        @NotNull Long parkingId,
        @NotBlank @Size(max = 32) String licensePlate,
        @NotNull AccessEventType eventType,
        @Size(max = 512) String photoUrl
) {}
