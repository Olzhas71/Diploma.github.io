package com.parking.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 255) String fullName,
        @Size(max = 32) String phone
) {}
