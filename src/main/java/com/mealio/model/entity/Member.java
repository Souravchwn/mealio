package com.mealio.model.entity;

import com.mealio.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A person belonging to a Mess.
 * Phone number is the WhatsApp-linked identifier.
 */
@Entity
@Table(name = "member", uniqueConstraints = @UniqueConstraint(columnNames = "phone"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Optimistic lock — guards balance from concurrent lost updates. */
    @Version
    private Long version;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * E.164 format, e.g. +8801XXXXXXXXX — optional if member uses Telegram only.
     */
    @Column(unique = true, length = 20)
    private String phone;

    /**
     * Telegram's numeric user ID (from Update.message.from.id).
     * Used to identify the member when they send commands in the mess Telegram
     * group.
     * Nullable — populated when member first interacts with the bot.
     */
    @Column(name = "telegram_user_id", unique = true)
    private Long telegramUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Running balance (positive = owed money, negative = owes money). */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mess_id", nullable = false)
    private Mess mess;
}
