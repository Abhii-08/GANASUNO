# GANASUNO 9.0 — Partial-feature completion batch 1

Base: 8.0. No new catalogue entries added. Ten previously partial entries received source implementations addressing their stated gaps. Status is **source-only**, not proof that the features work on an Android device. Current catalogue: 70 source-marked implemented (60 added + 10 existing), 80 partial, 50 not implemented. Coverage remains 150/200. Prior status entries are carried forward, not independently re-tested.

| ID | Changes / where to find |
|---|---|
| 33 | Settings → Advanced tools → Audio effects → Software preamp gain. Software PCM gain independent of EQ, supported mono/stereo PCM16 only. |
| 52 | Mini-player artwork/title/artist: swipe left/right to skip; downward swipe pauses and clears queue. Also Settings → Playback controls → Close mini-player and clear queue. |
| 75 | Song menu → Lyrics → More lyrics tools → Select lines for share / save image → choose background → Share PNG or Save PNG (document picker). Limit 22 lines/1,000 characters. |
| 76 | Song menu → Lyrics → More lyrics tools → Landscape fullscreen lyrics. Follows active song and timing offset; active line auto-scrolls, tap a line to seek, close restores orientation. |
| 77 | Song menu → Lyrics → Add or edit local LRC. Validate, correct errors, and save. Plain text or timed lyrics supported. |
| 94 | Home checks validated network; offline or failed search opens local tracks, with explicit offline messaging and library import guidance. File availability depends on retained permissions. |
| 109 | Library → Sort visible songs; Playlists → Save sorted order. New items record timestamps. Older records have unknown timestamps, not fabricated dates. |
| 155 | Advanced tools → any AI tool → Custom HTTPS OpenAI-compatible. Configure full endpoint, model, optional bearer key, and validated request-scoped headers. Sensitive headers are not persisted; transport overrides prohibited. |
| 159 | Listening analytics → Top albums; Song menu → Set album name for missing or incorrect metadata. Group by known album and artist, and disclose unknown listening time. Compilation albums may split by artist. |
| 182 | Settings → More features (v5–9) → Theme colour presets → Custom colour. Material content-based palette on supported Android 12+ devices; older Android uses principal playback control accent fallback. |

## Verification performed
- 48 app Kotlin sources parsed with Kotlin PSI; zero syntax errors (not Android type-check).
- 31 Android XML documents parsed for well-formedness.
- 33 compiled standalone JVM assertions in V9BatchTest; 28 additional regression assertions (V6 core 12, Insights 5, Portable/Word LRC 8, LRC parser 3).
- ZIP integrity checked.
- Android SDK and dependencies not available here: Gradle build, APK, emulator, UI, playback, live service/API and hardware testing not performed. Source-level completion remains provisional until these checks pass. The user needs to supply a trusted AI endpoint/key if needed. Existing third-party stream and licensing limitations unchanged.

## Reproduce JVM tests
From project root with Kotlin CLI installed:
```
P=app/src/main/java/com/abhi/ganasuno
kotlinc "$P/data/Song.kt" "$P/data/BatchCore.kt" "$P/data/Insights.kt" "$P/data/Fingerprint.kt" "$P/ui/WordLrc.kt" "$P/player/TenBandDsp.kt" tests/stubs/LibraryStore.kt tests/V9BatchTest.kt -include-runtime -d v9-tests.jar
java -jar v9-tests.jar
```

## Android acceptance checks still needed
Open project with JDK 17 / SDK 36, run `gradlew.bat assembleDebug`, install debug APK, and test the ten UI paths above. In particular, verify actual mini-player touch gestures, audio gain without EQ, offline network toggle, PNG export, landscape orientation and scrolling, album tag persistence, and custom colour on API 26 and API 31+. Verify a custom AI test server receives headers without redirects or credential leakage. Keep 8.0 unchanged for rollback.
