package com.lucidaps.cmidiscordpunishments.util;

import java.util.regex.Pattern;

public final class TextSanitizer {
    private static final Pattern COLOR_CODES = Pattern.compile(
        "(?i)§x(?:§[0-9A-F]){6}|&x(?:&[0-9A-F]){6}|[&§]#[0-9A-F]{6}|\\{#[0-9A-F]{6}}|[&§][0-9A-FK-ORX]"
    );

    private TextSanitizer() {
    }

    public static String clean(String value, String fallback, int maxLength) {
        String cleaned = value == null ? "" : COLOR_CODES.matcher(value).replaceAll("");
        cleaned = cleanControls(cleaned).trim();
        if (cleaned.isBlank()) {
            cleaned = fallback;
        }
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        if (maxLength <= 1) {
            return "…".substring(0, maxLength);
        }
        return cleaned.substring(0, maxLength - 1) + "…";
    }

    private static String cleanControls(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\r') {
                continue;
            }
            if (character == '\n' || character == '\t' || !Character.isISOControl(character)) {
                builder.append(character);
            }
        }
        return builder.toString();
    }
}
