package com.mealio.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Sends replies back to Telegram via the Bot API's sendMessage endpoint.
 *
 * Uses RestTemplate (no third-party Telegram SDK needed).
 * Base URL: https://api.telegram.org/bot{token}/sendMessage
 */
@Slf4j
@Service
public class TelegramBotService {

    private final RestTemplate restTemplate;
    private final String botApiUrl;

    public TelegramBotService(
            @Value("${mealio.telegram.bot-token}") String botToken) {
        this.restTemplate = new RestTemplate();
        this.botApiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
    }

    /**
     * Send a plain-text reply to a specific chat.
     *
     * @param chatId       Telegram chat ID (group or private)
     * @param text         Message text (supports emoji ✅)
     * @param replyToMsgId Optional message ID to reply to (pass null to skip)
     */
    public void sendMessage(Long chatId, String text, Long replyToMsgId) {
        try {
            var body = new java.util.HashMap<String, Object>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "HTML"); // allows <b>, <i> tags if needed
            if (replyToMsgId != null) {
                body.put("reply_to_message_id", replyToMsgId);
            }

            ResponseEntity<SendMessageResponse> response = restTemplate.postForEntity(botApiUrl, body,
                    SendMessageResponse.class);

            if (response.getBody() != null && !response.getBody().ok()) {
                log.warn("Telegram sendMessage returned ok=false: {}", response.getBody().description());
            }
        } catch (Exception e) {
            // Non-fatal — log and continue; Telegram will retry the webhook if we 500, so
            // we must 200
            log.error("Failed to send Telegram message to chat {}: {}", chatId, e.getMessage());
        }
    }

    /** Shorthand — no reply-to. */
    public void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, null);
    }

    // ── Telegram API response wrapper ─────────────────────────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SendMessageResponse(
            @JsonProperty("ok") boolean ok,
            @JsonProperty("description") String description) {
    }
}
