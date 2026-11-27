# GANASUNO 45.0 — Smart Player & Categorized Settings

Seven navigable settings groups, shared transport handling, glass player controls, direct 8D stereo rotation, software equalizer and sleep timer. See [changes and device QA](docs/V45_SMART_PLAYER.md).

---

# GANASUNO 42.0 — Simplified Menus

Consolidated Library, Search and Settings entry points. [Changes and QA](docs/V42_SIMPLIFIED_MENUS.md).

---

# GANASUNO 41.1 — Smart Loading Fix

Playback-intent-aware mini-player spinner and subtly spaced playback buttons. [Changes and device QA](docs/V411_SMART_LOADING.md).

---

# GANASUNO 41.0 — Titanium Grey Edition

Neutral #8E8E93 action accent and three-line Liquid Glass Settings button. [Changes and device QA](docs/V41_TITANIUM_GREY.md).

---

# GANASUNO 40.0 — Two-Row Liquid Glass Header

Both Home labels now share a taller glass card that flips as a unit. See [V40 implementation and device QA](docs/V40_TWO_ROW_HEADER.md).

---

# GANASUNO 38.0 — Home Header Flip Update

The original Listen Now heading is now the only animated title. Named queue metadata is retained from V37; Search and Library titles use V36 styling and behavior. [Implementation and device QA](docs/V38_HOME_HEADER_FLIP.md).

---

# GANASUNO 36.0 — Library and Settings Liquid Glass

V36: Library and Settings glass cards with reference-matched feature icons. See [V36 implementation and QA notes](docs/V36_LIBRARY_SETTINGS_GLASS.md).

20 illustrated glass category cards, a one-row search field, refined header and floating dock. Existing music engine and V34.1 PCM fix preserved. Open with Android Studio, JDK 17, SDK 36; build `:app:assembleFossDebug`. See [changes, tests and limitations](docs/V35_SEARCH_GLASS.md).

---

# GANASUNO 34.1 — PCM playback runtime hotfix

Open the existing project in Android Studio with JDK 17 and SDK 36. Build `:app:assembleFossDebug` or `:app:assembleFullDebug`. Default theme remains AMOLED dark; the existing light theme is supported. Settings → Appearance → Liquid Glass effects toggles the reduced-effects fallback.

[Implementation, tests and limitations](docs/V33_LIQUID_GLASS.md) · [200-feature status](docs/FEATURE_STATUS_200.csv).

**Build status:** Gradle download failed with `UnknownHostException: services.gradle.org`. The Android APK and device rendering have not been verified. This is an Android recreation, not Apple's native framework.

---

# GANASUNO 31.0 — reference-inspired UI

[Release notes and device acceptance](docs/V30_RELEASE_NOTES.md) · [All 200 UI routes](docs/V30_UI_ROUTE_AUDIT.csv) · [Source feature statuses](docs/FEATURE_STATUS_200.csv).

Open the existing Android Studio project, JDK 17 and SDK 36. Five navigation destinations, searchable Settings and direct song actions. Build attempt blocked by Gradle download; APK/device testing pending.

---

# GANASUNO 29.0 — Android transitions

[Latest release notes](docs/V29_RELEASE_NOTES.md) · [200-feature report](docs/FEATURE_STATUS_200.csv). Three Android source implementations for previously partial features; 145 source-implemented/existing, 23 partial, 32 missing. APK and audio handoff unverified.

Open with Android Studio, JDK 17 and SDK 36. Settings → Playback controls → Crossfade and DJ transitions (experimental, off by default).

---

# GANASUNO 28.0 — Offline automation and release alerts

[Latest release notes](docs/V28_RELEASE_NOTES.md) · [200-feature report](docs/FEATURE_STATUS_200.csv). Seven new areas: five added source implementations, two partial. 142 source-implemented/existing, 26 partial, 32 missing. APK not verified.

Open this existing project with Android Studio, JDK 17 and SDK 36. Settings → More features → Batch 28. See release notes for limitations and device acceptance tests.

---

# GANASUNO 27.0 — partial-feature follow-up

[Latest release notes](docs/V27_RELEASE_NOTES.md) · [200-feature report](docs/FEATURE_STATUS_200.csv). Six partial areas improved; 137 source-complete, 24 partial, 39 missing. APK not verified.

---

# GANASUNO 15.0 — Batch 6 start

