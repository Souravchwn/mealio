package com.mealio.adapter.telegram;

import com.mealio.port.out.BotReplyPort;
import com.mealio.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Telegram implementation of BotReplyPort.
 *
 * Wraps TelegramBotService (the raw HTTP client) and adapts it to the
 * platform-agnostic BotReplyPort contract.
 *
 * To add WhatsApp: create WhatsAppBotReplyAdapter implements BotReplyPort,
 * call Meta's sendMessage API, swap @Primary. Zero changes to domain code.
 */
@Component
@Primary // active BotReplyPort implementation
@RequiredArgsConstructor
public class TelegramBotReplyAdapter implements BotReplyPort {

    private final TelegramBotService telegramBotService;

    @Override
    public void sendReply(String chatId, String text, Long replyToId) {
        telegramBotService.sendMessage(Long.parseLong(chatId), text, replyToId);
    }
}
