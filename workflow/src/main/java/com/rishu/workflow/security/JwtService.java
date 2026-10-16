package com.rishu.workflow.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // HS256 needs a key of at least 256 bits
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is missing: set the JWT_SECRET environment variable (app.jwt.secret)");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret is too short: JWT_SECRET (app.jwt.secret) must be at least "
                            + MIN_SECRET_BYTES + " bytes, got " + secretBytes.length);
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token) {

        try {

            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}