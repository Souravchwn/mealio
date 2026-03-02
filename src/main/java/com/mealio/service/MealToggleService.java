package com.mealio.service;

import com.mealio.exception.CutOffTimeExceededException;
import com.mealio.exception.MonthAlreadyClosedException;
import com.mealio.exception.ResourceNotFoundException;
import com.mealio.model.entity.DailyLog;
import com.mealio.model.entity.Member;
import com.mealio.model.entity.Mess;
import com.mealio.port.in.BotCommand;
import com.mealio.port.in.HeadcountUseCase;
import com.mealio.port.in.MealToggleUseCase;
import com.mealio.port.out.MemberIdentityPort;
import com.mealio.repository.DailyLogRepository;
import com.mealio.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Domain service: MealToggleUseCase implementation.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * CONCURRENCY DESIGN (Thread Safety):
 * ──────────────────────────────────────────────────────────────────────────────
 *
 * Problem: A member double-taps /off within milliseconds. Two HTTP requests
 * arrive at the same time. Both could read "no DailyLog exists", both create
 * one, and one of them hits the UNIQUE(member_id, date) constraint → 500 error.
 *
 * Solution — THREE LAYERS OF DEFENSE:
 *
 * Layer 1 — Pessimistic Lock (via findByIdWithLock)
 * We lock the Member row with SELECT FOR UPDATE before touching DailyLog.
 * This serializes all operations for the same member within the DB layer.
 * Different members can still run fully in parallel.
 *
 * Layer 2 — Optimistic Lock (@Version on DailyLog)
 * If somehow two transactions bypass the pessimistic lock (e.g., lock timeout
 * on a slow DB), Hibernate's @Version check fires. The second committer gets
 * ObjectOptimisticLockingFailureException — a clean, typed exception.
 *
 * Layer 3 — @Retryable
 * On OCC failure or lock acquisition failure, Spring Retry re-runs the entire
 * method (with a fresh transaction) up to 3 times with exponential back-off.
 * If all retries fail, @Recover sends a friendly error reply.
 *
 * NOTE: @Retryable must be on the OUTER proxy (before @Transactional).
 * With @EnableRetry(proxyTargetClass=true), Spring Boot creates the retry
 * advice OUTSIDE the transaction proxy — each retry starts a fresh TX. ✅
 * ──────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealToggleService implements MealToggleUseCase {

    private final MemberIdentityPort memberIdentityPort;
    private final MemberRepository memberRepository;
    private final DailyLogRepository dailyLogRepository;
    private final HeadcountUseCase headcountUseCase;

    private static final ZoneId BD_TZ = ZoneId.of("Asia/Dhaka");

    /**
     * Layer 3: Retry on transient concurrency failures.
     * maxAttempts=3, jitter back-off prevents retry storms.
     */
    @Override
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class,
            CannotAcquireLockException.class
    }, maxAttempts = 3, backoff = @Backoff(delay = 80, multiplier = 2, random = true))
    @Transactional(timeout = 10)
    public String processCommand(BotCommand command) {
        // ── Resolve member identity (platform-agnostic) ───────────────────────
        Member member = memberIdentityPort.resolve(command.platformUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Your account is not registered in any mess. Ask your Admin to add you."));

        // ── Layer 1: Pessimistic lock on the Member row
        // ────────────────────────────────
        // SELECT * FROM member WHERE id = ? FOR UPDATE (5s timeout)
        // All subsequent DailyLog operations for THIS member are serialized.
        // Other members in the same mess are completely unaffected.
        final Member lockedMember = memberRepository.findByIdWithLock(member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member", member.getId()));

        Mess mess = lockedMember.getMess();

        // ── Cut-off time guard ──────────────────────────────────────────────────
        LocalTime nowBD = LocalTime.now(BD_TZ);
        if (nowBD.isAfter(mess.getCutOffTime())) {
            throw new CutOffTimeExceededException(mess.getName(), mess.getCutOffTime().toString());
        }

        // ── Find or create today's DailyLog (safe — member row is locked) ────
        LocalDate today = LocalDate.now(BD_TZ);
        DailyLog dailyLog = dailyLogRepository.findByMemberAndDate(lockedMember, today)
                .orElseGet(() -> DailyLog.builder()
                        .member(lockedMember)
                        .date(today)
                        .build());

        if (dailyLog.isFrozen()) {
            throw new MonthAlreadyClosedException(today.toString().substring(0, 7));
        }

        // ── Parse command ────────────────────────────────────────────────────────────
        String cmd = command.text().trim().toLowerCase();
        if (cmd.contains("@"))
            cmd = cmd.substring(0, cmd.indexOf('@'));

        String reply = applyCommand(dailyLog, cmd, lockedMember.getName());

        // ── Save (Layer 2: @Version on DailyLog fires here if concurrent) ────
        dailyLogRepository.save(dailyLog);

        headcountUseCase.refreshCache(mess);

        log.info("Command '{}' from [{}] → {}",
                command.text(), command.platformUserId(), reply);
        return reply;
    }

    /**
     * Layer 3 recovery: called after all retries are exhausted.
     * Returns a user-friendly message instead of propagating the exception.
     */
    @Recover
    public String recoverProcessCommand(Exception ex, BotCommand command) {
        log.error("All retries exhausted for command '{}' from [{}]: {}",
                command.text(), command.platformUserId(), ex.getMessage());
        return "⚠️ সার্ভার ব্যস্ত ছিল। দয়া করে কয়েক সেকেন্ড পরে আবার চেষ্টা করুন।";
    }

    // ── Command dispatch ──────────────────────────────────────────────────────

    private String applyCommand(DailyLog dl, String cmd, String name) {
        return switch (cmd) {
            case "/on" -> {
                dl.setBreakfast(true);
                dl.setLunch(true);
                dl.setDinner(true);
                yield "✅ " + name + " — সব মিল চালু (B+L+D)";
            }
            case "/off" -> {
                dl.setBreakfast(false);
                dl.setLunch(false);
                dl.setDinner(false);
                yield "❌ " + name + " — আজকের সব মিল বন্ধ";
            }
            case "/breakfast on" -> {
                dl.setBreakfast(true);
                yield "🌅 " + name + " — নাস্তা চালু";
            }
            case "/breakfast off" -> {
                dl.setBreakfast(false);
                yield "🌅 " + name + " — নাস্তা বন্ধ";
            }
            case "/lunch on" -> {
                dl.setLunch(true);
                yield "☀️ " + name + " — দুপুর চালু";
            }
            case "/lunch off" -> {
                dl.setLunch(false);
                yield "☀️ " + name + " — দুপুর বন্ধ";
            }
            case "/dinner on" -> {
                dl.setDinner(true);
                yield "🌙 " + name + " — রাত চালু";
            }
            case "/dinner off" -> {
                dl.setDinner(false);
                yield "🌙 " + name + " — রাত বন্ধ";
            }
            case "/status" -> buildStatus(dl, name);
            case "/help" -> buildHelp();
            default -> {
                if (cmd.startsWith("/guest")) {
                    int n = parseGuestCount(cmd);
                    dl.setGuestCount(dl.getGuestCount() + n);
                    yield "👥 " + name + " — " + n + " জন মেহমান। মোট: " + dl.getGuestCount();
                }
                yield "❓ অজানা কমান্ড। /help টাইপ করুন।";
            }
        };
    }

    private String buildStatus(DailyLog dl, String name) {
        return "📋 <b>" + name + "</b>\n" +
                "🌅 নাস্তা: " + (dl.isBreakfast() ? "✅" : "❌") + "\n" +
                "☀️ দুপুর: " + (dl.isLunch() ? "✅" : "❌") + "\n" +
                "🌙 রাত: " + (dl.isDinner() ? "✅" : "❌") + "\n" +
                "👥 মেহমান: " + dl.getGuestCount() + " জন";
    }

    private String buildHelp() {
        return """
                🍽 <b>Mealio কমান্ড</b>
                /on | /off         — সব মিল চালু/বন্ধ
                /breakfast on|off  — শুধু নাস্তা
                /lunch on|off      — শুধু দুপুর
                /dinner on|off     — শুধু রাত
                /guest [n]         — মেহমান যোগ
                /status            — আজকের অবস্থা
                ⏰ কাটঅফ টাইমের আগে কমান্ড দিতে হবে।""";
    }

    private int parseGuestCount(String cmd) {
        String[] parts = cmd.split("\\s+");
        if (parts.length >= 2) {
            try {
                return Math.max(1, Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }
}
