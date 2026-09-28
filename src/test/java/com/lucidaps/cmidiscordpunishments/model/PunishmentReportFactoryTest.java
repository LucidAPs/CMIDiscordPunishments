package com.lucidaps.cmidiscordpunishments.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PunishmentReportFactoryTest {
    private static final UUID UUID_VALUE = UUID.fromString("0d85571b-891d-4c1f-8fc6-9866945bdc9e");

    @Test
    void mapsPermanentAndTemporaryBans() {
        PunishmentReport permanent = PunishmentReportFactory.playerBan(
            "Alice", UUID_VALUE, "Moderator", "Cheating", null
        );
        PunishmentReport temporary = PunishmentReportFactory.playerBan(
            "Alice", UUID_VALUE, "Moderator", "Cheating", 2_000_000_000_000L
        );
        PunishmentReport ipPermanent = PunishmentReportFactory.ipBan(
            "203.0.113.42", "Moderator", "Alt abuse", 0L
        );
        PunishmentReport ipTemporary = PunishmentReportFactory.ipBan(
            "203.0.113.42", "Moderator", "Alt abuse", 2_000_000_000_000L
        );

        assertEquals(PunishmentType.BAN, permanent.type());
        assertEquals(PunishmentType.TEMP_BAN, temporary.type());
        assertEquals(PunishmentType.IP_BAN, ipPermanent.type());
        assertEquals(PunishmentType.TEMP_IP_BAN, ipTemporary.type());
        assertEquals("203.0.113.42", ipTemporary.target());
    }

    @Test
    void mapsKickJailWarningAndReversals() {
        PunishmentReport kick = PunishmentReportFactory.kick("Alice", UUID_VALUE, "Mod", "Spam");
        PunishmentReport jail = PunishmentReportFactory.jail(
            "Alice", UUID_VALUE, "Mod", "Griefing", 2_000_000_000_000L, "spawn", 3
        );
        PunishmentReport warning = PunishmentReportFactory.warning(
            "Alice", UUID_VALUE, "Mod", "Language", "Swear", 2
        );
        PunishmentReport unban = PunishmentReportFactory.unban("Alice", UUID_VALUE, "Mod");
        PunishmentReport ipUnban = PunishmentReportFactory.ipUnban("203.0.113.42", "Mod");
        PunishmentReport unjail = PunishmentReportFactory.unjail("Alice", UUID_VALUE, "Mod", "spawn", 3);

        assertEquals(PunishmentType.KICK, kick.type());
        assertEquals("Spam", kick.reason());
        assertEquals(PunishmentType.JAIL, jail.type());
        assertEquals("spawn", jail.details().get("Jail"));
        assertEquals("3", jail.details().get("Cell"));
        assertEquals(PunishmentType.WARN, warning.type());
        assertEquals("Swear", warning.details().get("Category"));
        assertEquals("2", warning.details().get("Points"));
        assertEquals(PunishmentType.UNBAN, unban.type());
        assertNull(unban.reason());
        assertEquals(PunishmentType.IP_UNBAN, ipUnban.type());
        assertEquals(PunishmentType.UNJAIL, unjail.type());
    }
}
