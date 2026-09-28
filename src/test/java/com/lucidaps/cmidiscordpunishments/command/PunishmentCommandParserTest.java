package com.lucidaps.cmidiscordpunishments.command;

import com.lucidaps.cmidiscordpunishments.TestSettings;
import com.lucidaps.cmidiscordpunishments.config.PluginSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PunishmentCommandParserTest {
    private final PluginSettings settings = TestSettings.create(null);
    private final PunishmentCommandParser parser = new PunishmentCommandParser();

    @Test
    void parsesDirectNamespacedAndBaseCommands() {
        assertParsed("/mute Alice 1h reason", TrackedCommand.MUTE, "Alice");
        assertParsed("cmi:unmute Alice", TrackedCommand.UNMUTE, "Alice");
        assertParsed("/cmi mute Alice 30m", TrackedCommand.MUTE, "Alice");
        assertParsed("cmi:cmi unjail Alice", TrackedCommand.UNJAIL, "Alice");
    }

    @Test
    void parsesConfiguredDirectAlias() {
        assertParsed("/silenceplayer Alice 10m", TrackedCommand.MUTE, "Alice");
    }

    @Test
    void rejectsUnrelatedAndIncompleteCommands() {
        assertTrue(parser.parse("/ban Alice", settings).isEmpty());
        assertTrue(parser.parse("/cmi mute", settings).isEmpty());
        assertTrue(parser.parse("", settings).isEmpty());
    }

    private void assertParsed(String command, TrackedCommand type, String target) {
        ParsedCommand parsed = parser.parse(command, settings).orElseThrow();
        assertEquals(type, parsed.type());
        assertEquals(target, parsed.target());
    }
}
