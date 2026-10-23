package com.nexora.bank.nexora_backend.security;

import com.nexora.bank.nexora_backend.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the behaviour of the signing-key change.
 *
 * The key used to be a literal in this class, so anyone reading the published
 * source could sign a token for any subject and any role. These tests assert
 * that a token signed with a different key is rejected, and that the component
 * refuses to exist at all without usable key material rather than quietly
 * falling back to a known default.
 */
class JwtSecretConfigurationTest {

    private static final String VALID_SECRET =
            "a-test-signing-key-of-more-than-thirty-two-bytes-0123456789";
    private static final long ONE_HOUR = 3_600_000L;

    @Test
    @DisplayName("a token signed with a different key is rejected")
    void tokenSignedWithForeignKeyIsRejected() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, ONE_HOUR);

        String forged = Jwts.builder()
                .setSubject("attacker@gmail.com")
                .claim("role", Role.ADMIN.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ONE_HOUR))
                .signWith(
                        Keys.hmacShaKeyFor(
                                "a-completely-different-key-also-long-enough-987654321"
                                        .getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        assertThat(jwtUtil.validateToken(forged)).isFalse();
    }

    @Test
    @DisplayName("a token this instance issued is accepted and carries its claims")
    void ownTokenRoundTrips() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, ONE_HOUR);

        String token = jwtUtil.generateToken("owner@gmail.com", Role.USER);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("owner@gmail.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("USER");
    }

    @Test
    @DisplayName("no secret means the component refuses to be created")
    void missingSecretFailsFast() {
        assertThatThrownBy(() -> new JwtUtil(null, ONE_HOUR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.jwt.secret");

        assertThatThrownBy(() -> new JwtUtil("   ", ONE_HOUR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.jwt.secret");
    }

    @Test
    @DisplayName("a short secret is refused rather than silently weakening HS256")
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtUtil("too-short", ONE_HOUR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short");
    }

    @Test
    @DisplayName("an administrator claim on a foreign-signed token buys nothing")
    void forgedAdminClaimIsRejected() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, ONE_HOUR);

        // The point of the old hardcoded key was that anyone holding it could
        // mint a token claiming any role. The key itself is deliberately not
        // reproduced here — it is already published, and copying it into the
        // current tree would outlive any future history cleanup.
        String forgedAdmin = Jwts.builder()
                .setSubject("attacker@gmail.com")
                .claim("role", Role.ADMIN.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ONE_HOUR))
                .signWith(
                        Keys.hmacShaKeyFor(
                                "any-key-that-is-not-the-configured-one-1234567890"
                                        .getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        assertThat(jwtUtil.validateToken(forgedAdmin)).isFalse();
    }
}
