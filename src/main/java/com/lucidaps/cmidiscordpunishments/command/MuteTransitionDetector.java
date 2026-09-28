package com.lucidaps.cmidiscordpunishments.command;

import com.lucidaps.cmidiscordpunishments.model.PunishmentType;

import java.util.Objects;
import java.util.Optional;

public final class MuteTransitionDetector {
    private MuteTransitionDetector() {
    }

    public static Optional<PunishmentType> detect(
        TrackedCommand command,
        MuteSnapshot before,
        MuteSnapshot after
    ) {
        if (before == null || after == null) {
            return Optional.empty();
        }
        if (before.muted() && !after.muted()) {
            return Optional.of(PunishmentType.UNMUTE);
        }
        if (command == TrackedCommand.UNMUTE || !after.muted()) {
            return Optional.empty();
        }
        boolean changed = !before.muted()
            || !Objects.equals(before.mutedUntil(), after.mutedUntil())
            || !Objects.equals(normalize(before.reason()), normalize(after.reason()));
        return changed ? Optional.of(PunishmentType.MUTE) : Optional.empty();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
