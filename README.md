# CMIDiscordPunishments

[![CI](https://github.com/lucidaps/CMIDiscordPunishments/actions/workflows/ci.yml/badge.svg)](https://github.com/lucidaps/CMIDiscordPunishments/actions/workflows/ci.yml)

A Paper 1.21.11 plugin that listens to CMI's punishment API events and sends successful moderation actions to a Discord webhook.

## Requirements

- Paper 1.21.11 on Java 21
- CMI 9.8.6.4 and its required CMILib version
- A webhook created in a private Discord staff channel

## Build and install

1. Download the plugin JAR from the [latest GitHub release](https://github.com/lucidaps/CMIDiscordPunishments/releases/latest), or run `mvn clean package` with Java 21 to build it yourself.
2. Copy `CMIDiscordPunishments-1.0.0.jar` into the server's `plugins` directory.
3. Start the server once, then open `plugins/CMIDiscordPunishments/config.yml`.
4. Set `webhook.url` and `server-name`.
5. Run `/cmidp reload`, followed by `/cmidp test`.

The webhook URL is a secret. Do not post the configuration publicly. IP-ban reports include the full IP address, so the destination should be restricted to trusted staff.

## Reported actions

- Ban, temporary ban, unban
- IP ban, temporary IP ban, IP unban
- Kick
- Jail and unjail
- Mute and unmute
- Warning, including its CMI category and points

CMI API events are used for all actions that expose one, including actions initiated by console automation or another plugin. CMI does not expose mute events, so mute and unmute are detected by checking CMI state before and after recognized commands. Direct custom command aliases can be added under `commands.aliases`.

Cancelled events, commands that fail permission or validation checks, and mute commands that do not change CMI state are not reported. Reports are queued and sent off the server thread. Network errors, Discord rate limits, and server errors are retried in memory; the queue is not persisted across a crash or forced shutdown.

## Administration

- `/cmidp reload` reloads and validates the configuration. An invalid reload leaves the previous working settings active.
- `/cmidp test` sends a test embed and reports Discord's result to the command sender.
- Permission: `cmidiscordpunishments.admin` (operators by default).
