package com.lucidaps.cmidiscordpunishments.config;

import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsLoaderTest {
    @Test
    void acceptsDiscordBotDestinationAndHexColors() throws Exception {
        YamlConfiguration config = baseConfig();
        config.set("discord.bot-token", "secret-token");
        config.set("discord.channel-id", "123456789012345678");
        config.set("events.ban.color", "#123ABC");
        config.set("commands.aliases.mute", java.util.List.of("/silenceplayer"));

        PluginSettings settings = SettingsLoader.load(config);
        assertTrue(settings.discord().isPresent());
        assertEquals("secret-token", settings.discord().orElseThrow().botToken());
        assertEquals("123456789012345678", settings.discord().orElseThrow().channelId());
        assertFalse(settings.discord().orElseThrow().toString().contains("secret-token"));
        assertEquals(0x123ABC, settings.style(PunishmentType.BAN).color());
        assertTrue(settings.muteAliases().contains("silenceplayer"));
        assertTrue(settings.muteAliases().contains("mute"));
    }

    @Test
    void blankDiscordDestinationDisablesDeliveryWithoutInvalidatingConfig() throws Exception {
        PluginSettings settings = SettingsLoader.load(baseConfig());
        assertTrue(settings.discord().isEmpty());
    }

    @Test
    void rejectsPartialOrInvalidDiscordDestination() {
        YamlConfiguration tokenOnly = baseConfig();
        tokenOnly.set("discord.bot-token", "secret-token");
        assertThrows(SettingsException.class, () -> SettingsLoader.load(tokenOnly));

        YamlConfiguration invalidChannel = baseConfig();
        invalidChannel.set("discord.bot-token", "secret-token");
        invalidChannel.set("discord.channel-id", "not-a-channel");
        assertThrows(SettingsException.class, () -> SettingsLoader.load(invalidChannel));

        YamlConfiguration zeroChannel = baseConfig();
        zeroChannel.set("discord.bot-token", "secret-token");
        zeroChannel.set("discord.channel-id", "0");
        assertThrows(SettingsException.class, () -> SettingsLoader.load(zeroChannel));

        YamlConfiguration overflowChannel = baseConfig();
        overflowChannel.set("discord.bot-token", "secret-token");
        overflowChannel.set("discord.channel-id", "18446744073709551616");
        assertThrows(SettingsException.class, () -> SettingsLoader.load(overflowChannel));
    }

    @Test
    void rejectsOutOfRangeDeliverySettings() {
        YamlConfiguration badRetries = baseConfig();
        badRetries.set("delivery.max-retries", 99);
        assertThrows(SettingsException.class, () -> SettingsLoader.load(badRetries));
    }

    private static YamlConfiguration baseConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("discord.bot-token", "");
        config.set("discord.channel-id", "");
        config.set("delivery.connect-timeout-seconds", 5);
        config.set("delivery.request-timeout-seconds", 10);
        config.set("delivery.max-retries", 3);
        config.set("delivery.queue-capacity", 250);
        return config;
    }
}
