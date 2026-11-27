# GANASUNO 27.0 — Partial feature follow-up

Requested target: complete 20 of 24 partial features. **Not achieved.** Six partial feature areas received actual source changes (#58, #59, #82, #110, #113, #177); none are promoted to complete. Totals remain 137 source-complete/existing, 24 partial, 39 missing. Original version 26 ZIP unchanged.

## Changes
- Combined song details optionally fetch community dislike estimates alongside public metadata and extractor information. The video ID is sent only after confirmation. Unknown fields remain unknown.
- Public comment and reply composition uses the official YouTube Data API, an explicitly reviewed public message and a short-lived user-supplied OAuth token. Reply parent is verified against the video. Comment liking is not exposed by the official API. Ambiguous failures should not be retried blindly.
- Regional chart screen adds a sample across IN, US, GB, JP, BR and DE. Ranking counts markets first, then rank points; unavailable markets are excluded and successful markets are named. At least two markets required. This is not an official global chart.
- Linked remote playlist can now be imported into the local playlist, retaining existing order, local-only tracks and existing metadata. Remote ownership checked; up to 1000 remote entries; no import if pagination incomplete; local association rechecked on apply. No automatic or destructive sync.
- Liked-video bulk import explicitly preserves existing favourites and order. Only local metadata is copied, not a persistent account login.
- Native Qt local queue supports M3U/M3U8 import/export. Imports require UTF-8, existing local audio paths and recognized extensions; URL entries excluded. Export stores local absolute paths, not audio. GUI testing pending.

## Usage
Android: Settings → More features → Batch 27 · account and blog; for chart and combined metadata use the existing Batch 25 and Batch 22 menus. Public comment screen has write/reply actions. Desktop: run `desktop/native_player.py` after installing its optional requirements, then use Import M3U / Export M3U.

## Actual validation
- 21 standalone Kotlin assertions compiled and executed (`Batch27.kt`, `Song.kt`, `tests/V27BatchTest.kt`).
- 97 Android Kotlin files parsed with Kotlin PSI; zero syntax errors. This is not Android type checking.
- 32 XML files parsed.
- 38 backend and 13 desktop Python unit tests passed (includes three new M3U tests).
- No Android SDK, APK build, emulator, native GUI, or live OAuth/API tests in this environment.

## References
- https://developers.google.com/youtube/data/reference/rest/v3/commentThreads/insert
- https://developers.google.com/youtube/data/reference/rest/v3/comments/insert
- https://developers.google.com/youtube/data/reference/rest/v3/comments
- https://developers.google.com/youtube/data/reference/rest/v3/playlistItems/list

## Device acceptance
Build both variants with `gradlew.bat assembleFossDebug assembleFullDebug --stacktrace`. Use a controlled test account/video to post and reply, verify actual visibility and failure handling, then test pull twice with duplicate and local-only tracks, wrong owner, association changed and long remote playlist. Verify chart with unavailable countries and no API key. Test native GUI import/export on each target platform. Do not publish as a tested release until successful.
