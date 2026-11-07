# MaceCorePickup

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
