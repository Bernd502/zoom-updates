# Zoom update channel

This public repository serves Zoom release binaries and a signed update manifest.
The current manual download is **[zoom-1.20.8+26.2.jar](zoom-1.20.8+26.2.jar)**.
Its nine menu images are distributed as a separate [background pack](zoom-menu-backgrounds-v1.zip).
Zoom downloads and verifies the pack automatically, then caches it in the
Minecraft instance's `config/zoom/` folder for offline use. The menus remain
usable with a dark background if the pack is unavailable.
Older versioned jars are retained as archives; install only one Zoom jar in a
Minecraft instance. Source code remains in the private Crown Client repository.

The client verifies `latest.sig` with an embedded Ed25519 public key before trusting `latest.json`. The manifest contains the version and SHA-256 of `zoom.jar`. The matching private signing key is kept outside this repository.
The signed automatic-update manifest still points to 2.8.19+26.2. Install
1.20.8 manually until a new manifest is signed and published. The 1.20.7, 1.20.6, 1.20.5,
1.20.4, 1.20.3 (startup-crashing), 1.20.2, 1.20.1, 1.20.0, 1.19.9 and 1.19.8 JARs remain
available as archives. Future releases advance the last component
from 0 through 9, then increase the middle component and reset the last to 0;
the first component remains 1.

To publish a build, replace `zoom.jar`, add its versioned JAR, update `latest.json` with the version and SHA-256, sign the exact manifest bytes with the private Ed25519 key, and commit the resulting `latest.sig`. The private repository contains the signing script; never upload the private key.

## Separate Paper server plugin

**[MaceCorePickup 1.0.1 JAR](plugins/MaceCorePickup-1.0.1.jar)** · [Source ZIP](plugins/MaceCorePickup-1.0.1-source.zip)

Install this JAR in a **Paper 1.21.11 server's** `plugins` folder, alongside BuffedItems. It is separate from the Zoom Fabric client. A non-OP carrying a configured core cannot pick up a dropped mace. Operators can use `/macecorepickup on`, `off`, `toggle`, or `status`; the setting persists across restarts. Remove any older MaceCorePickup JAR before installing 1.0.1.
