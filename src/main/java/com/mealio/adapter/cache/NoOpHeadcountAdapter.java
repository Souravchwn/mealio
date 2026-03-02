package com.mealio.adapter.cache;

import com.mealio.dto.HeadcountResponse;
import com.mealio.port.out.HeadcountCachePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * No-op HeadcountCachePort — active when mealio.firebase.enabled=false (default
 * in dev).
 *
 * push() → just logs, no external call.
 * fetch() → always empty, HeadcountService falls back to DB (correct
 * behaviour).
 *
 * This lets the entire application start cleanly locally with zero Firebase
 * config.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mealio.firebase.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpHeadcountAdapter implements HeadcountCachePort {

    @Override
    public void push(String messId, HeadcountResponse headcount) {
        log.debug("[NoOp] Headcount cache push skipped for mess {} (Firebase disabled)", messId);
    }

    @Override
    public Optional<HeadcountResponse> fetch(String messId) {
        return Optional.empty(); // always triggers DB fallback
    }
}
