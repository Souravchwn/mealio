package com.mealio.controller;

import com.mealio.security.JwtProperties;
import com.mealio.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * System-admin authentication endpoint.
 *
 * <p>
 * There is intentionally no user-registration flow for the system admin.
 * Credentials are configured via environment variables
 * ({@code JWT_ADMIN_USERNAME} /
 * {@code JWT_ADMIN_PASSWORD}) and validated here at login time.
 *
 * <p>
 * Successful login returns a compact HS256 JWT. All subsequent requests to
 * protected endpoints must include:
 * 
 * <pre>
 * Authorization: Bearer &lt;token&gt;
 * </pre>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final JwtService jwtService;
    private final JwtProperties jwtProps;

    // ── Request / Response records ─────────────────────────────────────────────

    public record LoginRequest(
            @NotBlank(message = "username is required") String username,
            @NotBlank(message = "password is required") String password) {
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            Instant issuedAt) {
    }

    public record ErrorResponse(int status, String message) {
    }

    // ── Endpoints ──────────────────────────────────────────────────────────────

    /**
     * {@code POST /api/auth/login}
     *
     * <p>
     * Validates credentials against the configured system-admin account and
     * issues a signed JWT on success.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {

        boolean validUsername = jwtProps.adminUsername().equals(req.username());
        boolean validPassword = jwtProps.adminPassword().equals(req.password());

        if (!validUsername || !validPassword) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(401, "Invalid credentials"));
        }

        String token = jwtService.generateToken(req.username(), "SYSTEM_ADMIN");

        return ResponseEntity.ok(new LoginResponse(
                token,
                "Bearer",
                jwtProps.expirationMs() / 1000, // seconds
                Instant.now()));
    }
}
