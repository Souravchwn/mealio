package com.mealio.port.in;

/**
 * Platform-agnostic inbound command from any messaging bot.
 *
 * When switching platforms (Telegram → WhatsApp → SMS), only the
 * Controller adapter changes. MealToggleService never changes.
 *
 * @param platformUserId Platform-specific user identifier as a String.
 *                       Telegram: Long.toString(from.id())
 *                       WhatsApp: E.164 phone number
 *                       SMS: E.164 phone number
 * @param text           Raw command text, e.g. "/off" or "/guest 3"
 * @param chatId         Where to send the reply (group ID, DM ID, phone, etc.)
 * @param replyToId      Optional: message ID to quote/reply-to (null if
 *                       unsupported)
 */
public record BotCommand(
        String platformUserId,
        String text,
        String chatId,
        Long replyToId) {
    /** Convenience constructor — no reply-to threading. */
    public BotCommand(String platformUserId, String text, String chatId) {
        this(platformUserId, text, chatId, null);
    }
}
