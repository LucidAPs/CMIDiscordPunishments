package com.lucidaps.cmidiscordpunishments.model;

public enum PunishmentType {
    TEST("test", "Webhook Test", 0x3498DB),
    BAN("ban", "Player Banned", 0xE74C3C),
    TEMP_BAN("temp-ban", "Player Temporarily Banned", 0xE67E22),
    IP_BAN("ip-ban", "IP Address Banned", 0xC0392B),
    TEMP_IP_BAN("temp-ip-ban", "IP Address Temporarily Banned", 0xD35400),
    UNBAN("unban", "Player Unbanned", 0x2ECC71),
    IP_UNBAN("ip-unban", "IP Address Unbanned", 0x27AE60),
    KICK("kick", "Player Kicked", 0xF1C40F),
    JAIL("jail", "Player Jailed", 0xE67E22),
    UNJAIL("unjail", "Player Released From Jail", 0x2ECC71),
    MUTE("mute", "Player Muted", 0x9B59B6),
    UNMUTE("unmute", "Player Unmuted", 0x2ECC71),
    WARN("warn", "Player Warned", 0xF39C12);

    private final String configKey;
    private final String defaultTitle;
    private final int defaultColor;

    PunishmentType(String configKey, String defaultTitle, int defaultColor) {
        this.configKey = configKey;
        this.defaultTitle = defaultTitle;
        this.defaultColor = defaultColor;
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
}
