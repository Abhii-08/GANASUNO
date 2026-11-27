# GANASUNO 34.0 — Playback recovery and English UI

## What was observed
The supplied emulator screenshots show a successful build, successful remote search, and a generic playback failure toast. This does not reveal whether the failure is in the upstream extractor, network/HTTP response, media decoding, or audio output. The previous toast incorrectly claimed that the next track was being played.

## Source changes
- Default `values/strings.xml`: translate all 19 identified Hinglish UI messages into English; keep existing localized Arabic/Hebrew resources and song/artist metadata intact.
- `data/PlaybackRecovery.kt`: testable HTTPS media URL validation, expiry checks and transient-error classification.
- `data/Streams.kt`: reject invalid/expired provider URLs; shorten URL-cache lifetime; allow available combined audio/video stream if separate audio is absent. No additional provider client or access-control workaround was introduced.
- `data/NpDownloader.kt`: log HTTP error status only. Do not log signed URLs, request headers or response content.
- `player/PlaybackCache.kt`: mark stream lookup failures for diagnostic classification.
- `player/PlaybackFailures.kt`: inspect Media3 error code, underlying HTTP status and extraction error category without exposing credentials or URLs.
- `player/PlaybackService.kt`: retry a temporary transport failure once; do not loop on access-denied or rate-limit errors. Keep queue on error.
- `MainActivity.kt`: meaningful English playback errors, explicit retry/prepare, diagnostic dialog, local import, authorized direct-file playback and official YouTube fallback. Mini and full player share retry handling.
- `VideoActivity.kt`: sanitized English video errors and Media3 playback diagnostics.
- `app/build.gradle.kts`: version 34.0 (code 38), existing app ID, Gradle and dependencies retained.
- `tests/V34PlaybackTest.kt`, `tests/test_v34_playback.py`: standalone policy and source wiring checks.

## Verify on Windows Android Studio
Open this extracted project as a new folder. Keep the application ID `com.abhi.ganasuno` and the same signing key to retain installed app data. Do not uninstall or clear data unnecessarily. Build the Privacy edition using `.\gradlew.bat :app:clean :app:assembleFossDebug`. Select `fossDebug` and Run.

1. Search for a song and try to play it. If playback fails, tap Play again to see the new error details; More options > View diagnostics.
2. Open Logcat and filter by `GANASUNO-Playback`, or run `adb logcat -s GANASUNO-Playback` using Android SDK platform-tools. Share the relevant error lines only (no credentials or signed URLs).
3. For an independent test, copy the included original `docs/playback-test.wav` into the emulator Downloads directory, then Library > Import audio and choose the file. It should play a quiet two-second test tone. This is not a music track. If local audio works but online media does not, the provider/stream path needs investigation; if local audio also fails, investigate decoder/output/player configuration.
4. Test actual audio playback, pause, next, previous, mini-player expansion, and queue persistence. If the provider denies access, use the official player or authorized audio source; this project cannot guarantee playback of every remote song.

## Tests and limits
Standalone Kotlin playback-policy checks: 25/25. Python project tests: 48/48 (including five new source-level checks). Backend regression tests: 38/38. Kotlin parser: 111 files, no syntax errors. XML parse: 44 files. These do not prove Android type checking or runtime playback. Gradle build attempted; wrapper download blocked by `UnknownHostException: services.gradle.org`. Android APK and emulator playback not verified. Existing extractor version is unchanged, because there is no verified dependency upgrade or evidence that a particular new version resolves the user's failure. The exact upstream root cause remains unconfirmed until real diagnostics are supplied. The 200-feature status count is unchanged.
