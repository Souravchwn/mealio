package com.mealio.port.in;

/**
 * Use-case interface: process a meal toggle command from any bot platform.
 *
 * The service that implements this knows nothing about Telegram, WhatsApp,
 * or any other platform. It only speaks BotCommand and returns a reply string.
 *
 * Switch platforms by: changing the controller (adapter) + MemberIdentityPort
 * impl.
 * This interface and its implementation (MealToggleService) never change.
 */
public interface MealToggleUseCase {
    /**
     * @param command Platform-agnostic command wrapper
     * @return Human-readable reply to send back to the chat
     */
    String processCommand(BotCommand command);
}
