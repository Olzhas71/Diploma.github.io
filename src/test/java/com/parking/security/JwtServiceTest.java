package com.parking.security;

import com.parking.entity.Role;
import com.parking.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(
                "test-secret-key-test-secret-key-test-secret-key-1234567890",
                3_600_000L, 86_400_000L, "parking-system-test");
    }

    @Test
    void roundTripsAccessToken() {
        User user = User.builder().id(42L).email("u@x.io").role(Role.DRIVER).build();
        String token = service.generateAccessToken(user);

        Claims claims = service.parse(token);

        assertThat(claims.getSubject()).isEqualTo("u@x.io");
        assertThat(claims.get("uid", Integer.class)).isEqualTo(42);
        assertThat(claims.get("role", String.class)).isEqualTo("DRIVER");
        assertThat(service.isAccessToken(claims)).isTrue();
        assertThat(service.isRefreshToken(claims)).isFalse();
    }

    @Test
    void rejectsTamperedToken() {
        User user = User.builder().id(1L).email("a@b.io").role(Role.ADMIN).build();
        String token = service.generateAccessToken(user);
        // Flip a character in the signature segment to invalidate the HMAC.
        int lastDot = token.lastIndexOf('.');
        char c = token.charAt(lastDot + 1);
        char replacement = c == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, lastDot + 1) + replacement + token.substring(lastDot + 2);
        assertThatThrownBy(() -> service.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void distinguishesAccessAndRefresh() {
        User user = User.builder().id(7L).email("z@y.io").role(Role.DRIVER).build();
        Claims refresh = service.parse(service.generateRefreshToken(user));
        assertThat(service.isRefreshToken(refresh)).isTrue();
        assertThat(service.isAccessToken(refresh)).isFalse();
    }
}
