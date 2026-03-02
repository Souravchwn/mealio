package com.mealio.dto;

import java.time.LocalDate;

/**
 * Cook's dashboard headcount — pulled from Firebase cache for low-latency
 * display.
 */
public record HeadcountResponse(
        String messId,
        String messName,
        LocalDate date,
        int memberCount, // members with at least 1 meal on today
        int guestCount, // total guest meals today
        int totalHeadcount, // memberCount + guestCount
        String source // "firebase" | "database" (fallback)
) {
}
