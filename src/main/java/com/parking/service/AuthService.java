package com.parking.service;

import com.parking.dto.auth.AuthResponse;
import com.parking.dto.auth.LoginRequest;
import com.parking.dto.auth.RefreshTokenRequest;
import com.parking.dto.auth.RegisterRequest;
import com.parking.entity.Role;
import com.parking.entity.User;
import com.parking.exception.BadRequestException;
import com.parking.exception.ConflictException;
import com.parking.exception.ResourceNotFoundException;
import com.parking.repository.UserRepository;
import com.parking.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new ConflictException("Email already registered");
        }
        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .role(Role.DRIVER)
                .enabled(true)
                .build();
        userRepository.save(user);
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> ResourceNotFoundException.of("User", request.email()));
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        Claims claims = jwtService.parse(request.refreshToken());
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadRequestException("Not a refresh token");
        }
        User user = userRepository.findByEmail(claims.getSubject())
                .orElseThrow(() -> ResourceNotFoundException.of("User", claims.getSubject()));
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
