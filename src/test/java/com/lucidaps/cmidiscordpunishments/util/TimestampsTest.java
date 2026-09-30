package com.lucidaps.cmidiscordpunishments.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimestampsTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-09-28T10:15:30Z");

    @Test
    void formatsPermanentDurations() {
        assertEquals("Permanent", Timestamps.friendlyDuration(OCCURRED_AT, null));
        assertEquals("Permanent", Timestamps.friendlyDuration(OCCURRED_AT, 0L));
    }

    @Test
    void acceptsEpochSecondsAndMilliseconds() {
        long threeDaysInSeconds = OCCURRED_AT.plus(Duration.ofDays(3)).getEpochSecond();
        long twoAndAHalfHoursInMillis = OCCURRED_AT
            .plus(Duration.ofHours(2))
            .plus(Duration.ofMinutes(30))
            .toEpochMilli();

        assertEquals("3 days", Timestamps.friendlyDuration(OCCURRED_AT, threeDaysInSeconds));
        assertEquals("2 hours 30 minutes", Timestamps.friendlyDuration(OCCURRED_AT, twoAndAHalfHoursInMillis));
    }

    @Test
    void usesAtMostTwoNonZeroUnitsWithCorrectPluralization() {
        long expiry = OCCURRED_AT
            .plus(Duration.ofDays(1))
            .plus(Duration.ofHours(1))
            .plus(Duration.ofMinutes(20))
            .toEpochMilli();

        assertEquals("1 day 1 hour", Timestamps.friendlyDuration(OCCURRED_AT, expiry));
        assertEquals(
            "1 second",
            Timestamps.friendlyDuration(OCCURRED_AT, OCCURRED_AT.toEpochMilli() + 1L)
        );
    }

    @Test
    void labelsPastExpiryAsExpired() {
        assertEquals(
            "Expired",
            Timestamps.friendlyDuration(OCCURRED_AT, OCCURRED_AT.minusSeconds(1).toEpochMilli())
        );
    }
}
