package com.mealio.service;

import com.mealio.dto.HeadcountResponse;
import com.mealio.exception.ResourceNotFoundException;
import com.mealio.model.entity.DailyLog;
import com.mealio.model.entity.Mess;
import com.mealio.port.in.HeadcountUseCase;
import com.mealio.port.out.HeadcountCachePort;
import com.mealio.repository.DailyLogRepository;
import com.mealio.repository.MessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Domain service: implements HeadcountUseCase.
 *
 * Firebase is fully abstracted behind HeadcountCachePort.
 * In dev, NoOpHeadcountAdapter is injected. In prod, FirebaseHeadcountAdapter.
 * This service never changes when the cache backend changes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeadcountService implements HeadcountUseCase {

    private final DailyLogRepository dailyLogRepository;
    private final MessRepository messRepository;
    private final HeadcountCachePort headcountCachePort; // DIP — interface, not Firebase

    private static final ZoneId BD_TZ = ZoneId.of("Asia/Dhaka");

    @Override
    @Transactional(readOnly = true)
    public HeadcountResponse getHeadcount(UUID messId) {
        Mess mess = messRepository.findById(messId)
                .orElseThrow(() -> new ResourceNotFoundException("Mess", messId));

        // Try cache first (Stale-While-Revalidate — NFR-2)
        return headcountCachePort.fetch(messId.toString())
                .orElseGet(() -> computeFromDb(mess));
    }

    @Override
    @Transactional(readOnly = true)
    public void refreshCache(Mess mess) {
        HeadcountResponse data = computeFromDb(mess);
        headcountCachePort.push(mess.getId().toString(), data);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private HeadcountResponse computeFromDb(Mess mess) {
        LocalDate today = LocalDate.now(BD_TZ);
        List<DailyLog> logs = dailyLogRepository.findByMessAndDate(mess, today);

        int memberCount = (int) logs.stream()
                .filter(dl -> dl.isBreakfast() || dl.isLunch() || dl.isDinner())
                .count();

        int guestCount = logs.stream().mapToInt(DailyLog::getGuestCount).sum();

        return new HeadcountResponse(
                mess.getId().toString(),
                mess.getName(),
                today,
                memberCount,
                guestCount,
                memberCount + guestCount,
                "database");
    }
}
