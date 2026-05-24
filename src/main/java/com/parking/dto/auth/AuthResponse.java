package com.parking.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String email,
        String role
) {}
