package com.lucidaps.cmidiscordpunishments.command;

import com.lucidaps.cmidiscordpunishments.config.PluginSettings;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class PunishmentCommandParser {
    private static final Set<String> CMI_ROOTS = Set.of("cmi", "cmi:cmi");

    public Optional<ParsedCommand> parse(String rawCommand, PluginSettings settings) {
        if (rawCommand == null) {
            return Optional.empty();
        }
        String command = rawCommand.trim();
        while (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.isEmpty()) {
            return Optional.empty();
        }

        String[] tokens = Arrays.stream(command.split("\\s+"))
            .filter(token -> !token.isBlank())
            .toArray(String[]::new);
        if (tokens.length < 2) {
            return Optional.empty();
        }

        String label = normalize(tokens[0]);
        if (CMI_ROOTS.contains(label)) {
            if (tokens.length < 3) {
                return Optional.empty();
            }
            TrackedCommand type = baseSubcommand(normalize(tokens[1]));
            return type == null ? Optional.empty() : Optional.of(new ParsedCommand(type, tokens[2]));
        }

        TrackedCommand type = directCommand(label, settings);
        return type == null ? Optional.empty() : Optional.of(new ParsedCommand(type, tokens[1]));
    }

    private static TrackedCommand baseSubcommand(String subcommand) {
        return switch (subcommand) {
            case "mute" -> TrackedCommand.MUTE;
            case "unmute" -> TrackedCommand.UNMUTE;
            case "unjail" -> TrackedCommand.UNJAIL;
            default -> null;
        };
    }

    private static TrackedCommand directCommand(String label, PluginSettings settings) {
        if (settings.muteAliases().contains(label)) {
            return TrackedCommand.MUTE;
        }
        if (settings.unmuteAliases().contains(label)) {
            return TrackedCommand.UNMUTE;
        }
        if (settings.unjailAliases().contains(label)) {
            return TrackedCommand.UNJAIL;
        }
        return null;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
