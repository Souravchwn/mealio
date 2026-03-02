package com.mealio.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds to the {@code mealio.jwt} block in application.yml.
 *
 * <pre>
 * mealio:
 *   jwt:
 *     secret: &lt;hex-encoded 256-bit key&gt;
 *     expiration-ms: 86400000
 *     admin-username: sysadmin
 *     admin-password: ChangeMe_Prod!
 * </pre>
 */
@ConfigurationProperties(prefix = "mealio.jwt")
public record JwtProperties(
        String secret,
        long expirationMs,
        String adminUsername,
        String adminPassword) {
}
