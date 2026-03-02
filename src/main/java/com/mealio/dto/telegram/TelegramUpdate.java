package com.mealio.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal mapping of the Telegram Bot API "Update" object.
 * Only the fields Mealio needs are mapped; all others are ignored.
 *
 * Telegram docs: https://core.telegram.org/bots/api#update
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdate(

        @JsonProperty("update_id") Long updateId,

        /** Set for regular chat/group messages. */
        @JsonProperty("message") TelegramMessage message) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramMessage(

            @JsonProperty("message_id") Long messageId,

            /** The user who sent the message. */
            @JsonProperty("from") TelegramUser from,

            /** The chat the message was sent in (group or private). */
            @JsonProperty("chat") TelegramChat chat,

            /** Plain text body. */
            @JsonProperty("text") String text,

            /** Unix timestamp. */
            @JsonProperty("date") Long date) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramUser(

            /** Telegram's internal numeric user ID — used as our member identifier. */
            @JsonProperty("id") Long id,

            @JsonProperty("first_name") String firstName,

            @JsonProperty("last_name") String lastName,

            @JsonProperty("username") String username) {
        /** Helper: human-readable display name. */
        public String displayName() {
            if (firstName == null)
                return username != null ? "@" + username : String.valueOf(id);
            return lastName != null ? firstName + " " + lastName : firstName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramChat(
            @JsonProperty("id") Long id,

            /** "private", "group", "supergroup", "channel" */
            @JsonProperty("type") String type,

            @JsonProperty("title") String title) {
    }
}
