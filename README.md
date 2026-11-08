# Zoom update channel

This public repository serves Zoom release binaries and a signed update manifest.
The current manual download is **[zoom-1.21.4+26.2.jar](zoom-1.21.4+26.2.jar)**. The 1.21.3 and older jars remain available as archives.
Its nine menu images are distributed as a separate [background pack](zoom-menu-backgrounds-v1.zip).
Zoom downloads and verifies the pack automatically, then caches it in the
Minecraft instance's `config/zoom/` folder for offline use. The menus remain
usable with a dark background if the pack is unavailable.
Older versioned jars are retained as archives; install only one Zoom jar in a
Minecraft instance. Source code remains in the private Crown Client repository.

The client verifies `latest.sig` with an embedded Ed25519 public key before trusting `latest.json`. The manifest contains the version and SHA-256 of `zoom.jar`. The matching private signing key is kept outside this repository.
The signed automatic-update manifest still points to 2.8.19+26.2. Install
1.21.4 manually until a new manifest is signed and published. The 1.20.9, 1.20.8, 1.20.7, 1.20.6, 1.20.5,
1.20.4, 1.20.3 (startup-crashing), 1.20.2, 1.20.1, 1.20.0, 1.19.9 and 1.19.8 JARs remain
available as archives. Future releases advance the last component
from 0 through 9, then increase the middle component and reset the last to 0;
the first component remains 1.

To publish a build, replace `zoom.jar`, add its versioned JAR, update `latest.json` with the version and SHA-256, sign the exact manifest bytes with the private Ed25519 key, and commit the resulting `latest.sig`. The private repository contains the signing script; never upload the private key.

## Separate Paper server plugin

**[CoreSpawnGuard 1.0.0 JAR](plugins/CoreSpawnGuard-1.0.0.jar)** · [Source ZIP](plugins/CoreSpawnGuard-1.0.0-source.zip)

Standalone Paper 1.21.11 plugin for BuffedItems 1.9.0. Suppresses the configured cores around spawn without removing them. The default square reaches 32 blocks from `world` spawn at every height. See the [plugin guide](plugins/README.md).

**[MaceLimiter 1.0.0 JAR](plugins/MaceLimiter-1.0.0.jar)** · [Source ZIP](plugins/MaceLimiter-1.0.0-source.zip)

Standalone Paper 1.21.11 plugin. The default limit is one mace, and existing maces remain. OPs can view a colored `/mace status` with holders and locations, toggle enforcement, and set the cap. See the [plugin guide](plugins/README.md).


**[MaceCorePickup 1.0.3 JAR](plugins/MaceCorePickup-1.0.3.jar)** · [Source ZIP](plugins/MaceCorePickup-1.0.3-source.zip)

Install this JAR in a **Paper 1.21.11 server's** `plugins` folder, alongside BuffedItems. It is separate from the Zoom Fabric client. A non-OP cannot carry a configured core and a mace together in either pickup order, or store a mace in a container, bundle, item frame or armor stand. Hopper mace transfers are blocked. Operators can use `/mcp on`, `off`, `toggle`, `status`, or `inspect`; the setting persists across restarts. Remove any older MaceCorePickup JAR before installing 1.0.3.

## CoreCraftOnce Paper plugin

**[CoreCraftOnce 1.0.1 JAR](plugins/CoreCraftOnce-1.0.1.jar)** · [Source ZIP](plugins/CoreCraftOnce-1.0.1-source.zip)

On Paper 1.21.11 with BuffedItems, everyone can run `/cco status` or open the read-only `/cco` menu. Only admins can reset counts or give cores. Replace any older CoreCraftOnce JAR before installing it.

## LifeStealZ heart crafting guard

**[HeartCraftGuard 1.0.1 JAR](plugins/HeartCraftGuard-1.0.1.jar)** · [Source ZIP](plugins/HeartCraftGuard-1.0.1-source.zip)

A separate Paper 1.21.11 plugin that allows marked LifeStealZ hearts as ingredients only when the result is named Heart Egg (including color, case, separator and plural variants). Other heart-ingredient recipes stay blocked. A safe anvil rename does not bypass the rule. Install it alongside LifeStealZ.
