package com.lucidaps.cmidiscordpunishments.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lucidaps.cmidiscordpunishments.TestSettings;
import com.lucidaps.cmidiscordpunishments.model.PunishmentReport;
import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordEmbedRendererTest {
    @Test
    void createsSafeDiscordEmbed() {
        UUID uuid = UUID.fromString("0d85571b-891d-4c1f-8fc6-9866945bdc9e");
        PunishmentReport report = PunishmentReport.builder(PunishmentType.WARN)
            .target("&cAlice")
            .targetUuid(uuid)
            .actor("§4Moderator")
            .reason("@everyone &6Stop spamming")
            .occurredAt(Instant.parse("2026-09-28T10:15:30Z"))
            .detail("Category", "Spam")
            .detail("Points", 2)
            .build();

        JsonObject payload = JsonParser.parseString(
            new DiscordEmbedRenderer().render(report, TestSettings.create())
        ).getAsJsonObject();
        assertEquals(0, payload.getAsJsonObject("allowed_mentions").getAsJsonArray("parse").size());
        assertFalse(payload.has("username"));
        assertFalse(payload.has("avatar_url"));

        JsonObject embed = payload.getAsJsonArray("embeds").get(0).getAsJsonObject();
        assertEquals("Player Warned", embed.get("title").getAsString());
        assertEquals("2026-09-28T10:15:30Z", embed.get("timestamp").getAsString());
        JsonArray fields = embed.getAsJsonArray("fields");
        String allFields = fields.toString();
        assertFalse(allFields.contains("&c"));
        assertFalse(allFields.contains("§4"));
        assertTrue(allFields.contains("@everyone"));
        assertTrue(allFields.contains(uuid.toString()));
        assertTrue(allFields.contains("Test Server"));
    }

    @Test
    void truncatesOversizedUserText() {
        PunishmentReport.Builder builder = PunishmentReport.builder(PunishmentType.KICK)
            .target("A".repeat(2_000))
            .actor("B".repeat(2_000))
            .reason("C".repeat(3_000));
        for (int index = 0; index < 30; index++) {
            builder.detail("Detail " + index, "D".repeat(2_000));
        }
        PunishmentReport report = builder.build();
        JsonObject embed = JsonParser.parseString(
            new DiscordEmbedRenderer().render(report, TestSettings.create())
        ).getAsJsonObject().getAsJsonArray("embeds").get(0).getAsJsonObject();
        int totalCharacters = embed.get("title").getAsString().length()
            + embed.getAsJsonObject("footer").get("text").getAsString().length();
        for (var field : embed.getAsJsonArray("fields")) {
            assertTrue(field.getAsJsonObject().get("value").getAsString().length() <= 1_024);
            totalCharacters += field.getAsJsonObject().get("name").getAsString().length();
            totalCharacters += field.getAsJsonObject().get("value").getAsString().length();
        }
        assertTrue(embed.getAsJsonArray("fields").size() <= 25);
        assertTrue(totalCharacters <= 6_000);
    }
}
