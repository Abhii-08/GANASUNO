# GANASUNO 23.0 — seven partial feature upgrades

This is an incremental source release based on 22.0. Seven existing partial entries (#20, #23, #57, #89, #95, #123, #177) are improved; none is promoted to complete against the full catalogue. Carried-forward totals: 131 existing/implemented source, 19 partial, 50 not implemented. These are source classifications, not verified APK features.

- Native desktop: optional Qt two-deck equal-power crossfade, 0–15 seconds, manually assigned album continuity, and saved queue with selected track and album tags (not the playback position). Use `desktop/Open-Native-Windows.bat` or install the optional `desktop/requirements-native.txt` and run `python desktop/native_player.py`. Qt playback, timing and OS compatibility need live testing.
- Android offline storage: **Verify saved audio integrity** compares the SHA-256 and byte length with the receipt created at import/download. Older files do not have a receipt; it reports that explicitly. Authorized direct audio or user-selected files only, up to 100 MiB. Receipts are local preferences, not guaranteed to survive backup/restore.
- Current track waveform and music tags: multiple BPM candidates based on onset autocorrelation; approximate estimates, not verified metadata.
- Public catalogue curator tool: remember up to 30 channel IDs, browse playlists, about, or forget a saved channel. Official API key required; not a YouTube Music editorial feed.
- MusicBrainz artist details: manually verify and link a YouTube channel, browse uploads and about, replace/unlink. Maintainer contact and official public API key required; no automatic cross-provider identity verification.

## Checks executed
- Kotlin standalone compilation and 20 new assertions; 41 v22 regression assertions passed.
- Python native queue/session: 5 tests passed (including 4 new tests).
- Python backend: 37 tests passed.
- Native Python syntax compilation and 32 XML files parsed.
- Android Gradle build, emulator, Qt GUI, real audio and live API requests not verified: Android SDK and PySide6 not available here.

## Manual acceptance
Build `gradlew.bat assembleFossDebug --stacktrace` with JDK 17/SDK 36. Verify imported audio and checksum audit, restart and audit again. Test curator and artist link add/remove with valid API credentials. Test the tempo picker against known tracks. On desktop play two local tracks, test crossfade, same-album suppression, skip and pause mid-fade, then restart to confirm the queue restores. No Android crossfade is claimed.
