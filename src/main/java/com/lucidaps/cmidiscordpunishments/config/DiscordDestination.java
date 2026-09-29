package com.lucidaps.cmidiscordpunishments.config;

import java.util.Objects;

public record DiscordDestination(String botToken, String channelId) {
    public DiscordDestination {
        botToken = Objects.requireNonNull(botToken, "botToken");
        channelId = Objects.requireNonNull(channelId, "channelId");
    }

    @Override
    public String toString() {
        return "DiscordDestination[botToken=<redacted>, channelId=" + channelId + "]";
    }
}
