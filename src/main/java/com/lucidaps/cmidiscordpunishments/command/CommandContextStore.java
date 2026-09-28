package com.lucidaps.cmidiscordpunishments.command;

import java.time.Duration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CommandContextStore {
    private static final long TIME_TO_LIVE_MILLIS = Duration.ofSeconds(5).toMillis();
    private final Queue<Context> contexts = new ConcurrentLinkedQueue<>();

    public void record(TrackedCommand type, String target, String actor) {
        purgeExpired();
        contexts.add(new Context(type, normalize(target), actor, System.currentTimeMillis()));
    }

    public Optional<String> consume(TrackedCommand type, String playerName, UUID playerUuid) {
        long now = System.currentTimeMillis();
        String normalizedName = normalize(playerName);
        String normalizedUuid = playerUuid == null ? "" : normalize(playerUuid.toString());
        Iterator<Context> iterator = contexts.iterator();
        while (iterator.hasNext()) {
            Context context = iterator.next();
            if (now - context.createdAt > TIME_TO_LIVE_MILLIS) {
                contexts.remove(context);
                continue;
            }
            boolean targetMatches = context.target.equals(normalizedName) || context.target.equals(normalizedUuid);
            if (context.type == type && targetMatches && contexts.remove(context)) {
                return Optional.ofNullable(context.actor);
            }
        }
        return Optional.empty();
    }

    private void purgeExpired() {
        long cutoff = System.currentTimeMillis() - TIME_TO_LIVE_MILLIS;
        contexts.removeIf(context -> context.createdAt < cutoff);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record Context(TrackedCommand type, String target, String actor, long createdAt) {
    }
}
