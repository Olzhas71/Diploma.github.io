package com.parking.dto.parking;

import com.parking.entity.SpotStatus;
import com.parking.entity.SpotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpotRequest(
        @NotBlank @Size(max = 16) String spotNumber,
        Integer level,
        @NotNull SpotType type,
        SpotStatus status
) {}
