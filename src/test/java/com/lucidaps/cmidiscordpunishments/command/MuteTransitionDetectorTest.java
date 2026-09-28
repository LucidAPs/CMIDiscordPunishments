package com.lucidaps.cmidiscordpunishments.command;

import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuteTransitionDetectorTest {
    private static final UUID UUID_VALUE = UUID.fromString("0d85571b-891d-4c1f-8fc6-9866945bdc9e");

    @Test
    void recognizesNewAndChangedMutes() {
        MuteSnapshot clear = snapshot(false, 0L, null);
        MuteSnapshot muted = snapshot(true, 2_000_000_000_000L, "Spam");
        assertEquals(PunishmentType.MUTE,
            MuteTransitionDetector.detect(TrackedCommand.MUTE, clear, muted).orElseThrow());

        MuteSnapshot extended = snapshot(true, 2_100_000_000_000L, "Spam");
        assertEquals(PunishmentType.MUTE,
            MuteTransitionDetector.detect(TrackedCommand.MUTE, muted, extended).orElseThrow());
    }

    @Test
    void recognizesUnmuteIncludingMuteSubtraction() {
        MuteSnapshot muted = snapshot(true, 2_000_000_000_000L, "Spam");
        MuteSnapshot clear = snapshot(false, 0L, null);
        assertEquals(PunishmentType.UNMUTE,
            MuteTransitionDetector.detect(TrackedCommand.UNMUTE, muted, clear).orElseThrow());
        assertEquals(PunishmentType.UNMUTE,
            MuteTransitionDetector.detect(TrackedCommand.MUTE, muted, clear).orElseThrow());
    }

    @Test
    void ignoresFailedAndNoOpCommands() {
        MuteSnapshot clear = snapshot(false, 0L, null);
        MuteSnapshot muted = snapshot(true, 2_000_000_000_000L, "Spam");
        assertTrue(MuteTransitionDetector.detect(TrackedCommand.MUTE, clear, clear).isEmpty());
        assertTrue(MuteTransitionDetector.detect(TrackedCommand.UNMUTE, clear, clear).isEmpty());
        assertTrue(MuteTransitionDetector.detect(TrackedCommand.MUTE, muted, muted).isEmpty());
    }

    private static MuteSnapshot snapshot(boolean muted, Long until, String reason) {
        return new MuteSnapshot(UUID_VALUE, "Alice", muted, until, reason);
    }
}
