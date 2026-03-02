package com.mealio.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security configuration.
 *
 * <p>
 * Route access rules:
 * <ul>
 * <li>{@code POST /api/auth/login} — public (issues token)</li>
 * <li>{@code GET  /api/telegram/**} — public (Telegram health-check /
 * webhook)</li>
 * <li>{@code POST /api/telegram/webhook} — public (Telegram sends no auth
 * header)</li>
 * <li>{@code /api/admin/**} — requires ROLE_SYSTEM_ADMIN</li>
 * <li>Everything else — requires any authenticated caller</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless REST — no CSRF needed
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless — no sessions
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Route access rules
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoint — public
                        .requestMatchers("/api/auth/**").permitAll()
                        // Telegram webhook — Telegram sends no Authorization header
                        .requestMatchers("/api/telegram/**").permitAll()
                        // Admin panel — system-admin role required
                        .requestMatchers("/api/admin/**").hasRole("SYSTEM_ADMIN")
                        // Cook & expense APIs — any authenticated caller
                        .anyRequest().authenticated())

                // Plug in our JWT filter before Spring's default UsernamePassword filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
