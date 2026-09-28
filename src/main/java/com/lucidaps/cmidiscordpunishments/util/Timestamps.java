package com.lucidaps.cmidiscordpunishments.util;

public final class Timestamps {
    private static final long EPOCH_MILLIS_THRESHOLD = 100_000_000_000L;

    private Timestamps() {
    }

    public static long normalizeEpochMillis(long value) {
        if (value <= 0) {
            return value;
        }
        return value < EPOCH_MILLIS_THRESHOLD ? value * 1_000L : value;
    }

    public static String discordExpiry(Long rawValue) {
        if (rawValue == null || rawValue <= 0) {
            return "Permanent";
        }
        long epochSeconds = normalizeEpochMillis(rawValue) / 1_000L;
        return "<t:" + epochSeconds + ":F> (<t:" + epochSeconds + ":R>)";
    }
}
