package com.mealio.adapter.cache;

import com.mealio.dto.HeadcountResponse;
import com.mealio.port.out.HeadcountCachePort;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Firebase Realtime DB implementation of HeadcountCachePort.
 * Active only when mealio.firebase.enabled=true.
 *
 * HeadcountService never imports anything Firebase — it calls this port.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mealio.firebase.enabled", havingValue = "true")
public class FirebaseHeadcountAdapter implements HeadcountCachePort {

    private static final String NODE = "headcount/";

    @Override
    public void push(String messId, HeadcountResponse data) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(NODE + messId);
            Map<String, Object> payload = Map.of(
                    "date", data.date().toString(),
                    "memberCount", data.memberCount(),
                    "guestCount", data.guestCount(),
                    "totalHeadcount", data.totalHeadcount(),
                    "updatedAt", System.currentTimeMillis());
            ref.setValueAsync(payload);
            log.debug("Firebase headcount pushed for mess {}: {}", messId, data.totalHeadcount());
        } catch (Exception e) {
            log.warn("Firebase push failed for mess {} (non-fatal): {}", messId, e.getMessage());
        }
    }

    /**
     * Read from Firebase is async — for the SWR pattern we return empty here
     * and let HeadcountService fall back to the DB query, which it already does.
     * A full async read would require CompletableFuture; keeping it simple for MVP.
     */
    @Override
    public Optional<HeadcountResponse> fetch(String messId) {
        return Optional.empty(); // DB is always the source of truth read-side for now
    }
}
