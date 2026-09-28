package com.lucidaps.cmidiscordpunishments.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.lucidaps.cmidiscordpunishments.config.ActionStyle;
import com.lucidaps.cmidiscordpunishments.config.PluginSettings;
import com.lucidaps.cmidiscordpunishments.model.PunishmentReport;
import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import com.lucidaps.cmidiscordpunishments.util.TextSanitizer;
import com.lucidaps.cmidiscordpunishments.util.Timestamps;

import java.util.Map;

public final class DiscordEmbedRenderer {
    private static final int MAX_FIELDS = 25;
    private static final int MAX_EMBED_CHARACTERS = 6_000;
    private final Gson gson = new Gson();

    public String render(PunishmentReport report, PluginSettings settings) {
        ActionStyle style = settings.style(report.type());
        JsonObject payload = new JsonObject();
        payload.addProperty("username", TextSanitizer.clean(settings.webhookUsername(), "CMI Punishments", 80));
        if (settings.webhookAvatarUri() != null) {
            payload.addProperty("avatar_url", settings.webhookAvatarUri().toString());
        }

        JsonObject allowedMentions = new JsonObject();
        allowedMentions.add("parse", new JsonArray());
        payload.add("allowed_mentions", allowedMentions);

        String title = TextSanitizer.clean(style.title(), report.type().defaultTitle(), 256);
        String footerText = TextSanitizer.clean(settings.footer(), "CMI moderation log", 2_048);
        EmbedBudget budget = new EmbedBudget(MAX_EMBED_CHARACTERS - title.length() - footerText.length());

        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("color", style.color());
        embed.addProperty("timestamp", report.occurredAt().toString());

        JsonArray fields = new JsonArray();
        String target = TextSanitizer.clean(report.target(), "Unknown", 900);
        if (report.targetUuid() != null) {
            target += "\nUUID: " + report.targetUuid();
        }
        addField(fields, budget, "Target", target, true);
        addField(
            fields,
            budget,
            "Moderator / Source",
            TextSanitizer.clean(report.actor(), "CMI / Automatic", 1_024),
            true
        );

        if (hasExpiry(report.type())) {
            addField(fields, budget, "Expires", Timestamps.discordExpiry(report.expiresAtEpochMillis()), false);
        }
        if (hasReason(report.type()) || (report.reason() != null && !report.reason().isBlank())) {
            addField(
                fields,
                budget,
                "Reason",
                TextSanitizer.clean(report.reason(), "Not provided", 1_024),
                false
            );
        }
        addField(fields, budget, "Server", TextSanitizer.clean(settings.serverName(), "Minecraft Server", 1_024), true);

        for (Map.Entry<String, String> entry : report.details().entrySet()) {
            if (fields.size() >= MAX_FIELDS || budget.remaining() < 2) {
                break;
            }
            addField(
                fields,
                budget,
                TextSanitizer.clean(entry.getKey(), "Detail", 256),
                TextSanitizer.clean(entry.getValue(), "Unknown", 1_024),
                true
            );
        }
        embed.add("fields", fields);

        JsonObject footer = new JsonObject();
        footer.addProperty("text", footerText);
        embed.add("footer", footer);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);
        return gson.toJson(payload);
    }

    private static void addField(
        JsonArray fields,
        EmbedBudget budget,
        String rawName,
        String rawValue,
        boolean inline
    ) {
        if (fields.size() >= MAX_FIELDS || budget.remaining() < 2) {
            return;
        }
        String name = TextSanitizer.clean(rawName, "Detail", Math.min(256, budget.remaining() - 1));
        int availableForValue = Math.min(1_024, budget.remaining() - name.length());
        if (availableForValue < 1) {
            return;
        }
        String value = TextSanitizer.clean(rawValue, "Unknown", availableForValue);
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value);
        field.addProperty("inline", inline);
        fields.add(field);
        budget.consume(name.length() + value.length());
    }

    private static boolean hasExpiry(PunishmentType type) {
        return switch (type) {
            case BAN, TEMP_BAN, IP_BAN, TEMP_IP_BAN, JAIL, MUTE -> true;
            default -> false;
        };
    }

    private static boolean hasReason(PunishmentType type) {
        return switch (type) {
            case TEST, BAN, TEMP_BAN, IP_BAN, TEMP_IP_BAN, KICK, JAIL, MUTE, WARN -> true;
            default -> false;
        };
    }

    private static final class EmbedBudget {
        private int remaining;

        private EmbedBudget(int remaining) {
            this.remaining = Math.max(0, remaining);
        }

        private int remaining() {
            return remaining;
        }

        private void consume(int characters) {
            remaining = Math.max(0, remaining - characters);
        }
    }
}
