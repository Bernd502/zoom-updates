# LiteGuard

LiteGuard is a Paper moderation plugin focused on early ban rejection and low-cost chat enforcement. Text mute, voice mute and ban checks use UUID-keyed `ConcurrentHashMap` caches; no chat or microphone event performs SQL. All SQLite work runs on a dedicated bounded executor through HikariCP.

## Build

Requires JDK 21 and Gradle Wrapper. Build and test with:

```sh
./gradlew clean test shadowJar
```

The installable shaded jar is `build/libs/LiteGuard-1.0.0.jar`. Paper API, LuckPerms and Simple Voice Chat APIs are compile-only and are not bundled. HikariCP, SQLite JDBC and its service registrations are shaded into the jar.

## Install and configure

1. Install Paper 1.21.11, Java 21, LuckPerms 5.4, and Simple Voice Chat 2.5.x if voice moderation is needed.
2. Put `LiteGuard-1.0.0.jar` in `plugins/` and start the server once.
3. Set explicit LuckPerms group weights (for example, `/lp group moderator setweight 10` and `/lp group admin setweight 50`). LiteGuard compares the greatest weight among each player's effective inherited groups; an unset weight is 0. Player staff must strictly outrank the target; equal weights are denied. Console is trusted.
4. Grant only the needed command nodes, for example `/lp group moderator permission set liteguard.command.mute true`, `/lp group moderator permission set liteguard.command.unmute true`, and `/lp group helper permission set liteguard.bypass.servermute true`. The command permissions default to op.
5. Configure the optional HTTPS Discord webhook in `plugins/LiteGuard/config.yml`. It receives punishment/revocation type, player name and UUID, reason, staff name, and expiry. Treat the webhook URL as a credential; LiteGuard does not print it.
6. Restart the server. SQLite is stored in `plugins/LiteGuard/liteguard.db` and uses WAL mode. Keep this database local to one Paper server; SQLite is not a shared multi-server moderation database. Stop the server before backing up or restoring this file (including any WAL sidecar files).

## Commands

- `/ban <player> [reason]`, `/tempban <player> <duration> [reason]`, `/unban <player>`
- `/mute <player> [duration] [reason]`, `/unmute <player>`
- `/vcmute <player> [duration] [reason]`, `/vcunmute <player>`
- `/servermute` toggles the persisted global text and voice lockdown. Players with `liteguard.bypass.servermute` can still chat and speak.

Durations support `30s`, `15m`, `12h`, `7d`, weeks (`1w`) and contiguous combinations (`1w2d3h`). The default maximum is 3650 days.

## Data and concurrency notes

- `AsyncPlayerPreLoginEvent` waits up to the configured cache-readiness limit on its own asynchronous login thread. If the database/cache cannot initialize, the login is denied with a retry message rather than allowing a ban-check bypass. No database work blocks the main server thread.
- Player names are indexed from login history. Offline targets must have joined while LiteGuard was active or still be in Paper's in-memory offline-player cache.
- Cache snapshots refresh every 30 seconds by default to pick up database-side changes. This is for same-process consistency and manual DB maintenance, not a cluster protocol.
- Voice mute cancels microphone packets immediately when the Simple Voice Chat server plugin is installed; without it, voice punishments remain stored but cannot intercept voice packets. Feedback is scheduled to the server thread and throttled per player.
- Webhook sends are asynchronous, HTTPS-only, timeout-bounded, and never retry automatically.

## API version notes

The official Simple Voice Chat Maven endpoint returned 404 for API `2.5.24` during verification. This build compiles against published API `2.5.27`, the nearest available 2.5.x API; the source uses only the shared server microphone-event API. LuckPerms 5.4 does not expose rank weight on `CachedMetaData`; LiteGuard checks LuckPerms' effective inherited group weights instead.
