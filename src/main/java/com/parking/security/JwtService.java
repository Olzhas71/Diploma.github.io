package com.parking.security;

import com.parking.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;
    private final String issuer;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-expiration-ms}") long accessExpirationMs,
                      @Value("${app.jwt.refresh-token-expiration-ms}") long refreshExpirationMs,
                      @Value("${app.jwt.issuer}") String issuer) {
        // Accept either base64-encoded or plain string secrets.
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
            if (bytes.length < 32) bytes = secret.getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.issuer = issuer;
    }

    public String generateAccessToken(User user) {
        return buildToken(user, accessExpirationMs, "access");
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpirationMs, "refresh");
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000L;
    }

    private String buildToken(User user, long ttlMs, String type) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .claims(Map.of(
                        "uid", user.getId(),
                        "role", user.getRole().name(),
                        "typ", type
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new JwtException("Invalid or expired token");
        }
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("typ", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("typ", String.class));
    }
}
