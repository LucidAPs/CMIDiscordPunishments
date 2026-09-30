package com.lucidaps.cmidiscordpunishments.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lucidaps.cmidiscordpunishments.TestSettings;
import com.lucidaps.cmidiscordpunishments.model.PunishmentReport;
import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordEmbedRendererTest {
    @Test
    void createsCompactSafeDiscordEmbed() {
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

        JsonObject payload = render(report);
        assertEquals(0, payload.getAsJsonObject("allowed_mentions").getAsJsonArray("parse").size());
        assertFalse(payload.has("username"));
        assertFalse(payload.has("avatar_url"));

        JsonObject embed = payload.getAsJsonArray("embeds").get(0).getAsJsonObject();
        assertEquals("⚠️ Alice WARNED", embed.get("title").getAsString());
        assertEquals(
            "Reason: @everyone Stop spamming\nWarned by: Moderator\nCategory: Spam\nPoints: 2",
            embed.get("description").getAsString()
        );
        assertFalse(embed.has("timestamp"));
        assertFalse(embed.has("fields"));
        assertFalse(embed.has("footer"));
        assertFalse(payload.toString().contains(uuid.toString()));
        assertFalse(payload.toString().contains("Test Server"));
    }

    @Test
    void rendersFriendlyDurationWithoutAbsoluteExpiry() {
        Instant occurredAt = Instant.parse("2026-09-28T10:15:30Z");
        long expiry = occurredAt.plus(Duration.ofHours(2)).plus(Duration.ofMinutes(30)).toEpochMilli();
        PunishmentReport report = PunishmentReport.builder(PunishmentType.TEMP_BAN)
            .target("Alice")
            .actor("Moderator")
            .reason("Cheating")
            .expiresAt(expiry)
            .occurredAt(occurredAt)
            .build();

        JsonObject embed = render(report).getAsJsonArray("embeds").get(0).getAsJsonObject();

        assertEquals("🔨 Alice TEMP-BANNED", embed.get("title").getAsString());
        assertEquals(
            "Reason: Cheating\nDuration: 2 hours 30 minutes\nBanned by: Moderator",
            embed.get("description").getAsString()
        );
        assertFalse(embed.toString().contains(Long.toString(expiry)));
        assertFalse(embed.toString().contains("<t:"));
    }

    @Test
    void omitsDescriptionLinesWhoseValuesAreMissing() {
        PunishmentReport report = PunishmentReport.builder(PunishmentType.WARN)
            .target("Alice")
            .actor("Moderator")
            .build();

        JsonObject embed = render(report).getAsJsonArray("embeds").get(0).getAsJsonObject();

        assertEquals("Warned by: Moderator", embed.get("description").getAsString());
    }

    @Test
    void rendersPermanentReversalJailAndTestDefaults() {
        PunishmentReport permanentBan = PunishmentReport.builder(PunishmentType.BAN)
            .target("Alice")
            .actor("Moderator")
            .reason("Cheating")
            .build();
        PunishmentReport unmute = PunishmentReport.builder(PunishmentType.UNMUTE)
            .target("Alice")
            .actor("Moderator")
            .build();
        PunishmentReport jail = PunishmentReport.builder(PunishmentType.JAIL)
            .target("Alice")
            .actor("Moderator")
            .expiresAt(Instant.parse("2026-09-29T10:15:30Z").toEpochMilli())
            .occurredAt(Instant.parse("2026-09-28T10:15:30Z"))
            .detail("Jail", "spawn")
            .detail("Cell", 3)
            .build();
        PunishmentReport test = PunishmentReport.builder(PunishmentType.TEST)
            .actor("Admin")
            .detail("Status", "Configuration loaded successfully")
            .build();

        assertEquals(
            "Reason: Cheating\nDuration: Permanent\nBanned by: Moderator",
            embed(permanentBan).get("description").getAsString()
        );
        assertEquals("Unmuted by: Moderator", embed(unmute).get("description").getAsString());
        assertEquals(
            "Duration: 1 day\nJailed by: Moderator\nJail: spawn\nCell: 3",
            embed(jail).get("description").getAsString()
        );
        assertEquals("✅ DISCORD BOT TEST", embed(test).get("title").getAsString());
        assertEquals(
            "Status: Configuration loaded successfully\nRequested by: Admin",
            embed(test).get("description").getAsString()
        );
    }

    @Test
    void truncatesOversizedUserText() {
        PunishmentReport report = PunishmentReport.builder(PunishmentType.KICK)
            .target("A".repeat(2_000))
            .actor("B".repeat(2_000))
            .reason("C".repeat(5_000))
            .build();

        JsonObject embed = render(report).getAsJsonArray("embeds").get(0).getAsJsonObject();
        String title = embed.get("title").getAsString();
        String description = embed.get("description").getAsString();

        assertTrue(title.length() <= 256);
        assertTrue(description.length() <= 4_096);
        assertTrue(title.length() + description.length() <= 6_000);
    }

    private static JsonObject render(PunishmentReport report) {
        return JsonParser.parseString(
            new DiscordEmbedRenderer().render(report, TestSettings.create())
        ).getAsJsonObject();
    }

    private static JsonObject embed(PunishmentReport report) {
        return render(report).getAsJsonArray("embeds").get(0).getAsJsonObject();
    }
}
