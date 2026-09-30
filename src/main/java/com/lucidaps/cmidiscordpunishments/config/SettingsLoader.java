package com.lucidaps.cmidiscordpunishments.config;

import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import com.lucidaps.cmidiscordpunishments.util.MessageTemplates;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SettingsLoader {
    private SettingsLoader() {
    }

    public static PluginSettings load(FileConfiguration config) throws SettingsException {
        DiscordDestination discord = parseDiscordDestination(
            config.getString("discord.bot-token", ""),
            config.getString("discord.channel-id", "")
        );

        int connectTimeout = boundedInt(config, "delivery.connect-timeout-seconds", 5, 1, 60);
        int requestTimeout = boundedInt(config, "delivery.request-timeout-seconds", 10, 1, 120);
        int maxRetries = boundedInt(config, "delivery.max-retries", 3, 0, 10);
        int queueCapacity = boundedInt(config, "delivery.queue-capacity", 250, 10, 5_000);

        Map<PunishmentType, ActionStyle> styles = new EnumMap<>(PunishmentType.class);
        for (PunishmentType type : PunishmentType.values()) {
            String base = "events." + type.configKey();
            boolean enabled = config.getBoolean(base + ".enabled", true);
            String title = config.getString(base + ".title", type.defaultTitle());
            int color = parseColor(config.get(base + ".color"), type.defaultColor(), base + ".color");
            List<String> description = description(
                config,
                base + ".description",
                type.defaultDescription()
            );
            validateTemplate(title, base + ".title");
            for (int index = 0; index < description.size(); index++) {
                validateTemplate(description.get(index), base + ".description[" + index + "]");
            }
            styles.put(type, new ActionStyle(enabled, title, color, description));
        }

        return new PluginSettings(
            discord,
            config.getString("server-name", "Minecraft Server"),
            Duration.ofSeconds(connectTimeout),
            Duration.ofSeconds(requestTimeout),
            maxRetries,
            queueCapacity,
            styles,
            aliases(config.getStringList("commands.aliases.mute"), "mute", "cmi:mute"),
            aliases(config.getStringList("commands.aliases.unmute"), "unmute", "cmi:unmute"),
            aliases(config.getStringList("commands.aliases.unjail"), "unjail", "cmi:unjail")
        );
    }

    private static DiscordDestination parseDiscordDestination(String rawToken, String rawChannelId)
        throws SettingsException {
        String token = trimToNull(rawToken);
        String channelId = trimToNull(rawChannelId);
        if (token == null && channelId == null) {
            return null;
        }
        if (token == null || channelId == null) {
            throw new SettingsException("discord.bot-token and discord.channel-id must either both be set or both be blank");
        }
        if (!channelId.chars().allMatch(Character::isDigit)) {
            throw new SettingsException("discord.channel-id must be a positive decimal Discord channel ID");
        }
        try {
            long parsed = Long.parseUnsignedLong(channelId);
            if (parsed == 0L) {
                throw new NumberFormatException("zero is not a channel ID");
            }
        } catch (NumberFormatException exception) {
            throw new SettingsException("discord.channel-id must be a positive decimal Discord channel ID", exception);
        }
        return new DiscordDestination(token, channelId);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int boundedInt(FileConfiguration config, String path, int fallback, int min, int max)
        throws SettingsException {
        int value = config.getInt(path, fallback);
        if (value < min || value > max) {
            throw new SettingsException(path + " must be between " + min + " and " + max);
        }
        return value;
    }

    private static int parseColor(Object raw, int fallback, String path) throws SettingsException {
        if (raw == null) {
            return fallback;
        }
        try {
            int color;
            if (raw instanceof Number number) {
                color = number.intValue();
            } else {
                String value = String.valueOf(raw).trim();
                color = Integer.parseInt(value.startsWith("#") ? value.substring(1) : value, 16);
            }
            if (color < 0 || color > 0xFFFFFF) {
                throw new NumberFormatException("outside RGB range");
            }
            return color;
        } catch (NumberFormatException exception) {
            throw new SettingsException(path + " must be an RGB hex color such as #E74C3C", exception);
        }
    }

    private static List<String> description(FileConfiguration config, String path, List<String> fallback)
        throws SettingsException {
        if (!config.contains(path)) {
            return fallback;
        }
        if (!config.isList(path)) {
            throw new SettingsException(path + " must be a YAML list of message lines");
        }
        List<?> rawLines = config.getList(path, List.of());
        java.util.ArrayList<String> lines = new java.util.ArrayList<>(rawLines.size());
        for (Object rawLine : rawLines) {
            if (!(rawLine instanceof String line)) {
                throw new SettingsException(path + " must contain only text lines");
            }
            lines.add(line);
        }
        return List.copyOf(lines);
    }

    private static void validateTemplate(String template, String path) throws SettingsException {
        Set<String> unknown = MessageTemplates.unknownPlaceholders(template);
        if (!unknown.isEmpty()) {
            throw new SettingsException(path + " contains unknown placeholder(s): " + String.join(", ", unknown));
        }
    }

    private static Set<String> aliases(List<String> configured, String... defaults) {
        Set<String> aliases = new LinkedHashSet<>();
        for (String value : defaults) {
            aliases.add(normalizeAlias(value));
        }
        for (String value : configured) {
            String alias = normalizeAlias(value);
            if (!alias.isEmpty()) {
                aliases.add(alias);
            }
        }
        return aliases;
    }

    private static String normalizeAlias(String value) {
        String alias = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        while (alias.startsWith("/")) {
            alias = alias.substring(1);
        }
        return alias;
    }
}
