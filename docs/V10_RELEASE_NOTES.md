# GANASUNO 10.0 — Partial-feature completion batch 2

Base: 9.0. No new catalogue entries added. Ten previously partial items have source implementations addressing their stated gaps, within the app's existing Android and browser-companion scope. Current catalogue: 80 source-marked implemented (70 added + 10 existing), 70 partial, 50 not implemented; coverage remains 150/200. These are provisional source classifications, not runtime certification. Previous status entries were carried forward without full independent verification.

| ID | Implementation / location |
|---|---|
| 24 | Settings → Playback controls → Manual fade in/out. Choose 100–30,000 ms, fade in/play, fade out/pause or cancel. Manual fades cancel the sleep timer to avoid conflicting volume writes; not overlapping crossfade. |
| 27 | Settings → New features (v8) → Explicit filter. Provider badge parsing records known explicit IDs, manual tagging remains available. Unknown or unlabelled tracks are not guaranteed clean. |
| 51 | Now playing → Fullscreen / Exit fullscreen, and Desktop Companion → Fullscreen. The browser companion is not a native desktop binary. |
| 107 | Library → Playlists → select playlist → Change cover. Pick a local image, crop by centre/top-left/bottom-right, preview and save. Cover stored as bounded JPEG, moves on rename, removes on deletion. |
| 116 | Settings → Listening analytics → Most played (counts). Counts start in v10, one play per playback entry after five seconds of tracked active playback; previous listening duration cannot be reconstructed as play counts. |
| 128 | Storage and backups → Offline storage → authorized audio saving; Cancel aborts the transfer and cleans incomplete output. Local copy cancellation is cooperative. Already committed files are not removed by cancellation. Individual/bulk deletion remains available. |
| 150 | Listen Together → Members and host controls → Block installation for this room. Upgrade the bundled server too; new v10 rooms require a random per-server installation token, shared with server only. Blocking covers the room lifetime, removes matching sessions and prevents same-token rejoin. Token reset can evade blocking; not account authentication. Legacy rooms have reduced guarantees. |
| 173 | GANASUNO Library widget → tap one of the first three alphabetical playlists to open/play it; refresh when leaving app and periodically. Android launcher sizing and start behavior still need testing. |
| 178 | Desktop browser companion library sidebar: draggable vertical scrollbar, keyboard operation and synchronization with native scrolling. |
| 191 | Settings → Diagnostics: live refresh, search/filter, clear and filtered text export. Private bounded in-memory app events (no system-wide logcat or credentials). |

## Checks run
- Kotlin PSI parsed 51 app Kotlin files: zero syntax errors. This is not Android compilation or type checking.
- 31 Android XML files parsed for well-formedness.
- 18 compiled standalone Kotlin assertions on fade math, crop geometry, play ranking and explicit-badge identifiers.
- 13 Python room-server tests passed (11 previous + 2 new tests covering blocking, privacy, rejoin and multiple sessions).
- All desktop JS passed `node --check`.
- Android SDK and Gradle dependencies are unavailable in this environment; APK compilation, emulator, UI, audio, live provider parsing, actual downloads, Android/server integration and browser runtime were not verified.

## Reproduce new pure Kotlin tests
```
P=app/src/main/java/com/abhi/ganasuno
kotlinc "$P/data/Song.kt" "$P/data/Batch10.kt" tests/V10BatchTest.kt -include-runtime -d v10-tests.jar
java -jar v10-tests.jar
```
Run `python -m unittest -v` from `server/`.

## Device acceptance checks still required
Open with JDK 17 and Android SDK 36, build via `gradlew.bat assembleDebug`, install on Pixel 4 or a physical phone. Check fade pause/play and interruption; explicit badge filtering; full screen open/exit and orientation; playlist crop persistence across rename/restart; counted plays through pause/resume/skip and repeat; cancel direct-file download before commit; widget taps from a cold start; live log updates and export. Test desktop fullscreen/scrollbar in the actual browser. Test two Android devices against the updated trusted HTTPS room server. Do not treat these features as production-certified before device testing.
