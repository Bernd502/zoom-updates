# Zoom update channel

This public repository serves Zoom release binaries and a signed update manifest.
The current manual download is **[zoom-1.19.8+26.2.jar](zoom-1.19.8+26.2.jar)**.
Older versioned jars are retained as archives; install only one Zoom jar in a
Minecraft instance. Source code remains in the private Crown Client repository.

The client verifies `latest.sig` with an embedded Ed25519 public key before trusting `latest.json`. The manifest contains the version and SHA-256 of `zoom.jar`. The matching private signing key is kept outside this repository.
The signed automatic-update manifest still points to 2.8.19+26.2. Install
1.19.8 manually until a new manifest is signed and published.

To publish a build, replace `zoom.jar`, add its versioned JAR, update `latest.json` with the version and SHA-256, sign the exact manifest bytes with the private Ed25519 key, and commit the resulting `latest.sig`. The private repository contains the signing script; never upload the private key.
