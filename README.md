# Zoom update channel

This public repository serves Zoom release binaries and a signed update manifest. Source code remains in the private Crown Client repository.

The client verifies `latest.sig` with an embedded Ed25519 public key before trusting `latest.json`. The manifest contains the version and SHA-256 of `zoom.jar`. The matching private signing key is kept outside this repository.

To publish a build, replace `zoom.jar`, add its versioned JAR, update `latest.json` with the version and SHA-256, sign the exact manifest bytes with the private Ed25519 key, and commit the resulting `latest.sig`. The private repository contains the signing script; never upload the private key.
