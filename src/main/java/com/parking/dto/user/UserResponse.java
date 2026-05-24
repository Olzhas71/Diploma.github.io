package com.parking.dto.user;

import com.parking.entity.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role,
        boolean enabled,
        Instant createdAt
) {}
