package com.lucidaps.cmidiscordpunishments.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Timestamps {
    private static final long EPOCH_MILLIS_THRESHOLD = 100_000_000_000L;
    private static final long SECOND_MILLIS = 1_000L;
    private static final long MINUTE_SECONDS = 60L;
    private static final long HOUR_SECONDS = 60L * MINUTE_SECONDS;
    private static final long DAY_SECONDS = 24L * HOUR_SECONDS;

    private Timestamps() {
    }

    public static long normalizeEpochMillis(long value) {
        if (value <= 0) {
            return value;
        }
        return value < EPOCH_MILLIS_THRESHOLD ? value * 1_000L : value;
    }

    public static String friendlyDuration(Instant occurredAt, Long rawValue) {
        if (rawValue == null || rawValue <= 0) {
            return "Permanent";
        }
        Instant effectiveOccurrence = occurredAt == null ? Instant.now() : occurredAt;
        long remainingMillis = normalizeEpochMillis(rawValue) - effectiveOccurrence.toEpochMilli();
        if (remainingMillis <= 0) {
            return "Expired";
        }

        long totalSeconds = Math.floorDiv(remainingMillis - 1L, SECOND_MILLIS) + 1L;
        List<String> parts = new ArrayList<>(2);
        totalSeconds = appendPart(parts, totalSeconds, DAY_SECONDS, "day");
        totalSeconds = appendPart(parts, totalSeconds, HOUR_SECONDS, "hour");
        totalSeconds = appendPart(parts, totalSeconds, MINUTE_SECONDS, "minute");
        appendPart(parts, totalSeconds, 1L, "second");
        return String.join(" ", parts);
    }

    private static long appendPart(List<String> parts, long remaining, long unitSeconds, String unitName) {
        long amount = remaining / unitSeconds;
        if (amount > 0 && parts.size() < 2) {
            parts.add(amount + " " + unitName + (amount == 1 ? "" : "s"));
        }
        return remaining % unitSeconds;
    }
}
