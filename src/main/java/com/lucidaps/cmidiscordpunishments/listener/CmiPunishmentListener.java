package com.lucidaps.cmidiscordpunishments.listener;

import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.Modules.Jail.CMIJailCell;
import com.Zrips.CMI.Modules.Warnings.CMIPlayerWarning;
import com.Zrips.CMI.Modules.Warnings.CMIWarningCategory;
import com.Zrips.CMI.events.CMIIpBanEvent;
import com.Zrips.CMI.events.CMIIpUnBanEvent;
import com.Zrips.CMI.events.CMIPlayerBanEvent;
import com.Zrips.CMI.events.CMIPlayerJailEvent;
import com.Zrips.CMI.events.CMIPlayerKickEvent;
import com.Zrips.CMI.events.CMIPlayerUnBanEvent;
import com.Zrips.CMI.events.CMIPlayerUnjailEvent;
import com.Zrips.CMI.events.CMIPlayerWarnEvent;
import com.lucidaps.cmidiscordpunishments.CMIDiscordPunishments;
import com.lucidaps.cmidiscordpunishments.command.CommandContextStore;
import com.lucidaps.cmidiscordpunishments.command.TrackedCommand;
import com.lucidaps.cmidiscordpunishments.discord.WebhookDispatcher;
import com.lucidaps.cmidiscordpunishments.model.PunishmentReportFactory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.logging.Level;

public final class CmiPunishmentListener implements Listener {
    private static final String AUTOMATIC_SOURCE = "CMI / Automatic";

    private final CMIDiscordPunishments plugin;
    private final WebhookDispatcher dispatcher;
    private final CommandContextStore contextStore;

    public CmiPunishmentListener(
        CMIDiscordPunishments plugin,
        WebhookDispatcher dispatcher,
        CommandContextStore contextStore
    ) {
        this.plugin = plugin;
        this.dispatcher = dispatcher;
        this.contextStore = contextStore;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBan(CMIPlayerBanEvent event) {
        UUID targetUuid = event.getBanned();
        dispatcher.submit(PunishmentReportFactory.playerBan(
            playerName(targetUuid),
            targetUuid,
            senderName(event.getBannedBy()),
            event.getReason(),
            event.getUntil()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIpBan(CMIIpBanEvent event) {
        dispatcher.submit(PunishmentReportFactory.ipBan(
            event.getIp(),
            senderName(event.getBannedBy()),
            event.getReason(),
            event.getUntil()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(CMIPlayerKickEvent event) {
        UUID targetUuid = event.getBanned();
        dispatcher.submit(PunishmentReportFactory.kick(
            playerName(targetUuid),
            targetUuid,
            senderName(event.getBannedBy()),
            event.getReason()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJail(CMIPlayerJailEvent event) {
        CMIUser eventUser = event.getUser();
        CMIJailCell cell = event.getCell();
        if (eventUser == null) {
            return;
        }
        UUID uuid = eventUser.getUniqueId();
        plugin.getServer().getScheduler().runTask(plugin, () -> sendJailReport(uuid, eventUser, cell));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnjail(CMIPlayerUnjailEvent event) {
        CMIUser user = event.getUser();
        if (user == null) {
            return;
        }
        String actor = contextStore.consume(TrackedCommand.UNJAIL, user.getName(), user.getUniqueId())
            .orElse(AUTOMATIC_SOURCE);
        CMIJailCell cell = event.getCell();
        dispatcher.submit(PunishmentReportFactory.unjail(
            user.getName(),
            user.getUniqueId(),
            actor,
            jailName(cell),
            cell == null ? null : cell.getId()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWarn(CMIPlayerWarnEvent event) {
        CMIUser user = event.getUser();
        CMIPlayerWarning warning = event.getWarning();
        if (user == null || warning == null) {
            return;
        }
        CMIWarningCategory category = warning.getCategory();
        dispatcher.submit(PunishmentReportFactory.warning(
            user.getName(),
            user.getUniqueId(),
            valueOrDefault(warning.getGivenBy(), AUTOMATIC_SOURCE),
            warning.getReason(),
            category == null ? null : category.getName(),
            category == null ? null : category.getPoints()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerUnban(CMIPlayerUnBanEvent event) {
        Player player = event.getPlayer();
        dispatcher.submit(PunishmentReportFactory.unban(
            player == null ? "Unknown" : player.getName(),
            player == null ? null : player.getUniqueId(),
            senderName(event.getBannedBy())
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIpUnban(CMIIpUnBanEvent event) {
        dispatcher.submit(PunishmentReportFactory.ipUnban(event.getIp(), senderName(event.getBannedBy())));
    }

    private void sendJailReport(UUID uuid, CMIUser eventUser, CMIJailCell cell) {
        try {
            CMIUser user = uuid == null ? eventUser : CMIUser.getUser(uuid);
            if (user == null || !user.isJailed()) {
                return;
            }
            CMIJailCell effectiveCell = cell == null ? user.getCell() : cell;
            dispatcher.submit(PunishmentReportFactory.jail(
                user.getName(),
                user.getUniqueId(),
                playerNameOrAutomatic(user.getJailedBy()),
                user.getJailedReason(),
                user.getJailedUntil(),
                jailName(effectiveCell),
                effectiveCell == null ? null : effectiveCell.getId()
            ));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not read final CMI jail state.", exception);
        }
    }

    private static String jailName(CMIJailCell cell) {
        return cell == null || cell.getJail() == null ? null : cell.getJail().getName();
    }

    private static String senderName(CommandSender sender) {
        return sender == null ? AUTOMATIC_SOURCE : valueOrDefault(sender.getName(), AUTOMATIC_SOURCE);
    }

    private static String playerNameOrAutomatic(UUID uuid) {
        return uuid == null ? AUTOMATIC_SOURCE : playerName(uuid);
    }

    private static String playerName(UUID uuid) {
        if (uuid == null) {
            return "Unknown";
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return valueOrDefault(player.getName(), uuid.toString());
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
