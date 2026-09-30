package com.lucidaps.cmidiscordpunishments.discord;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lucidaps.cmidiscordpunishments.config.ActionStyle;
import com.lucidaps.cmidiscordpunishments.config.PluginSettings;
import com.lucidaps.cmidiscordpunishments.model.PunishmentReport;
import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import com.lucidaps.cmidiscordpunishments.util.MessageTemplates;
import com.lucidaps.cmidiscordpunishments.util.TextSanitizer;
import com.lucidaps.cmidiscordpunishments.util.Timestamps;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DiscordEmbedRenderer {
    private static final int MAX_TITLE_CHARACTERS = 256;
    private static final int MAX_DESCRIPTION_CHARACTERS = 4_096;
    private static final int MAX_EMBED_CHARACTERS = 6_000;
    private final Gson gson = new Gson();

    public String render(PunishmentReport report, PluginSettings settings) {
        ActionStyle style = settings.style(report.type());
        Map<String, String> values = templateValues(report, settings);

        String renderedTitle = MessageTemplates.renderTitle(style.title(), values);
        String fallbackTitle = MessageTemplates.renderTitle(report.type().defaultTitle(), values);
        String title = TextSanitizer.clean(renderedTitle, fallbackTitle, MAX_TITLE_CHARACTERS);
        String description = renderDescription(style, values, MAX_EMBED_CHARACTERS - title.length());

        JsonObject payload = new JsonObject();
        JsonObject allowedMentions = new JsonObject();
        allowedMentions.add("parse", new JsonArray());
        payload.add("allowed_mentions", allowedMentions);

        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("color", style.color());
        if (!description.isBlank()) {
            embed.addProperty("description", description);
        }

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);
        return gson.toJson(payload);
    }

    private static String renderDescription(ActionStyle style, Map<String, String> values, int budget) {
        StringBuilder description = new StringBuilder();
        for (String template : style.description()) {
            String line = MessageTemplates.renderDescriptionLine(template, values);
            if (line == null || line.isBlank()) {
                continue;
            }
            if (!description.isEmpty()) {
                description.append('\n');
            }
            description.append(line);
        }
        int maxLength = Math.min(MAX_DESCRIPTION_CHARACTERS, Math.max(0, budget));
        return TextSanitizer.clean(description.toString(), "", maxLength);
    }

    private static Map<String, String> templateValues(PunishmentReport report, PluginSettings settings) {
        Map<String, String> values = new LinkedHashMap<>();
        putClean(values, "target", report.target());
        putClean(values, "actor", report.actor());
        putClean(values, "reason", report.reason());
        putClean(values, "server", settings.serverName());
        putClean(values, "category", report.details().get("Category"));
        putClean(values, "points", report.details().get("Points"));
        putClean(values, "jail", report.details().get("Jail"));
        putClean(values, "cell", report.details().get("Cell"));
        putClean(values, "status", report.details().get("Status"));
        if (hasDuration(report.type())) {
            values.put("duration", Timestamps.friendlyDuration(report.occurredAt(), report.expiresAtEpochMillis()));
        }
        return values;
    }

    private static void putClean(Map<String, String> values, String name, String rawValue) {
        String cleaned = TextSanitizer.clean(rawValue, "", MAX_DESCRIPTION_CHARACTERS);
        if (!cleaned.isBlank()) {
            values.put(name, cleaned);
        }
    }

    private static boolean hasDuration(PunishmentType type) {
        return switch (type) {
            case BAN, TEMP_BAN, IP_BAN, TEMP_IP_BAN, JAIL, MUTE -> true;
            default -> false;
        };
    }
}
