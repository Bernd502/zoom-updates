# SFTab 1.0.0

A separate **Paper 1.21.11 / Java 21** server plugin for customizing the native Tab player list. No client mod or required plugin dependencies. Optional LuckPerms adds rank prefixes, suffixes and primary-group sorting; optional PlaceholderAPI adds installed `%expansion_placeholder%` values.

## Install and edit

1. Put `SFTab-1.0.0.jar` in the server's `plugins/` directory and restart.
2. As OP, run `/sftab edit`. Click a setting to browse or edit it. Text/numeric edits prompt in chat; use `cancel` to cancel, or `<empty>` for a blank line.
3. In a list, use **Add line** to append or **Shift + right-click** to remove a line. The editor supports pagination.
4. Open `worlds`, `animations`, or `group-formats` and use **Add entry** to create a world layout, animation or rank format. New world layouts copy the default layout. Keys created in game use letters, digits, underscores and hyphens.
5. Everything saves and applies immediately. You can also edit `plugins/SFTab/config.yml` and run `/sftab reload`. Reload file changes before using the GUI if you want to keep those edits.

Customizable: header/footer lines, colors, gradients, animations, player names, rank formats, group order, sorting direction and mode, update interval, disabled worlds, per-world layouts, server name and website. All settings are available in the editor as well as YAML.

Text templates use MiniMessage: `<aqua>Hello</aqua>`, `<bold>Title</bold>`, `<#55ccff>Text</#55ccff>`, `<gradient:aqua:blue>Title</gradient>`. `server-name`, `website` and player-derived values are literal text; apply colors around their placeholders in the header/footer. LuckPerms prefixes/suffixes accept MiniMessage or legacy `&` colors. PlaceholderAPI output is parsed as MiniMessage; install the desired expansions separately.

| Placeholder | Meaning |
| --- | --- |
| `{server}`, `{website}` | Your configured literal server name and website |
| `{player}`, `{display_name}`, `{uuid}` | Player's real name, plain display name and UUID |
| `{prefix}`, `{suffix}`, `{group}` | LuckPerms metadata; blank prefix/suffix and `default` group without LuckPerms |
| `{online}`, `{max_players}` | Online players visible to that viewer and server capacity |
| `{ping}`, `{world}`, `{level}` | Ping in milliseconds, world and experience level |
| `{health}`, `{hearts}` | Health points or hearts as text |
| `{tps}` | One-minute TPS capped at 20 |
| `{date}`, `{time}` | Server-local date and 24-hour time |
| `{animation:welcome}` | Current frame from `animations.welcome` |
| `%expansion_placeholder%` | PlaceholderAPI, when installed |

Animation frames advance once each `refresh-ticks` (20 ticks = one second). Normal placeholders work inside frames; nested animations do not expand recursively. Unknown placeholders remain visible to help spot typos.

World layouts inherit omitted header, footer and player-name fields. A matching `group-formats` entry overrides the player's name format, including the world name format. Sorting can use configured primary-group order, alphabetic name, ping, world, or `none`. Unlisted groups follow the listed groups; names break ties. Descending reverses the order.

## Commands and permissions

- `/sftab edit` — open the complete editor (also `/sftab gui`).
- `/sftab reload` — reload YAML; malformed configuration leaves previous settings active.
- `/sftab on` and `/sftab off` — persistently enable/disable styling.
- `/sftab status` — version, enabled state, update interval and integration status.
- `/sftab set server-name Your Server` — change server name. Also supports `website`, `player-name` and `refresh-ticks`.
- `/sftab set header 2 <aqua>Your title</aqua>` — replace line 2. `footer` also works; specifying the next line appends it.
- `/sftab cancel` — cancel your pending chat edit.
- `/sftab toggle` — hide/show the custom header/footer for yourself until reconnecting. Player entry names and sorting are server-wide.

`sftab.admin` controls every administration command and editor action and defaults to OP. `sftab.toggle` defaults to all players. Console can use administration commands except the menu. Changes use validated snapshots and an atomic file replacement where supported. `refresh-ticks` is restricted to 2..1200; lists are limited to 100 entries and text to 4096 characters.

## Native-list behavior

SFTab styles actual server player entries. Minecraft determines rows, columns, skin icons and native latency bars; this release does not fabricate slots/players or change below-name tags or scoreboard objectives. Vanilla visibility/vanish filtering is preserved. A player's entry name is the same for every viewer; headers/footers are rendered for their viewer. Use one plugin to manage Tab names/headers/order to avoid competing updates.

Turning the plugin off restores the header/footer, entry name and list order captured before SFTab managed each player. Disabling a world restores players in that world. Personal toggles only affect headers/footers and are session-local.

## Build and verification

Run `./gradlew clean test build` with Java 21. Output: `build/libs/SFTab-1.0.0.jar`. Paper API and LuckPerms are compile-only; do not bundle their jars. Dependencies resolve from Maven Central and Paper's Maven repository.

10 tests cover configuration types/bounds, immutable snapshots, world inheritance, text escaping, animation cycling and stable sorting. Paper 1.21.11 build 132 plus LuckPerms 5.5.71 passed startup, command editing, on/off, status and reload checks. Invalid refresh rates and malformed YAML were rejected while previous settings stayed active. No real-player GUI/visual test or live PlaceholderAPI expansion test has been performed yet.
