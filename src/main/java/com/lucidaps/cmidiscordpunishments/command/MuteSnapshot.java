package com.lucidaps.cmidiscordpunishments.command;

import java.util.UUID;

public record MuteSnapshot(UUID uuid, String name, boolean muted, Long mutedUntil, String reason) {
}
