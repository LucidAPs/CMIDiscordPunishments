package com.lucidaps.cmidiscordpunishments.config;

import com.lucidaps.cmidiscordpunishments.model.PunishmentType;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record PluginSettings(
    URI webhookUri,
    String webhookUsername,
    URI webhookAvatarUri,
    String serverName,
    Duration connectTimeout,
    Duration requestTimeout,
    int maxRetries,
    int queueCapacity,
    String footer,
    Map<PunishmentType, ActionStyle> actionStyles,
    Set<String> muteAliases,
    Set<String> unmuteAliases,
    Set<String> unjailAliases
) {
    public PluginSettings {
        webhookUsername = normalize(webhookUsername, "CMI Punishments");
        serverName = normalize(serverName, "Minecraft Server");
        footer = normalize(footer, "CMI moderation log");
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(10) : requestTimeout;
        actionStyles = Collections.unmodifiableMap(new EnumMap<>(actionStyles));
        muteAliases = immutableAliases(muteAliases);
        unmuteAliases = immutableAliases(unmuteAliases);
        unjailAliases = immutableAliases(unjailAliases);
    }

    public Optional<URI> webhook() {
        return Optional.ofNullable(webhookUri);
    }

    public ActionStyle style(PunishmentType type) {
        return actionStyles.getOrDefault(type, new ActionStyle(true, type.defaultTitle(), type.defaultColor()));
    }

    public static PluginSettings disabledDefaults() {
        Map<PunishmentType, ActionStyle> styles = new EnumMap<>(PunishmentType.class);
        for (PunishmentType type : PunishmentType.values()) {
            styles.put(type, new ActionStyle(true, type.defaultTitle(), type.defaultColor()));
        }
        return new PluginSettings(
            null,
            "CMI Punishments",
            null,
            "Minecraft Server",
            Duration.ofSeconds(5),
            Duration.ofSeconds(10),
            3,
            250,
            "CMI moderation log",
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
