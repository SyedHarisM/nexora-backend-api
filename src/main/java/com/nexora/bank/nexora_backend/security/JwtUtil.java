package com.nexora.bank.nexora_backend.security;

import com.nexora.bank.nexora_backend.entity.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Access-token issuing and verification.
 *
 * The signing key comes from configuration and has no fallback value. A
 * hardcoded default travels with the source, so anything able to read the
 * source could forge a token for any subject and any role. When the property is
 * missing the application fails to start: refusing to boot is safer than
 * booting with a key someone else already knows.
 */
@Component
public class JwtUtil {

    /** HS256 needs at least 256 bits of key material. */
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final Key signingKey;
    private final long accessExpirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.accessExpirationMs}") long accessExpirationMs) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret is not configured. Set the JWT_SECRET environment variable to a "
                            + "random value of at least " + MINIMUM_SECRET_BYTES + " bytes.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret is too short for HS256. Supply at least " + MINIMUM_SECRET_BYTES
                            + " bytes of key material.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationMs = accessExpirationMs;
    }

    // 🔑 Signing key, built once from configuration
    private Key getSignKey() {
        return signingKey;
    }

    // 🔐 Generate Token (EMAIL + ROLE)
    public String generateToken(String email, Role role) {

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 📌 Extract email
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 📌 Extract role
    public String extractRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    // ✔ Validate token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}