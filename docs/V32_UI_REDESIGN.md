# V32 – Premium native UI redesign

Base: V31.1 Resource Fixed. Original application ID, Kotlin/XML/ViewBinding, Media3 service, local library schema, API configuration, desktop and server are retained. No added dependencies. Android 8+, JDK 17, Gradle 8.13. Dark mode remains default for new installations; existing light/system theme settings are respected.

## Implemented source changes
- `MainActivity.kt`: debounced search (850 ms), local suggestion chips, clear action, stale request guard, real local home shelves and recommendation feed, library filter and list/grid mode, local artwork detail panels, player swipe-up/down and repeat/shuffle/EQ/output/timer routes, album-derived dock colors, fullscreen lyrics artwork tint.
- `ui/PremiumUi.kt`: bounded artwork shelves, action tiles, darkened sampled colors and lightweight transition with reduced-motion guard. No true backdrop blur.
- `ui/SearchBrowseView.kt`: 20 query-based categories, horizontally scrollable frequent searches from saved activity. Artwork for known saved matches only.
- `ui/ImageLoader.kt`: optional bitmap callback; existing bounded HTTPS cache retained.
- `ui/SongAdapter.kt`: grid view type with artwork above metadata.
- `res/layout/activity_main.xml`: clear button, suggestions row, larger playback targets, marquee, profile/settings icon.
- `res/drawable/ic_close.xml`, `ic_profile.xml`, night search drawable and colors: resources and updated palette.
- `app/build.gradle.kts`: version 32.0 / code 36.
- Updated previous static tests for deliberate design/version changes; seven new source-contract tests.

## Brief coverage (not runtime certification)
1–2: AMOLED palette and artwork color extraction. Real backdrop glass blur not implemented; lightweight gradient fallback.
3–5: Search suggestions are based on local history, saved tracks and quick categories, not remote trending query analytics. Category tiles launch queries, not a complete separately curated genre catalogue. Public video/channel/playlist and MusicBrainz catalogue routes retained. Frequent artwork is limited to matches in saved music.
6–7: Persistent dock and 3 tabs; real player callbacks, progress, gesture routes and sampled gradients. No live glass blur.
8: Real local history, picks, personalized mixes, top/favourite tracks, remote search-based recommendations. Daily mixes, moods, artists and album discovery use existing action routes; not all are dedicated home carousels. Trending album route is catalogue browsing, not a verified trending album feed.
9–10: Existing player styles and waveform retained; added direct controls and larger art, swipe down on cover, fullscreen lyrics. No exact Apple Music shared-element animation; timed lyrics require source data.
11–12: Library search/filter, local grid/list, local detail panels with artwork and play/shuffle/management. Remote detail screens retain earlier implementation. Collapsing headers and dedicated download button on every collection not implemented.
13–17: Ripple feedback, color transitions, animation preference guard, scaled text and adaptive widths. Comprehensive TalkBack, 60 FPS benchmarks, foldable/tablet layouts and memory profiling not verified. No skeleton animation or fully shared-element navigation.
18–19: Existing Kotlin XML architecture kept deliberately to reduce regressions. Not migrated to Compose, Coil, Navigation Compose or ViewModel. Playback and service files unchanged. Static callback checks do not establish end-to-end functionality.
20: Source-only checks: 106 Android Kotlin files parsed with no PSI errors, 37 Python project checks and 38 backend regression tests passed. Gradle build attempted, blocked by UnknownHostException services.gradle.org while fetching distribution. No APK produced; AAPT2, Kotlin type checking against Android libraries, runtime playback, UI appearance, multi-size and live API tests remain unverified.

Previous 145/23/32 source classification is retained, not independently recertified. No new 200-feature completion count claimed. See FEATURE_STATUS_200.csv for earlier status and V30_UI_ROUTE_AUDIT.csv for historical routes.

## Run on Windows
Extract into a new folder, open the project root containing settings.gradle.kts, select JDK 17, sync, then run ` .\\gradlew.bat :app:assembleFossDebug ` in PowerShell (without surrounding spaces). Select fossDebug and Pixel 4. Check version 32.0. For existing installations use the same signing key to retain data; do not uninstall just to update.