[Release notes](docs/V15_RELEASE_NOTES.md) · [200-feature status](docs/FEATURE_STATUS_200.csv). Nine partial entries improved, two promoted to source implementation; 106 implemented, 44 partial, 50 missing. No Android APK build verified. Open the existing project with JDK 17 and SDK 36. For playlist sharing, update and host the included HTTPS server.

---

# GANASUNO 14.0 — Batch 4 follow-up + Batch 5

See [release notes](docs/V14_RELEASE_NOTES.md) and [200-feature matrix](docs/FEATURE_STATUS_200.csv). 104 source-complete, 46 partial, 50 not implemented; APK not verified. Open the existing project with JDK 17 and API 36. New tools: Settings → More features → Batch 4–5 tools.

---

# GANASUNO 13.0 — batch 4

Latest release: [Batch 4 notes](docs/V13_RELEASE_NOTES.md). Ten partial entries upgraded; five promoted to source-complete, five remain partial. Carried-forward catalogue: 95 source-complete, 55 partial, 50 not implemented. Android build/runtime unverified.

---

# GANASUNO 12.0 — batch 3 follow-up

Latest: [Batch 3 follow-up release notes](docs/V12_RELEASE_NOTES.md). Two additional source-level completions (software reverb and progressive karaoke); APK not verified.

Open the existing Android Studio project with JDK 17 and SDK 36. See [release notes](docs/V10_RELEASE_NOTES.md) and [200-feature status](docs/FEATURE_STATUS_200.csv). Android APK build and runtime remain unverified. Update the room server in `server/` along with this app.

---

# GANASUNO 6.0

Open this existing Android Studio project. See [v6 release notes](docs/V6_RELEASE_NOTES.md) and [200-feature status](docs/FEATURE_STATUS_200.csv). New controls: Settings → More features (v6). Development build; APK not verified.

---

# GANASUNO 4.0 — development preview

See [V4 release notes](docs/V4_RELEASE_NOTES.md) and [all 200 feature statuses](docs/FEATURE_STATUS_200.csv). Android Studio project, JDK 17, API 26 minimum, compile SDK 36. Open the existing project, do not create an Empty Activity. Desktop offline companion is in `desktop/`.

---

# Earlier setup notes (retained from GANASUNO 3.0)

Incremental upgrade, not a complete 200-feature release. Read [v3 notes](docs/V3_RELEASE_NOTES.md) and [200-feature matrix](docs/FEATURE_STATUS_200.csv). APK build not verified.

# ganasuno — Android Studio edition

Native Android music player migrated from the supplied ganasuno project. Existing Kotlin/XML screens, Media3 playback and streaming code are preserved; no Compose rewrite was made.

## Configuration
- Name: ganasuno
- Application ID and namespace: `com.abhi.ganasuno`
- Minimum SDK: API 26 / Android 8.0
- Compile and target SDK: API 36
- Java/Kotlin JVM target: 17
- Gradle: 8.13; Android Gradle Plugin: 8.13.2; Kotlin: 2.3.21
- Android Studio: Narwhal 3 Feature Drop (2025.1.3) or a newer version supporting AGP 8.13.

## Open on Windows
1. Cancel the New Project dialog; do not create an Empty Activity project.
2. Extract this ZIP into `D:\AndroidProjects` (or another path without spaces). The extracted `ganasuno` folder is the project root.
3. Android Studio > Open > select that `ganasuno` folder containing `settings.gradle.kts`.
4. Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK: select installed JDK 17 (or download JDK 17). Use the Gradle wrapper.
5. Tools > SDK Manager > SDK Platforms: install Android 16 / API 36. In SDK Tools install Android SDK Build-Tools 35.0.0 and Android SDK Platform-Tools (install any additional requested versions when prompted).
6. Keep internet enabled, disable Gradle offline mode, and sync the project. First sync downloads Gradle and libraries, including the JitPack extractor.
7. Select the `app` run configuration and your USB-debugging-enabled Android phone (or emulator), then Run.

## Build APK (Windows PowerShell)
Open a terminal at the project root, with JAVA_HOME pointing to JDK 17 and Android SDK configured by Android Studio.

```powershell
.\gradlew.bat assembleFossDebug --stacktrace
```

Output: `app/build/outputs/apk/foss/debug/app-foss-debug.apk` after a successful build.

