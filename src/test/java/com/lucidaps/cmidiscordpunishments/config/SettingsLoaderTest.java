package com.lucidaps.cmidiscordpunishments.config;

import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsLoaderTest {
    @Test
    void acceptsDiscordWebhookAndHexColors() throws Exception {
        YamlConfiguration config = baseConfig();
        config.set("webhook.url", "https://discord.com/api/webhooks/123/token");
        config.set("events.ban.color", "#123ABC");
        config.set("commands.aliases.mute", java.util.List.of("/silenceplayer"));

        PluginSettings settings = SettingsLoader.load(config);
        assertTrue(settings.webhook().isPresent());
        assertEquals(0x123ABC, settings.style(PunishmentType.BAN).color());
        assertTrue(settings.muteAliases().contains("silenceplayer"));
        assertTrue(settings.muteAliases().contains("mute"));
    }

    @Test
    void blankWebhookDisablesDeliveryWithoutInvalidatingConfig() throws Exception {
        PluginSettings settings = SettingsLoader.load(baseConfig());
        assertTrue(settings.webhook().isEmpty());
    }

    @Test
    void rejectsNonDiscordAndOutOfRangeSettings() {
        YamlConfiguration badUrl = baseConfig();
        badUrl.set("webhook.url", "https://example.com/webhooks/123/token");
        assertThrows(SettingsException.class, () -> SettingsLoader.load(badUrl));

        YamlConfiguration badRetries = baseConfig();
        badRetries.set("delivery.max-retries", 99);
        assertThrows(SettingsException.class, () -> SettingsLoader.load(badRetries));
    }

    private static YamlConfiguration baseConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("webhook.url", "");
        config.set("delivery.connect-timeout-seconds", 5);
        config.set("delivery.request-timeout-seconds", 10);
        config.set("delivery.max-retries", 3);
        config.set("delivery.queue-capacity", 250);
        return config;
    }
}
