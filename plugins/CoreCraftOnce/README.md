# CoreCraftOnce 1.0.0 - Paper 1.21.11

[Download the plugin JAR](CoreCraftOnce-1.0.0.jar) | [Download the complete source ZIP](CoreCraftOnce-1.0.0-source.zip) | [SHA-256 checksums](SHA256SUMS)

Install the JAR in your Paper server's `plugins/` folder alongside BuffedItems and CoreLimiter, then restart. Requires Java 21+. Each configured core can be successfully crafted up to its server-wide limit. Counts persist across restarts, and shift-click crafting is blocked for configured cores.

Run `/cco` in-game to open the status menu: green cores have not been crafted; red cores have. Click a red entry to reset just that core. Operators can also run `/cco reset <core-id>`, `/cco status`, and `/cco give <player> <core-id> [amount]`. Configure limits in `plugins/CoreCraftOnce/config.yml`; `0` means unlimited. Admin-issued cores do not consume recipe limits.

The source project ZIP includes the Gradle build files and all plugin source.
