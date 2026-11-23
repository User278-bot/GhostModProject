# Changelog
## v1.7.0 (2026/11/23)
### feat
- Added support for Minecraft 26.3
- Unified all network packets (including authentication) into binary Protobuf (`oneof`) with HMAC-SHA256 CHAP authentication and protocol version validation
- Added asynchronous skin caching (`GhostSkinCache`) to prevent redundant skin fetches when ghosts re-enter view distance (#68)

### fix
- Matched sneaking nametag visibility behavior to vanilla Minecraft (#51)
- Improved ghost synchronization timing and movement interpolation (#93)
- Fixed arm swing tick calculation on Minecraft 26.3+
- Made connection/disconnection in the configuration screen asynchronous to prevent UI freezes and unintended reconnections
- Fixed skin layer changes not syncing immediately after closing the Skin Customization screen
- Fixed `CustomModelData` model value restoration and improved exception safety for invalid UUIDs and item IDs

### perf
- Optimized ghost equipment synchronization to update only when equipment changes
- Skipped unnecessary entity scanning when no ghosts are present and skipped player data conversion while disconnected

## ⚠️ IMPORTANT NOTICE

* **Breaking Change:** This version refreshes the communication protocol and is **not compatible** with older server software (`v1.6.x` or earlier).
* **Action Required:** Please ensure your server software (**Ghost Mod Server**) is updated to **`v1.7.x`** before installing this update. Mod and Server must share the same `X.Y` version (e.g., `v1.7.x`).
