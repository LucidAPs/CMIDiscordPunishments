package com.lucidaps.cmidiscordpunishments.util;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageTemplates {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}\\r\\n]+)}");
    private static final Set<String> ALLOWED_PLACEHOLDERS = Set.of(
        "target",
        "actor",
        "reason",
        "duration",
        "server",
        "category",
        "points",
        "jail",
        "cell",
        "status"
    );

    private MessageTemplates() {
    }

    public static Set<String> unknownPlaceholders(String template) {
        Set<String> unknown = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template == null ? "" : template);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!ALLOWED_PLACEHOLDERS.contains(placeholder)) {
                unknown.add(placeholder);
            }
        }
        return unknown;
    }

    public static String renderTitle(String template, Map<String, String> values) {
        return replace(template, values, false);
    }

    public static String renderDescriptionLine(String template, Map<String, String> values) {
        return replace(template, values, true);
    }

    private static String replace(String template, Map<String, String> values, boolean omitWhenMissing) {
        String source = template == null ? "" : template;
        Matcher matcher = PLACEHOLDER.matcher(source);
        StringBuilder rendered = new StringBuilder(source.length());
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null || value.isBlank()) {
                if (omitWhenMissing) {
                    return null;
                }
                value = "Unknown";
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }
}
