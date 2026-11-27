# V44.0 — Grouped Settings

Base: V43.1 Player Controls Fixed. Version code 108; JDK and dependency versions unchanged.

## Navigation
Settings initially displays seven Liquid Glass cards using the same renderer as Library. Open a card to see its related settings; use Back to Settings or Android Back to return. The group is retained on activity recreation and cleared when leaving Settings. Root search queries all original settings; category search queries only that category. Empty search restores the cards. No additional settings or duplicated top-level controls were created.

| Category | Original actions |
|---|---:|
| Appearance & player (Appearance, Player & appearance, Lyrics) | 10 |
| Playback | 9 |
| Sound & equalizer | 6 |
| Discovery & history (Discovery, Listening insights) | 7 |
| Storage | 4 |
| Integrations | 6 |
| System & privacy | 11 |

All 53 ToolRow action definitions are byte-for-byte unchanged from V43.1. Existing contextual shortcuts elsewhere are intentionally untouched; this is a Settings navigation change, not a new whole-app deduplication pass. Original Library grouping, player controls, favourites, user-data stores, and playback engine preserved. Seven group icons reuse existing native vectors and layered glass material, not live per-card blur.

## Modified files
- MainActivity.kt: settings group navigation, search candidates, back and saved state; existing action catalogue extracted.
- GlassFeatureCard.kt: group-specific icon mappings.
- app/build.gradle.kts: version 44.0 / 108.
- tests: existing version and Settings catalogue assertions updated; new grouped Settings source checks.
- README and this report.

## Verification
108 Python source checks passed; 38 backend tests passed. Kotlin PSI parser: 118 files, 0 syntax errors (not semantic compilation). 100 main XML files parsed. APK build attempted with Gradle wrapper, blocked by UnknownHostException services.gradle.org before compilation. Android device navigation, layout, search and playback must be verified manually.

Device QA: Open Settings in light/dark theme. Check seven category cards. Open each, exercise each original action, tap Back to Settings, try Android Back and rotate the device. Search `crossfade` from root; clear search and confirm seven cards return. Check a nonmatching query. Play music and switch to Settings and a category; verify playback and mini-player remain active. Verify V43.1 player controls and four Library cards still work. Keep existing app data; install as an update with the same signing key.
