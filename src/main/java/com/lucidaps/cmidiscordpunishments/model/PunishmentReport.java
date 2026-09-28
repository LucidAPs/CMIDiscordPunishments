package com.lucidaps.cmidiscordpunishments.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PunishmentReport(
    PunishmentType type,
    String target,
    UUID targetUuid,
    String actor,
    String reason,
    Long expiresAtEpochMillis,
    Instant occurredAt,
    Map<String, String> details
) {
    public PunishmentReport {
        Objects.requireNonNull(type, "type");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        details = Collections.unmodifiableMap(new LinkedHashMap<>(details == null ? Map.of() : details));
    }

    public static Builder builder(PunishmentType type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final PunishmentType type;
        private String target;
        private UUID targetUuid;
        private String actor;
        private String reason;
        private Long expiresAtEpochMillis;
        private Instant occurredAt = Instant.now();
        private final Map<String, String> details = new LinkedHashMap<>();

        private Builder(PunishmentType type) {
            this.type = Objects.requireNonNull(type, "type");
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder targetUuid(UUID targetUuid) {
            this.targetUuid = targetUuid;
            return this;
        }

        public Builder actor(String actor) {
            this.actor = actor;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder expiresAt(Long expiresAtEpochMillis) {
            this.expiresAtEpochMillis = expiresAtEpochMillis;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder detail(String name, Object value) {
            if (name != null && value != null) {
                details.put(name, String.valueOf(value));
            }
            return this;
        }

        public PunishmentReport build() {
            return new PunishmentReport(
                type,
                target,
                targetUuid,
                actor,
                reason,
                expiresAtEpochMillis,
                occurredAt,
                details
            );
        }
    }
}
