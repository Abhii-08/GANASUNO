# GANASUNO 24.0 — integrity, synchronization and desktop resume

Incremental release from 23.0. Three partial catalogue entries (#123, #149, #177) receive additional functionality. No entry is promoted to complete: 131 implemented/existing source, 19 partial, 50 missing. #132 backup integrity also benefits from this change without changing its classification.

- Offline audio: new app-owned audio copies write portable checksum receipts. The archive stores SHA-256 and byte-count manifest entries; restoration validates file names, duplicate entries, all hashes, and manifest/file correspondence before applying the library. Existing portable receipts are checked on export. Old archives without a manifest remain supported and do not gain a verified receipt automatically. For old downloads, running integrity audit migrates a matching legacy receipt to the portable format; until then the archive records the current bytes as its baseline. A checksum is not an authenticity signature. Authorized audio only; no bypass of streaming service restrictions.
- Listen Together: recent minimum RTT estimates one-way latency, host sample age includes dispatch delay, server advances the sample based on speed and age, and the guest adds estimated arrival delay. Server uses a monotonic clock. Older clients may omit sample age. No guarantee of exact synchronization; network asymmetry, buffering and foreground-only polling remain limitations. Deploy the updated server for host sample age handling.
- Native desktop: versioned local session includes track position, volume, crossfade length, and album-continuity setting; restores version 1 queue files as well. On restart user presses Play to resume (seekable local media only). Manual GitHub Actions workflow packages an optional PyInstaller one-directory build on Windows/macOS/Linux. The workflow has not been run; no executable or installers are supplied. Qt multimedia plugins and codecs must be checked on each target OS.

## Checks actually executed
- Kotlin standalone compilation with filesystem Context/LibraryStore stubs: 18 new assertions and 6 previous archive assertions pass.
- Python backend: 38 tests pass, including new sample age validation.
- Python desktop/control tests: 10 pass, including session restore and version compatibility.
- Native player/queue/server Python syntax compilation; 32 XML documents parsed.
- Android Gradle build, Android emulator, Qt GUI playback, packaging workflow and live network sync not run in this environment.

## Acceptance steps
1. Build `gradlew.bat assembleFossDebug --stacktrace` with JDK 17 and SDK 36. Test import, verify, backup and restore an authorized local audio file; confirm receipt and rejection of a damaged archive. Check old v23 archives still restore.
2. Run updated room server behind trusted HTTPS; test two devices, publish at 1.5x, pause, seek, check delay and disconnect recovery.
3. Run native Qt desktop player, seek within a local file, change volume/crossfade, close/reopen and press Play. Check the track resumes and same-album behavior remains. Trigger desktop workflow manually if desired; smoke-test outputs on each OS.
