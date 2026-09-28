package com.lucidaps.cmidiscordpunishments;

import com.lucidaps.cmidiscordpunishments.config.ActionStyle;
import com.lucidaps.cmidiscordpunishments.config.PluginSettings;
import com.lucidaps.cmidiscordpunishments.model.PunishmentType;

import java.net.URI;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class TestSettings {
    private TestSettings() {
    }

    public static PluginSettings create(URI webhook) {
        return create(webhook, 20);
    }

    public static PluginSettings create(URI webhook, int queueCapacity) {
        Map<PunishmentType, ActionStyle> styles = new EnumMap<>(PunishmentType.class);
        for (PunishmentType type : PunishmentType.values()) {
            styles.put(type, new ActionStyle(true, type.defaultTitle(), type.defaultColor()));
        }
        return new PluginSettings(
            webhook,
            "CMI Tests",
            null,
            "Test Server",
            Duration.ofSeconds(2),
            Duration.ofSeconds(2),
            3,
            queueCapacity,
            "Test footer",
            styles,
            Set.of("mute", "cmi:mute", "silenceplayer"),
            Set.of("unmute", "cmi:unmute"),
            Set.of("unjail", "cmi:unjail")
        );
    }
}
