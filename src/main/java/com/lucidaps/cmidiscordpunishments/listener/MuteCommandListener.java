package com.lucidaps.cmidiscordpunishments.listener;

import com.Zrips.CMI.Containers.CMIUser;
import com.lucidaps.cmidiscordpunishments.CMIDiscordPunishments;
import com.lucidaps.cmidiscordpunishments.command.CommandContextStore;
import com.lucidaps.cmidiscordpunishments.command.MuteSnapshot;
import com.lucidaps.cmidiscordpunishments.command.MuteTransitionDetector;
import com.lucidaps.cmidiscordpunishments.command.ParsedCommand;
import com.lucidaps.cmidiscordpunishments.command.PunishmentCommandParser;
import com.lucidaps.cmidiscordpunishments.command.TrackedCommand;
import com.lucidaps.cmidiscordpunishments.config.PluginSettings;
import com.lucidaps.cmidiscordpunishments.discord.WebhookDispatcher;
import com.lucidaps.cmidiscordpunishments.model.PunishmentReport;
import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class MuteCommandListener implements Listener {
    private final CMIDiscordPunishments plugin;
    private final Supplier<PluginSettings> settings;
    private final WebhookDispatcher dispatcher;
    private final CommandContextStore contextStore;
    private final PunishmentCommandParser parser = new PunishmentCommandParser();

    public MuteCommandListener(
        CMIDiscordPunishments plugin,
        Supplier<PluginSettings> settings,
        WebhookDispatcher dispatcher,
        CommandContextStore contextStore
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.dispatcher = dispatcher;
        this.contextStore = contextStore;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        inspect(event.getPlayer(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        inspect(event.getSender(), event.getCommand());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemoteServerCommand(RemoteServerCommandEvent event) {
        inspect(event.getSender(), event.getCommand());
    }

    private void inspect(CommandSender sender, String rawCommand) {
        Optional<ParsedCommand> parsed = parser.parse(rawCommand, settings.get());
        if (parsed.isEmpty()) {
            return;
        }
        ParsedCommand command = parsed.get();
        if (command.type() == TrackedCommand.UNJAIL) {
            contextStore.record(command.type(), command.target(), sender.getName());
            return;
        }

        MuteSnapshot before = findSnapshot(command.target());
        if (before == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> verify(command, sender.getName(), before));
    }

    private void verify(ParsedCommand command, String actor, MuteSnapshot before) {
        String lookup = before.uuid() == null ? command.target() : before.uuid().toString();
        MuteSnapshot after = findSnapshot(lookup);
        if (after == null) {
            return;
        }
        MuteTransitionDetector.detect(command.type(), before, after).ifPresent(type -> {
            PunishmentReport.Builder report = PunishmentReport.builder(type)
                .target(after.name())
                .targetUuid(after.uuid())
                .actor(actor);
            if (type == PunishmentType.MUTE) {
                report.reason(after.reason()).expiresAt(after.mutedUntil());
            }
            dispatcher.submit(report.build());
        });
    }

    private MuteSnapshot findSnapshot(String target) {
        try {
            CMIUser user = CMIUser.getUser(target);
            if (user == null) {
                return null;
            }
            return new MuteSnapshot(
                user.getUniqueId(),
                user.getName(),
                user.isMuted(),
                user.getMutedUntil(),
                user.getMutedReason()
            );
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not inspect CMI mute state for " + target + ".", exception);
            return null;
        }
    }
}
