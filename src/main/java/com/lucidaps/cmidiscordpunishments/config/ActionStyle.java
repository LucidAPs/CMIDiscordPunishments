package com.lucidaps.cmidiscordpunishments.config;

import java.util.Objects;

public record ActionStyle(boolean enabled, String title, int color) {
    public ActionStyle {
        title = Objects.requireNonNullElse(title, "Punishment").trim();
        if (title.isEmpty()) {
            title = "Punishment";
        }
        if (color < 0 || color > 0xFFFFFF) {
            throw new IllegalArgumentException("Discord embed colors must be between #000000 and #FFFFFF");
        }
    }
}