GitHub Actions debug build workflow is retained. Incompatible upstream multi-module workflows and phone/Gradle caches are omitted. No signing credentials or machine-specific `local.properties` are bundled.

## Existing functionality
Music search, live stations, streaming through BravePipeExtractor, Media3 playback and media notifications, queue storage, smart shuffle, sleep timer, audio and video settings, video playback, subtitles, silence skipping and fixed loudness boost. This paragraph describes the original baseline; see v3 release notes for current additions.

## Notes and limitations
The new application ID installs as a separate app from `com.mix.ganasuno`; previous app data will not automatically transfer. Existing music APIs and stream extraction are unofficial and may fail if the upstream service changes. Use playback only where authorized.

Static migration checks were performed; a full Android build and on-device playback test could not be completed in the preparation environment because Android SDK and network access were unavailable. If sync or build fails, collect the first actual error from the Build window or terminal. The retained wrapper bootstrap JAR comes from the original source archive; its distribution URL is now pinned to 8.13.

Original project notes, including historical phone-build configuration and SimpMusic attribution, are in `docs/ORIGINAL_PROJECT_NOTES.md`.

## Version 6.0 additions
See [V6 release notes](docs/V6_RELEASE_NOTES.md). Android software ten-band EQ, GraphicEQ import, local decoded waveform/tempo/key analysis, private timestamp reactions, listening fingerprint, decade analytics, local AI translation ratings, and desktop DJ filter sweeps. The feature status CSV distinguishes source additions and partial coverage; Android APK and live services are still unverified.

## Version 7.0 additions
See [V7 release notes](docs/V7_RELEASE_NOTES.md) and [room server setup](server/README.md). Self-hosted listening rooms, host controls and suggestions, dedicated insights widget, local playlist details and browser desktop saved playlists. Android build and runtime remain unverified. Room sync needs your own trusted HTTPS endpoint.

## Version 8.0 additions
Settings → **New features (v8)**: 320 kbps preference (source-dependent), manual explicit filter, artwork queue, rotating cover, curated theme playlists, local related artists, saved collection links, RTL preview and upstream support links. Launcher turntable widget and desktop album-continuity preference included. See `docs/V8_RELEASE_NOTES.md` and `docs/FEATURE_STATUS_200.csv` for exact scope: 150/200 full-or-partial source coverage, not a fully tested release.

## Version 9.0 — completion batch 1
Ten previously partial catalogue entries upgraded: 33, 52, 75, 76, 77, 94, 109, 155, 159, 182. Source status: 70 implemented, 80 partial, 50 missing; Android build/runtime verification pending. See `docs/V9_RELEASE_NOTES.md` for usage, limitations, tests and acceptance checklist. No new catalogue entries counted.

## Version 16.0 — next partial-feature batch
See [V16 release notes](docs/V16_RELEASE_NOTES.md). Official public music catalogue, metadata, region-based popular videos, offline public playlist snapshots, and scheduled update metadata checks. Bring your own API key and trusted update manifest. The catalogue opens official YouTube links; it does not resolve streams. 109 source-implemented / 41 partial / 50 missing, carried-forward source matrix. APK build and device runtime are not verified.

## Version 17.0 — partial-feature batch
See [V17 release notes](docs/V17_RELEASE_NOTES.md). Seed-based radio, shuffle-aware artwork queue, room comments, desktop grouping, public playlist discovery and local related-artist improvements. Source matrix: 111 implemented, 39 partial, 50 missing. APK compilation and live integration testing remain unverified.

## Version 18.0 — queue, favourite collections, rooms and desktop window
See `docs/V18_RELEASE_NOTES.md` and `docs/FEATURE_STATUS_200.csv`. Android Studio still uses JDK 17 and compile SDK 36. To test Android, run `gradlew.bat assembleFossDebug --stacktrace`, then launch on your device. The optional desktop window requires `python -m pip install -r desktop/requirements.txt` and `python desktop/launcher.py`. Original browser companion remains available. No APK, device or actual desktop GUI build verification was possible in this environment.

