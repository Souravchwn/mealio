package com.mealio.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Creates and validates compact HMAC-SHA256 JWTs for System Admin access.
 *
 * <p>
 * The secret is a hex string from {@code mealio.jwt.secret} — it must be
 * at least 64 hex characters (32 bytes → 256 bits) so that JJWT accepts it
 * for HS256.
 */
@Component
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    // ── key ───────────────────────────────────────────────────────────────────

    private SecretKey key() {
        // Decode the hex string to raw bytes, then build an HMAC key.
        byte[] keyBytes = hexToBytes(props.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── generation ────────────────────────────────────────────────────────────

    /**
     * Generates a signed HS256 JWT for the given subject (username).
     *
     * @param subject usually the admin username
     * @param role    a claim value, e.g. "SYSTEM_ADMIN"
     * @return compact JWT string
     */
    public String generateToken(String subject, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + props.expirationMs());

        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    // ── validation / extraction ───────────────────────────────────────────────

    /**
     * Validates the token and extracts its claims.
     *
     * @param token compact JWT
     * @return parsed {@link Claims}
     * @throws JwtException if the token is invalid or expired
     */
    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Convenience — extract subject (username) from a valid token. */
    public String extractSubject(String token) {
        return validateAndExtract(token).getSubject();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
