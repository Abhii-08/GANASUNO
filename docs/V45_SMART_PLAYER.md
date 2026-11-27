# GANASUNO 45.0 — Smart Player & Categorized Settings

Base: GANASUNO 44.0 Smart Settings Indicators. Version code 111; Android 8+; JDK 17; unchanged build dependencies and app ID.

## Changes

- Settings root contains seven navigable category cards rather than a flat list. All 53 existing settings rows retain their actions; Audio output is grouped with Sound & Effects. Global search includes every row, and category search is scoped to that category. Back navigates to the root. Category restored on activity recreation. Existing saved-state indicators remain connected.
- Mini and full player transport, artwork swipes and mini-player gesture/long-press actions use the same skip handler. Available previous/next tracks are checked before seeking. A skip starts playback and prepares the player when idle, ended or errored; 350 ms guard prevents immediate repeated skip requests. Play resumes or restarts an ended item; errors keep the existing diagnostic/retry dialog. Both player surfaces share the same buffering/pause display policy.
- Existing opt-in endless mix can resume at the first newly appended track if the original playback was still requested when its queue ended. No fake tracks are generated. If there is no next/previous item, manual skip reports the queue boundary; it does not promise a new remote recommendation. Existing normal Media3 auto-advance remains in place.
- Now Playing uses rounded layered glass controls and vector transport icons; the album cover is sized to leave more room on compact screens. The scrollable dialog and existing player modes remain. Quick cards connect to actual 8D stereo rotation preference, existing 10-band software EQ, and existing SleepTimer (including countdown, stop at end, cancel). 8D and EQ status indicate saved preferences; audible effect depends on compatible PCM playback. No simulated hardware status.
- Player More menu no longer repeats direct software EQ, timer or 8D on/off shortcuts; device EQ and rotation/delay parameters remain. Library groups, Baby Pink heart, header flip and Titanium Grey theme retained. No data migration or dependency upgrade.

## Changed production files

- `app/build.gradle.kts`
- `app/src/main/java/com/abhi/ganasuno/MainActivity.kt`
- `app/src/main/java/com/abhi/ganasuno/player/TransportPolicy.kt` (new)
- `app/src/main/java/com/abhi/ganasuno/player/PlaybackService.kt`
- `app/src/main/java/com/abhi/ganasuno/ui/GlassFeatureCard.kt`
- `app/src/main/java/com/abhi/ganasuno/ui/SettingsDialog.kt`
- `app/src/main/res/layout/dialog_settings.xml`
- README, tests and this report.

## Checks performed

113 Python source/regression checks; 38 backend unit tests; 71 pure Kotlin transport policy assertions; 22 EffectsProcessor JVM assertions with Media3 test doubles; 120 Kotlin files parsed without syntax errors; 100 XML files parsed. Source checks are not instrumentation or type-checking of Android APIs. Gradle assembly attempted; download failed with UnknownHostException: services.gradle.org before Android compilation. No APK or device verification is claimed.

## Device QA

1. Build `:app:assembleFossDebug`, update existing install (same signing key), do not clear data. Confirm version 45.0.
2. Open Settings; verify seven cards, each submenu and system Back; search for crossfade and rotation from root; change a saved setting and confirm status updates.
3. Play a queue of at least three tracks. Test both surfaces for Play, Pause, Next and Previous while paused, playing and buffering; test rapid skips and queue boundaries. Verify artwork, position and title. Test replay after end; test optional endless mix with valid returned tracks. Repeat with local audio to distinguish network failures.
4. Tap 8D and EQ, test with stereo PCM audio; test timer Off, duration, end-of-song. Check favourite persistence, shuffle/repeat, lyrics, queue and More options. Test light/dark, compact screens and app background playback.
5. If skip or play still fails, capture `GANASUNO-Playback` and `ExoPlayerImplInternal` Logcat. Exact runtime cause of user's remaining bug was not provided; source-level issues addressed, runtime outcome still unverified.
