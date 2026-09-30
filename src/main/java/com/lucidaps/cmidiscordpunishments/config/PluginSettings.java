package com.lucidaps.cmidiscordpunishments.config;

import com.lucidaps.cmidiscordpunishments.model.PunishmentType;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record PluginSettings(
    DiscordDestination discordDestination,
    String serverName,
    Duration connectTimeout,
    Duration requestTimeout,
    int maxRetries,
    int queueCapacity,
    Map<PunishmentType, ActionStyle> actionStyles,
    Set<String> muteAliases,
    Set<String> unmuteAliases,
    Set<String> unjailAliases
) {
    public PluginSettings {
        serverName = normalize(serverName, "Minecraft Server");
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(10) : requestTimeout;
        actionStyles = Collections.unmodifiableMap(new EnumMap<>(actionStyles));
        muteAliases = immutableAliases(muteAliases);
        unmuteAliases = immutableAliases(unmuteAliases);
        unjailAliases = immutableAliases(unjailAliases);
    }

    public Optional<DiscordDestination> discord() {
        return Optional.ofNullable(discordDestination);
    }

    public ActionStyle style(PunishmentType type) {
        return actionStyles.getOrDefault(
            type,
            new ActionStyle(true, type.defaultTitle(), type.defaultColor(), type.defaultDescription())
        );
    }

    public static PluginSettings disabledDefaults() {
        Map<PunishmentType, ActionStyle> styles = new EnumMap<>(PunishmentType.class);
        for (PunishmentType type : PunishmentType.values()) {
            styles.put(type, new ActionStyle(true, type.defaultTitle(), type.defaultColor(), type.defaultDescription()));
        }
        return new PluginSettings(
            null,
            "Minecraft Server",
            Duration.ofSeconds(5),
            Duration.ofSeconds(10),
            3,
            250,
            styles,
            Set.of("mute", "cmi:mute"),
            Set.of("unmute", "cmi:unmute"),
            Set.of("unjail", "cmi:unjail")
        );
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static Set<String> immutableAliases(Set<String> aliases) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(aliases == null ? Set.of() : aliases));
    }
}
