# Paper plugin downloads

## SFMode — 0.2.0 staging build only

[Download JAR](SFMode-0.2.0.jar) · [Source ZIP](SFMode-0.2.0-source.zip) · [Source branch](https://github.com/Bernd502/Codexthingy/tree/work/sfmode)

Paper 1.21.11; requires LuckPerms. Startup, console status and UUID allowlist were smoke-tested. Player session transitions, actual inventory recovery, item rollback and all event paths need staging tests. **Not ready for a live server.**

# Paper server plugins

## CoreSpawnGuard

**[Download CoreSpawnGuard 1.0.0](CoreSpawnGuard-1.0.0.jar)** · [Source ZIP](CoreSpawnGuard-1.0.0-source.zip)

For Paper 1.21.11 with BuffedItems **1.9.0**. Disables configured cores' active abilities and passive effects inside a configurable square around spawn while players keep their items. Defaults to a 32-block radius in `world`; edit the plugin config for other worlds and core IDs. OPs and the console can use `/corespawn status|on|off|radius <blocks>`. BuffedItems removes passive effects on its next effect pass, usually within one second. A timed active effect cast before entering spawn can last until its normal expiry.

## MaceLimiter

**[Download MaceLimiter 1.0.0](MaceLimiter-1.0.0.jar)** · [Source ZIP](MaceLimiter-1.0.0-source.zip)

Separate Paper 1.21.11 plugin. Defaults to one mace. OPs and the console can run `/mace status [page]`, `/mace on`, `/mace off`, and `/mace limit <number>`. Status uses a colored meter and lists holders and locations. Existing maces found on first scan are kept, even above the cap. Offline players and unloaded chunks are shown at their last known location once discovered. Put this JAR in the server's `plugins` folder; it can run beside MaceCorePickup.

## MaceCorePickup


Latest version: **1.0.3** for Paper 1.21.11.

[Download JAR](MaceCorePickup-1.0.3.jar) · [Download source](MaceCorePickup-1.0.3-source.zip)

This is a standalone Paper server plugin, not a Zoom client mod. Put the JAR in the server's `plugins` folder with BuffedItems. Non-OP players cannot carry a configured BuffedItems core and a mace together, regardless of pickup order, or store a mace in a container, bundle, item frame or armor stand. Hopper mace transfers are blocked. Core IDs and the `buffeditems:buffeditem_id` key are configurable.

Operators or the console can use `/mcp on|off|toggle|status|inspect`; the state is saved. The source ZIP contains the full Gradle project and installation instructions.

## CoreCraftOnce

[Download CoreCraftOnce 1.0.1](CoreCraftOnce-1.0.1.jar) · [Source ZIP](CoreCraftOnce-1.0.1-source.zip)

Everyone may use `/cco status` or the `/cco` status menu. Only admins may reset or give cores. Install it on Paper 1.21.11 alongside BuffedItems.

## HeartCraftGuard

[Download HeartCraftGuard 1.0.0](HeartCraftGuard-1.0.0.jar) · [Source ZIP](HeartCraftGuard-1.0.0-source.zip)

Separate Paper 1.21.11 plugin requiring LifeStealZ. Prevents its marked hearts from being crafting ingredients. Plain anvil renaming remains available if the marker survives; anvil results that lose it are blocked.
