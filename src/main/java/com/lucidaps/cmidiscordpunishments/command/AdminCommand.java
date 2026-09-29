package com.lucidaps.cmidiscordpunishments.command;

import com.lucidaps.cmidiscordpunishments.CMIDiscordPunishments;
import com.lucidaps.cmidiscordpunishments.discord.DeliveryResult;
import com.lucidaps.cmidiscordpunishments.discord.DiscordBotDispatcher;
import com.lucidaps.cmidiscordpunishments.model.PunishmentReport;
import com.lucidaps.cmidiscordpunishments.model.PunishmentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "cmidiscordpunishments.admin";
    private static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
        .append(Component.text("CMIDP", NamedTextColor.BLUE))
        .append(Component.text("] ", NamedTextColor.DARK_GRAY));

    private final CMIDiscordPunishments plugin;
    private final DiscordBotDispatcher dispatcher;

    public AdminCommand(CMIDiscordPunishments plugin, DiscordBotDispatcher dispatcher) {
        this.plugin = plugin;
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission(PERMISSION)) {
            send(sender, NamedTextColor.RED, "You do not have permission to use this command.");
            return true;
        }
        if (args.length != 1) {
            sendUsage(sender, label);
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                CMIDiscordPunishments.ReloadResult result = plugin.reloadPluginSettings();
                send(sender, result.successful() ? NamedTextColor.GREEN : NamedTextColor.RED, result.message());
                yield true;
            }
            case "test" -> {
                sendTest(sender);
                yield true;
            }
            default -> {
                sendUsage(sender, label);
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission(PERMISSION) || args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return List.of("reload", "test").stream().filter(value -> value.startsWith(prefix)).toList();
    }

    private void sendTest(CommandSender sender) {
        PunishmentReport report = PunishmentReport.builder(PunishmentType.TEST)
            .target(plugin.settings().serverName())
            .actor(sender.getName())
            .reason("Manual Discord bot test from /cmidp test")
            .detail("Status", "Configuration loaded successfully")
            .build();
        send(sender, NamedTextColor.GRAY, "Sending a test report to Discord...");
        dispatcher.submit(report).whenComplete((result, error) -> runSync(() -> {
            if (error != null) {
                send(sender, NamedTextColor.RED, "Test failed: " + error.getMessage());
                return;
            }
            sendResult(sender, result);
        }));
    }

    private void sendResult(CommandSender sender, DeliveryResult result) {
        if (result.successful()) {
            send(sender, NamedTextColor.GREEN, "Test report delivered to Discord.");
        } else {
            send(sender, NamedTextColor.RED, "Test was not delivered: " + result.message());
        }
    }

    private void runSync(Runnable task) {
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    private static void sendUsage(CommandSender sender, String label) {
        send(sender, NamedTextColor.GRAY, "Usage: /" + label + " <reload|test>");
    }

    private static void send(CommandSender sender, NamedTextColor color, String message) {
        sender.sendMessage(PREFIX.append(Component.text(message, color)));
    }
}
