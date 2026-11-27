# GANASUNO 3.0 — incremental Android upgrade

This is not a completed 200-feature app. All original v2 code is retained or extended; old ZIP unchanged. Source-only status totals: {'Partial': 44, 'Existing source': 10, 'Not implemented': 112, 'Added source': 34}. No Android build or device runtime validation is available.

## Where to find the new functionality
- Discover chip: mood and genre shortcuts, keyword-based new releases, podcasts, trending videos and listening-based artist mix. These are searches, not official feeds.
- Library > Offline storage: import and copy local audio, save authorized direct HTTPS audio files (100 MB per file; no redirects or streaming-service downloads), inspect app-owned storage and clear thumbnail memory. Removing app-owned copies can invalidate references in existing queues/playlists and backups; re-import if necessary.
- Library > Automatic backups: private metadata snapshots using Android JobScheduler, with retention. Daily/weekly/30-day jobs may be delayed by Android. Manual snapshot and restore available. Audio bytes and external URI permissions are not backed up. Uninstalling removes app-private snapshots.
- Library > Bulk actions: multi-select visible songs and add them to favourites, playlists or queue.
- Listening history: local hour clock, busiest day/hour, previous-period comparison, yearly/custom dates, monthly recap playlists and text sharing. Based on sampled playback duration, not exact play counts. Album data unavailable.
- Playback controls: optional artist-mix continuation, sleep-timer fade duration and optional next-stream URL prefetch. A bounded fresh URL retry is attempted on eligible playback errors.
- Equalizer: hardware-supported presets and custom band levels. Actual number of bands depends on Android device.
- Settings: local search suggestions, storage/backups and local in-memory diagnostics (error codes only).
- Launcher: static library/history shortcuts and a periodic combined playlist/listening widget.

## Still pending
See FEATURE_STATUS_200.csv, including full crossfade/AutoMix/DJ DSP, 8D audio, Cast, official account integrations, AI translation, recognition, provider charts, full player styles, authenticated comments, synchronized rooms, desktop apps, Android Auto, and full backup/sync. No API credentials, external servers or third-party accounts are bundled. No claim of ad-free/premium availability or service authorization. Original source attribution is retained; a redistribution license must be verified with the original rights holders before public release.

## Verification
- Parsed all 25 Android Kotlin source files using the Kotlin compiler PSI: zero syntax errors (not type checking).
- Insights test: 5 checks passed, using a test-only model stub.
- LRC parser test: 3 checks passed.
- XML well-formedness and ZIP integrity checked.
- Android SDK, Gradle dependency resolution, APK compilation, emulator, network APIs and audio effects were not tested here. Run `gradlew.bat assembleDebug --stacktrace` on your PC.
