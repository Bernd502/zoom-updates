# Zoom update channel

This public repository contains only Zoom release binaries and the update manifest. Source code remains in the private Crown Client repository.

`latest.json` contains the latest version and SHA-256 for `zoom.jar`. The client reads this manifest at startup over HTTPS. To publish a new build, replace `zoom.jar`, add its versioned JAR, update the manifest hash and version, then commit and push.
