# GANASUNO 20.0 — partial-feature completion batch

12 previously partial feature areas changed. 10 are now marked as source-level implemented for the supported scope described below; two remain partial. This does not mean 10 Android features have passed device or end-to-end testing. Previous source classifications are carried forward, not re-audited.

| PDF ID | Result | Scope |
|---|---|---|
| 4 | Source implemented | 320 kbps preference + explicit custom authorized audio bitrate review |
| 48 | Source implemented | Shared timestamp comments on waveform and immediate reactions inside approved listening rooms |
| 54 | Partial | User GIF on Android 9+; no automatic provider covers |
| 72 | Source implemented | Local and persistent opt-in self-hosted lyrics voting; no upstream submission |
| 91 | Source implemented | Official public metadata home mode strict sum-of-requested-page-sizes budget; legacy search not server-limited |
| 96 | Source implemented | Last.fm similar artists with user API key; local fallback |
| 119 | Source implemented | Local WebView/browser desktop library sidebar with four groups |
| 186 | Source implemented | Arabic/Hebrew locale selection, RTL support; not complete UI translation |
| 190 | Source implemented | Next-launch local crash recovery, Full manual Sentry report; no automatic telemetry |
| 194 | Partial | Actual resolved dependency notices task requires Gradle and license audit |
| 195 | Source implemented | Opt-in bounded transient next-track cache read plus existing retry |
| 199 | Source implemented | Bundled developer journal and configured blog/support links; no supplied hosted project site |

Status: 125 source-implemented (including previous classifications), 25 partial, 50 not implemented. All 200 original rows retained.

## Setup and limitations
- Use JDK 17, Android Studio, compile SDK 36, min SDK 26. Choose fossDebug or fullDebug.
- Settings > New features (v8) contains audio quality, GIF import, related artists and layout controls. Playback controls contains next-track preloading. Enable media cache explicitly before byte buffering. Official public home mode is in Discovery settings; YouTube Data API key required each session. Its metadata opens on the official service, not through the unofficial playback source.
- More lyrics tools > Rate lyrics: local or self-hosted community vote. Run server/rooms.py with a private HTTPS reverse proxy or certificate and key. Production votes persist in --votes-db (default lyric-votes.sqlite3). Use a trusted server; votes are keyed by random tokens, not identities. This is not an abuse-resistant public rating platform.
- Last.fm official documentation: https://www.last.fm/api/show/artist.getSimilar ; use a valid API key and follow provider terms. API keys are not saved by the similar-artists tool.
- To generate the resolved dependency inventory, run `.\gradlew.bat :app:generateDependencyInventory` then rebuild. The supplied report lists declared dependencies only. The task collects notices from outer resolved archives; nested JARs, source licensing, and full redistribution compliance still require review.
- GIF import requires Android 9+, GIFs owned or authorized by the user, max 8 MiB each and 64 MiB total. GIFs are not included in existing audio/archive backups.
- Only authorized audio files may be saved; this update does not implement ad bypass or service-restricted downloads.

## Validation performed
- 79 Android Kotlin files parsed with Kotlin PSI (syntax only, no type or SDK compilation).
- 32 standalone Kotlin assertions for v20 core helpers passed.
- 37 Python backend tests passed, including persistent lyrics votes and loopback HTTP.
- 5 new desktop grouping assertions and 6 prior group assertions passed.
- XML parse of 32 documents and JS syntax checks passed.
- APK, actual emulator/device playback, Sentry live delivery, Last.fm live response, Gradle task and GIF rendering not tested (Android SDK/dependencies not present). No APK supplied.
