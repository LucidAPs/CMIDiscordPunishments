package com.lucidaps.cmidiscordpunishments;

import com.lucidaps.cmidiscordpunishments.command.AdminCommand;
import com.lucidaps.cmidiscordpunishments.command.CommandContextStore;
import com.lucidaps.cmidiscordpunishments.config.PluginSettings;
import com.lucidaps.cmidiscordpunishments.config.SettingsException;
import com.lucidaps.cmidiscordpunishments.config.SettingsLoader;
import com.lucidaps.cmidiscordpunishments.discord.DiscordBotDispatcher;
import com.lucidaps.cmidiscordpunishments.listener.CmiPunishmentListener;
import com.lucidaps.cmidiscordpunishments.listener.MuteCommandListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public final class CMIDiscordPunishments extends JavaPlugin {
    private final AtomicReference<PluginSettings> settings =
        new AtomicReference<>(PluginSettings.disabledDefaults());
    private DiscordBotDispatcher dispatcher;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadInitialSettings();

        dispatcher = new DiscordBotDispatcher(settings.get(), getLogger());
        CommandContextStore contextStore = new CommandContextStore();
        getServer().getPluginManager().registerEvents(
            new CmiPunishmentListener(this, dispatcher, contextStore),
            this
        );
        getServer().getPluginManager().registerEvents(
            new MuteCommandListener(this, settings::get, dispatcher, contextStore),
            this
        );

        AdminCommand adminCommand = new AdminCommand(this, dispatcher);
        PluginCommand command = getCommand("cmidiscordpunishments");
        if (command == null) {
            throw new IllegalStateException("cmidiscordpunishments is missing from plugin.yml");
        }
        command.setExecutor(adminCommand);
        command.setTabCompleter(adminCommand);

        if (settings.get().discord().isEmpty()) {
            getLogger().warning("No Discord bot destination is configured. Set discord.bot-token and "
                + "discord.channel-id in config.yml, then run /cmidp reload.");
        } else {
            getLogger().info("CMI punishment reporting is enabled for server " + settings.get().serverName() + ".");
        }
    }

    @Override
    public void onDisable() {
        if (dispatcher != null) {
            dispatcher.close();
        }
    }

    public PluginSettings settings() {
        return settings.get();
    }

    public ReloadResult reloadPluginSettings() {
        reloadConfig();
        try {
            PluginSettings loaded = SettingsLoader.load(getConfig());
            settings.set(loaded);
            dispatcher.updateSettings(loaded);
            return new ReloadResult(true, loaded.discord().isPresent()
                ? "Configuration reloaded; Discord reporting is enabled."
                : "Configuration reloaded; reporting remains disabled until the Discord bot token and channel ID "
                    + "are set.");
        } catch (SettingsException | IllegalArgumentException exception) {
            getLogger().log(Level.WARNING, "Configuration reload rejected: " + exception.getMessage());
            return new ReloadResult(false, "Reload failed: " + exception.getMessage());
        }
    }

    private void loadInitialSettings() {
        try {
            settings.set(SettingsLoader.load(getConfig()));
        } catch (SettingsException | IllegalArgumentException exception) {
            getLogger().log(Level.SEVERE, "Invalid config.yml; Discord reporting will remain disabled: "
                + exception.getMessage());
            settings.set(PluginSettings.disabledDefaults());
        }
    }

    public record ReloadResult(boolean successful, String message) {
    }
}
