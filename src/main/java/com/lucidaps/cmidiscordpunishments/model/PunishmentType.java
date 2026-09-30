package com.lucidaps.cmidiscordpunishments.model;

import java.util.List;

public enum PunishmentType {
    TEST(
        "test", "✅ DISCORD BOT TEST", 0x3498DB,
        "Status: {status}",
        "Requested by: {actor}"
    ),
    BAN(
        "ban", "🔨 {target} BANNED", 0xE74C3C,
        "Reason: {reason}",
        "Duration: {duration}",
        "Banned by: {actor}"
    ),
    TEMP_BAN(
        "temp-ban", "🔨 {target} TEMP-BANNED", 0xE67E22,
        "Reason: {reason}",
        "Duration: {duration}",
        "Banned by: {actor}"
    ),
    IP_BAN(
        "ip-ban", "⛔ {target} IP-BANNED", 0xC0392B,
        "Reason: {reason}",
        "Duration: {duration}",
        "Banned by: {actor}"
    ),
    TEMP_IP_BAN(
        "temp-ip-ban", "⛔ {target} TEMP IP-BANNED", 0xD35400,
        "Reason: {reason}",
        "Duration: {duration}",
        "Banned by: {actor}"
    ),
    UNBAN(
        "unban", "✅ {target} UNBANNED", 0x2ECC71,
        "Unbanned by: {actor}"
    ),
    IP_UNBAN(
        "ip-unban", "✅ {target} IP-UNBANNED", 0x27AE60,
        "Unbanned by: {actor}"
    ),
    KICK(
        "kick", "👢 {target} KICKED", 0xF1C40F,
        "Reason: {reason}",
        "Kicked by: {actor}"
    ),
    JAIL(
        "jail", "🔒 {target} JAILED", 0xE67E22,
        "Reason: {reason}",
        "Duration: {duration}",
        "Jailed by: {actor}",
        "Jail: {jail}",
        "Cell: {cell}"
    ),
    UNJAIL(
        "unjail", "🔓 {target} RELEASED", 0x2ECC71,
        "Released by: {actor}",
        "Jail: {jail}",
        "Cell: {cell}"
    ),
    MUTE(
        "mute", "🔇 {target} MUTED", 0x9B59B6,
        "Reason: {reason}",
        "Duration: {duration}",
        "Muted by: {actor}"
    ),
    UNMUTE(
        "unmute", "🔊 {target} UNMUTED", 0x2ECC71,
        "Unmuted by: {actor}"
    ),
    WARN(
        "warn", "⚠️ {target} WARNED", 0xF39C12,
        "Reason: {reason}",
        "Warned by: {actor}",
        "Category: {category}",
        "Points: {points}"
    );

    private final String configKey;
    private final String defaultTitle;
    private final int defaultColor;
    private final List<String> defaultDescription;

    PunishmentType(String configKey, String defaultTitle, int defaultColor, String... defaultDescription) {
        this.configKey = configKey;
        this.defaultTitle = defaultTitle;
        this.defaultColor = defaultColor;
        this.defaultDescription = List.of(defaultDescription);
    }

    public String configKey() {
        return configKey;
    }

    public String defaultTitle() {
        return defaultTitle;
    }

    public int defaultColor() {
        return defaultColor;
    }

    public List<String> defaultDescription() {
        return defaultDescription;
    }
}
