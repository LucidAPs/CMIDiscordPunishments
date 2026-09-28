package com.lucidaps.cmidiscordpunishments.config;

import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
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
        URI webhook = parseWebhook(config.getString("webhook.url", ""));
        URI avatar = parseOptionalHttpsUri(config.getString("webhook.avatar-url", ""), "webhook.avatar-url");

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
            styles.put(type, new ActionStyle(enabled, title, color));
        }

        return new PluginSettings(
            webhook,
            config.getString("webhook.username", "CMI Punishments"),
            avatar,
            config.getString("server-name", "Minecraft Server"),
            Duration.ofSeconds(connectTimeout),
            Duration.ofSeconds(requestTimeout),
            maxRetries,
            queueCapacity,
            config.getString("embeds.footer", "CMI moderation log"),
            styles,
            aliases(config.getStringList("commands.aliases.mute"), "mute", "cmi:mute"),
            aliases(config.getStringList("commands.aliases.unmute"), "unmute", "cmi:unmute"),
            aliases(config.getStringList("commands.aliases.unjail"), "unjail", "cmi:unjail")
        );
    }

    private static URI parseWebhook(String raw) throws SettingsException {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        URI uri = parseUri(raw, "webhook.url");
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean discordHost = host.equals("discord.com")
            || host.endsWith(".discord.com")
            || host.equals("discordapp.com")
            || host.endsWith(".discordapp.com");
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !discordHost || !uri.getPath().contains("/webhooks/")) {
            throw new SettingsException("webhook.url must be an HTTPS Discord webhook URL");
        }
        return uri;
    }

    private static URI parseOptionalHttpsUri(String raw, String path) throws SettingsException {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        URI uri = parseUri(raw, path);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new SettingsException(path + " must use HTTPS");
        }
        return uri;
    }

    private static URI parseUri(String raw, String path) throws SettingsException {
        try {
            URI uri = new URI(raw.trim());
            if (uri.getHost() == null || uri.getUserInfo() != null) {
                throw new SettingsException(path + " is not a valid absolute URL");
            }
            return uri;
        } catch (URISyntaxException exception) {
            throw new SettingsException(path + " is not a valid URL", exception);
        }
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
