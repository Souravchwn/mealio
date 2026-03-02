package com.mealio.adapter.telegram;

import com.mealio.model.entity.Member;
import com.mealio.port.out.MemberIdentityPort;
import com.mealio.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Telegram implementation of MemberIdentityPort.
 *
 * Translates Telegram's numeric user ID (stored as String in
 * BotCommand.platformUserId)
 * into a registered Member via telegramUserId column.
 *
 * To add WhatsApp: create WhatsAppMemberIdentityAdapter implements
 * MemberIdentityPort,
 * look up by phone, and swap @Primary annotation (or use Spring profiles).
 * MealToggleService doesn't change at all.
 */
@Slf4j
@Component
@Primary // marks this as the active MemberIdentityPort implementation
@RequiredArgsConstructor
public class TelegramMemberIdentityAdapter implements MemberIdentityPort {

    private final MemberRepository memberRepository;

    /**
     * @param platformUserId Telegram numeric user ID as a String
     * @return The matched Member, or empty if not registered
     */
    @Override
    public Optional<Member> resolve(String platformUserId) {
        try {
            Long telegramUserId = Long.parseLong(platformUserId);
            return memberRepository.findByTelegramUserId(telegramUserId);
        } catch (NumberFormatException e) {
            log.warn("TelegramMemberIdentityAdapter received non-numeric platformUserId: {}", platformUserId);
            return Optional.empty();
        }
    }
}
