package com.lucidaps.cmidiscordpunishments;

import com.lucidaps.cmidiscordpunishments.config.ActionStyle;
import com.lucidaps.cmidiscordpunishments.config.DiscordDestination;
import com.lucidaps.cmidiscordpunishments.config.PluginSettings;
import com.lucidaps.cmidiscordpunishments.model.PunishmentType;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class TestSettings {
    public static final String BOT_TOKEN = "test.bot.token";
    public static final String CHANNEL_ID = "123456789012345678";

    private TestSettings() {
    }

    public static PluginSettings create() {
        return create(20);
    }

    public static PluginSettings create(int queueCapacity) {
        return create(new DiscordDestination(BOT_TOKEN, CHANNEL_ID), queueCapacity);
    }

    public static PluginSettings create(String botToken, String channelId) {
        return create(new DiscordDestination(botToken, channelId), 20);
    }

    public static PluginSettings disabled() {
        return create(null, 20);
    }

    private static PluginSettings create(DiscordDestination destination, int queueCapacity) {
        Map<PunishmentType, ActionStyle> styles = new EnumMap<>(PunishmentType.class);
        for (PunishmentType type : PunishmentType.values()) {
            styles.put(type, new ActionStyle(
                true,
                type.defaultTitle(),
                type.defaultColor(),
                type.defaultDescription()
            ));
        }
        return new PluginSettings(
            destination,
            "Test Server",
            Duration.ofSeconds(2),
            Duration.ofSeconds(2),
            3,
            queueCapacity,
            styles,
            Set.of("mute", "cmi:mute", "silenceplayer"),
            Set.of("unmute", "cmi:unmute"),
            Set.of("unjail", "cmi:unjail")
        );
    }
}
