package com.mealio.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Admin matrix cell — represents a single member's meal status on a given date.
 */
public record DailyLogDto(
        UUID logId,
        UUID memberId,
        String memberName,
        LocalDate date,
        boolean breakfast,
        boolean lunch,
        boolean dinner,
        int guestCount,
        boolean frozen) {
}