## Version 19.0 — privacy editions and track analysis
Android Studio Build Variants: `fossDebug` (default recommendation, no crash upload transport) or `fullDebug` (manual crash reporting to a user-supplied Sentry DSN). Both have the original application ID; install one at a time, not side-by-side. Build using JDK 17 and SDK 36: `gradlew.bat assembleFossDebug assembleFullDebug --stacktrace`. Outputs: `app/build/outputs/apk/foss/debug/app-foss-debug.apk` and `app/build/outputs/apk/full/debug/app-full-debug.apk`. No DSN is bundled or persisted. Report submission is a manually confirmed envelope, not an automatic Sentry SDK. See docs/V19_RELEASE_NOTES.md. Source licensing remains unverified.

## Version 20.0 — partial feature completion batch
See [v20 release notes](docs/V20_RELEASE_NOTES.md) for exact scope and tests. Includes anonymous community lyric voting (self-hosted), animated GIF artwork import, bounded official metadata home, Last.fm related artists, RTL language selection, startup crash report, desktop sidebar and bounded optional playback prebuffer. Dependency inventory generation is optional and not a license compliance guarantee. Android build/runtime not verified here. The two build variants remain `assembleFossDebug` and `assembleFullDebug`.

## Version 21.0 — catalogue and artwork upgrade

Settings > More features (v5–21) > MusicBrainz · songs, artists and albums contains a new multi-category metadata browser. See `docs/V21_RELEASE_NOTES.md` for scope, maintainer contact requirements, testing and remaining limitations. Version 21 improves 9 prior partial areas; 3 are source-implemented for the documented scope. The requested 20-feature completion target was not reached. Android build and live API checks remain unverified.

## Version 22.0 — catalogue, accessibility and native desktop

Settings > More features (v5–22) > Batch 22. See `docs/V22_RELEASE_NOTES.md` for all 12 upgrades and limitations. Three advance to source implementation for the documented scope; nine remain partial. The optional native local-file Qt player is documented in `desktop/README.md`. Android build remains unverified.

## Version 23.0 — seven partial feature upgrades
See [V23 release notes](docs/V23_RELEASE_NOTES.md). Native Qt local crossfade, album continuity, persistent desktop queue, Android offline integrity audit, saved curators, manually linked artist channels and multiple BPM candidates. All seven catalogue entries remain partial against their original full scope. Source matrix unchanged: 131 implemented, 19 partial, 50 missing. APK and native GUI are not runtime verified.

## Version 24.0 — integrity, room timing and desktop resume
See `docs/V24_RELEASE_NOTES.md` for changes, tests and limitations. New audio backups preserve checksums, room sync estimates recent network delays, and native desktop saves playback position and preferences. The Android build, desktop runtime and optional packaging workflow have not been verified.

## Version 25.0 — Community tools and regional discovery
Settings → More features → Batch 25 · community and charts: configurable playback cache, opt-in SponsorBlock and nine-category selection, Return YouTube Dislike estimates, read-only public comments/replies and regional popular videos. See `docs/V25_RELEASE_NOTES.md` for usage, privacy, limitations and tests. Android and external services have not been runtime verified.

## Version 26.0 — Account and blog tools

Manual private YouTube playlist upload/append and unlink, official account video ratings and liked-video browsing, opt-in RSS/Atom blog notifications. See `docs/V26_RELEASE_NOTES.md` and `docs/FEATURE_STATUS_200.csv` for scope and verification. Requires user-supplied OAuth token for account operations. Android build and live services are not verified.


## V30.3 compilation hotfix
See `docs/V30.3_HOTFIX.md` for changes, checks and setup.

## V31 reference-inspired UI
See `docs/V31_REFERENCE_UI.md` for the changed navigation, search page, theme and validation limits.

## V32 UI update
See [UI redesign and verification limits](docs/V32_UI_REDESIGN.md).

### V37.0 — Playlist title flip
See [V37 implementation and QA](docs/V37_PLAYLIST_GREETING.md) for the scoped animation, metadata persistence, and known device-test limitations.

## V39.0 – Header & Search Liquid Glass
Home flip title now uses layered glass, and Search shortcuts reuse Library glass cards. See `docs/V39_HEADER_SEARCH_GLASS.md` for changes and verification limitations.

## V43.0 — Grouped Library & Player
Four Library groups, compact player controls and state-aware baby-pink favourites. See [V43 notes](docs/V43_GROUPED_LIBRARY_PLAYER.md).

## 44.0 — Smart Settings Indicators
See [implementation and verification](docs/V44_SMART_SETTINGS.md).
