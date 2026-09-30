# CMIDiscordPunishments

[![CI](https://github.com/lucidaps/CMIDiscordPunishments/actions/workflows/ci.yml/badge.svg)](https://github.com/lucidaps/CMIDiscordPunishments/actions/workflows/ci.yml)

A Paper 1.21.11 plugin that listens to CMI's punishment API events and sends successful moderation actions to a Discord channel through a bot account.

## Requirements

- Paper 1.21.11 on Java 21
- CMI 9.8.6.4 and its required CMILib version
- A Discord application with a bot user invited to your server
- A private staff channel where the bot has `View Channel`, `Send Messages`, and `Embed Links`

## Build and install

1. Download the plugin JAR from the [latest GitHub release](https://github.com/lucidaps/CMIDiscordPunishments/releases/latest), or run `mvn clean package` with Java 21 to build it yourself.
2. Copy `CMIDiscordPunishments-1.0.0.jar` into the server's `plugins` directory.
3. Start the server once, then open `plugins/CMIDiscordPunishments/config.yml`.
4. In the [Discord Developer Portal](https://discord.com/developers/applications), create an application and bot, then invite the bot to your server.
5. Enable Developer Mode in Discord, right-click the destination channel, and select **Copy Channel ID**.
6. Set `discord.bot-token`, `discord.channel-id`, and `server-name` in `config.yml`.
7. Run `/cmidp reload`, followed by `/cmidp test`.

The bot token is a secret with control of the bot. Do not commit it or post the configuration publicly; reset the token immediately if it is exposed. IP-ban reports include the full IP address, so the destination channel should be restricted to trusted staff.

The plugin uses Discord's HTTPS API only. It does not open a Gateway connection, require privileged intents, or make the bot appear online.

## Reported actions

- Ban, temporary ban, unban
- IP ban, temporary IP ban, IP unban
- Kick
- Jail and unjail
- Mute and unmute
- Warning, including its CMI category and points

CMI API events are used for all actions that expose one, including actions initiated by console automation or another plugin. CMI does not expose mute events, so mute and unmute are detected by checking CMI state before and after recognized commands. Direct custom command aliases can be added under `commands.aliases`.

Cancelled events, commands that fail permission or validation checks, and mute commands that do not change CMI state are not reported. Reports are queued and sent off the server thread. Network errors, Discord rate limits, and server errors are retried in memory; the queue is not persisted across a crash or forced shutdown.

## Message templates

Each entry under `events` controls whether an action is reported, its embed color, and its compact Discord message. `title` is a single template and `description` is an ordered list of lines:

```yaml
events:
  mute:
    enabled: true
    title: '🔇 {target} MUTED'
    description:
      - 'Reason: {reason}'
      - 'Duration: {duration}'
      - 'Muted by: {actor}'
    color: '#9B59B6'
```

Available placeholders are `{target}`, `{actor}`, `{reason}`, `{duration}`, `{server}`, `{category}`, `{points}`, `{jail}`, `{cell}`, and `{status}`. A description line is left out when any placeholder on that line has no value. Use `description: []` for a title-only embed. Unknown placeholders cause `/cmidp reload` to reject the new configuration and keep the previous working settings.

Durations are displayed in a friendly form such as `3 days` or `2 hours 30 minutes`; permanent punishments display `Permanent`. UUIDs and absolute expiry timestamps are not sent to Discord. IP-ban reports still contain the complete IP address, so use a private staff channel.

## Administration

- `/cmidp reload` reloads and validates the configuration. An invalid reload leaves the previous working settings active.
- `/cmidp test` sends a test embed and reports Discord's result to the command sender.
- Permission: `cmidiscordpunishments.admin` (operators by default).
