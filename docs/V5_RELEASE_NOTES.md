# GANASUNO 5.0 — development upgrade

This is a continuation of the supplied 4.0 source, not a finished 200-feature release. Android remains Kotlin/XML, package com.abhi.ganasuno, min API 26, compile/target API 36 and JDK 17. No changes to existing external streaming integration were made.

## New menu: Settings → More features (v5)
- Stereo rotation and delay: PCM16 mono/stereo audio processor installed in the Media3 music service. Rotation is stereo balance modulation, not full binaural 8D; mono can use delay but not stereo rotation. Presets: slapback, quarter-style (fixed time, not tempo-synced), dub. Configure delay 0–500 ms, feedback 0–65%, wet mix 0–45%, rotation period 3–30 s and direction. Effects are off by default; formats bypassed by the processor and standalone video player do not get these effects. Desktop local player has stereo rotation, preset delay and decoded waveform seeking.
- Preferred audio output: per-player preference for the Android-reported audio device (speaker, wired, Bluetooth, USB/HDMI as available). Android can override routing. Cast is not included.
- Theme: default, dynamic wallpaper on supported Android 12+ devices, green/orange/pink/red primary colour presets. No arbitrary custom colour picker.
- Settings export/import: share the non-secret preference JSON and paste it into the other device. Validates values before applying. Not an automated sync mechanism.
- Full audio archive: ZIP via Android document picker, includes private library metadata and app-owned offline audio. Limits: 100 MB/file, 1 GB total audio, 1000 files, 32 MB metadata. Restore stages and validates entries, rejects path traversal, duplicates and conflicting files, cleans up staged/new files if failure occurs, and replaces library metadata. No deletion of existing audio, no external document access permissions or credentials. Select a private destination; archives are not encrypted. Existing auto-backups still save metadata only.
- Developer diagnostics: SDK/build details, local errors, thumbnail memory cache clear. Private crash summary on the next launch; no automatic upload and no Sentry.
- Playlist menu: persistent title/artist sort, and song menu move up/down in the selected playlist.

## Validation performed
- Kotlin PSI syntax parsing of 37 application Kotlin files (not type checking or compilation).
- XML well-formedness check of 26 resources/manifest files.
- Standalone JVM DSP test: 8 assertions on bypass, impulse delay/feedback, and stereo rotation direction.
- Standalone JVM archive test: 9 assertions on round trip, identical file restoration, collision refusal, traversal refusal and cleanup. Uses a stub Android Context and LibraryStore; not a device/instrumentation test.
- Node syntax checks of desktop player.js and effects.js. Browser audio and UI runtime not tested.
- Archive integrity checked after packaging.

Run standalone tests from project root with Kotlin compiler and Java:

```
kotlinc app/src/main/java/com/abhi/ganasuno/player/StereoDsp.kt tests/StereoDspTest.kt -include-runtime -d dsp-test.jar
java -jar dsp-test.jar
kotlinc app/src/main/java/com/abhi/ganasuno/data/MediaArchive.kt tests/archive_stubs/*.kt tests/MediaArchiveTest.kt -include-runtime -d archive-test.jar
java -jar archive-test.jar
```

Android SDK/Gradle compilation, emulator execution, live streaming and service integrations remain unverified. Source-level statuses in FEATURE_STATUS_200.csv are not runtime guarantees. Account sync, Cast, Android Auto, native desktop, song recognition, Listen Together, and automatic DJ crossfade remain unfinished. No API keys or server credentials included. Original source licensing still needs confirmation before public distribution.

Media3 integration reference: https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer ; pinned 1.8.0 API source for BaseAudioProcessor and DefaultRenderersFactory: https://github.com/androidx/media/tree/1.8.0
