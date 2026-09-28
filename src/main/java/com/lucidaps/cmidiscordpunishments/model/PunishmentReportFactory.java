package com.lucidaps.cmidiscordpunishments.model;

import java.util.UUID;

public final class PunishmentReportFactory {
    private PunishmentReportFactory() {
    }

    public static PunishmentReport playerBan(
        String target,
        UUID targetUuid,
        String actor,
        String reason,
        Long until
    ) {
        return PunishmentReport.builder(isTemporary(until) ? PunishmentType.TEMP_BAN : PunishmentType.BAN)
            .target(target)
            .targetUuid(targetUuid)
            .actor(actor)
            .reason(reason)
            .expiresAt(until)
            .build();
    }

    public static PunishmentReport ipBan(String ip, String actor, String reason, Long until) {
        return PunishmentReport.builder(isTemporary(until) ? PunishmentType.TEMP_IP_BAN : PunishmentType.IP_BAN)
            .target(ip)
            .actor(actor)
            .reason(reason)
            .expiresAt(until)
            .build();
    }

    public static PunishmentReport kick(String target, UUID targetUuid, String actor, String reason) {
        return PunishmentReport.builder(PunishmentType.KICK)
            .target(target)
            .targetUuid(targetUuid)
            .actor(actor)
            .reason(reason)
            .build();
    }

    public static PunishmentReport unban(String target, UUID targetUuid, String actor) {
        return PunishmentReport.builder(PunishmentType.UNBAN)
            .target(target)
            .targetUuid(targetUuid)
            .actor(actor)
            .build();
    }

    public static PunishmentReport ipUnban(String ip, String actor) {
        return PunishmentReport.builder(PunishmentType.IP_UNBAN)
            .target(ip)
            .actor(actor)
            .build();
    }

    public static PunishmentReport warning(
        String target,
        UUID targetUuid,
        String actor,
        String reason,
        String category,
        Integer points
    ) {
        return PunishmentReport.builder(PunishmentType.WARN)
            .target(target)
            .targetUuid(targetUuid)
            .actor(actor)
            .reason(reason)
            .detail("Category", category)
            .detail("Points", points)
            .build();
    }

    public static PunishmentReport jail(
        String target,
        UUID targetUuid,
        String actor,
        String reason,
        Long until,
        String jail,
        Integer cell
    ) {
        return PunishmentReport.builder(PunishmentType.JAIL)
            .target(target)
            .targetUuid(targetUuid)
            .actor(actor)
            .reason(reason)
            .expiresAt(until)
            .detail("Jail", jail)
            .detail("Cell", cell)
            .build();
    }

    public static PunishmentReport unjail(
        String target,
        UUID targetUuid,
        String actor,
        String jail,
        Integer cell
    ) {
        return PunishmentReport.builder(PunishmentType.UNJAIL)
            .target(target)
            .targetUuid(targetUuid)
            .actor(actor)
            .detail("Jail", jail)
            .detail("Cell", cell)
            .build();
    }

    private static boolean isTemporary(Long until) {
        return until != null && until > 0L;
    }
}
