package com.mealio.controller;

import com.mealio.dto.telegram.TelegramUpdate;
import com.mealio.exception.CutOffTimeExceededException;
import com.mealio.exception.ResourceNotFoundException;
import com.mealio.port.in.BotCommand;
import com.mealio.port.in.MealToggleUseCase;
import com.mealio.port.out.BotReplyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Telegram adapter controller — maps Telegram Update → BotCommand →
 * MealToggleUseCase.
 *
 * This is the ONLY class that knows about Telegram.
 * Swapping to WhatsApp means replacing this file + writing two adapter
 * implementations.
 * MealToggleUseCase, MealToggleService, and all domain code stay untouched.
 *
 * Webhook registration:
 * curl
 * "https://api.telegram.org/bot{TOKEN}/setWebhook?url=https://your-domain/api/telegram/webhook"
 */
@Slf4j
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final MealToggleUseCase mealToggleUseCase; // DIP — interface
    private final BotReplyPort botReplyPort; // DIP — interface

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleUpdate(@RequestBody TelegramUpdate update) {
        if (update.message() == null || update.message().text() == null) {
            return ResponseEntity.ok().build();
        }

        TelegramUpdate.TelegramMessage msg = update.message();
        TelegramUpdate.TelegramUser from = msg.from();
        String text = msg.text().trim();

        if (!text.startsWith("/")) {
            return ResponseEntity.ok().build();
        }

        log.info("Telegram update #{} from user {} (@{}): {}",
                update.updateId(), from.id(), from.username(), text);

        // ── Build platform-agnostic command ───────────────────────────────────
        BotCommand command = new BotCommand(
                String.valueOf(from.id()), // platformUserId: Telegram numeric ID
                text,
                String.valueOf(msg.chat().id()), // chatId: where to reply
                msg.messageId() // replyToId: for threaded reply
        );

        // ── Dispatch to domain (no Telegram imports in domain) ────────────────
        String reply;
        try {
            reply = mealToggleUseCase.processCommand(command);
        } catch (ResourceNotFoundException ex) {
            reply = "⚠️ " + ex.getMessage();
        } catch (CutOffTimeExceededException ex) {
            reply = "⏰ " + ex.getMessage();
        } catch (Exception ex) {
            log.error("Error processing command from user {}: {}", from.id(), ex.getMessage(), ex);
            reply = "❌ একটি সমস্যা হয়েছে। একটু পরে আবার চেষ্টা করুন।";
        }

        // ── Send reply via port (no Telegram SDK in domain) ──────────────────
        botReplyPort.sendReply(command.chatId(), reply, command.replyToId());

        return ResponseEntity.ok().build(); // Always 200
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Mealio Telegram Bot is live ✅");
    }
}
