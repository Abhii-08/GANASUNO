# GANASUNO 35.0 — Search glass refinement

Base: GANASUNO 34.1 PCM Playback Fixed. Scope: Search visuals, shared header, mini-player and navigation styling. No new music feature claims; the V34.1 playback processor and service are unchanged.

## Changes

- `ui/GenreGlassCard.kt`: reusable lightweight layered tinted glass card, subtle gradient, specular highlight, edge, shadow, label scrim, rounded clipping and ripple feedback. Width-aware native vector artwork; 112dp base height with increased height for font scaling.
- `ui/SearchBrowseView.kt`: all 20 existing category names, colors, order and query callbacks retained; each mapped to a unique `res/drawable/genre_*.xml` original illustration. Real frequently searched cards and history callback preserved. Neutral History button.
- `res/layout/activity_main.xml`: single horizontal search row (search button, weighted text input, clear button), 48dp minimum input touch height, header spacing and circular profile button. Mini-player spacing, slim progress area and aligned floating navigation refined; binding IDs remain unchanged.
- `res/drawable{,-night}/search_field.xml`, `glass_profile_field.xml`: self-contained day/night tinted glass gradients and ripple touch surfaces; no dependency resource attributes.
- `MainActivity.kt`: neutral theme-aware search/profile icon tint; playback callbacks unchanged.
- `ui/LiquidGlassSurface.kt`: subtle reflected highlights and edge polish for existing sampled-backdrop mini-player/navigation. Keeps previous backdrop sampling, reduced-effects mode, selection animation and artwork tint.
- `app/build.gradle.kts`: versionName 35.0, versionCode 40; no dependency changes.
- `tests/test_v35_search_glass.py`: static tests for category palette/artwork mapping, card layers, search row and preserved player safety guard. Old search resource/version assertions updated to the new design/version.

## Checks performed

- 53 project Python checks passed, including 5 new UI/source contracts.
- 38 server regression tests passed.
- 22 PCM processor JVM assertions passed using Media3 test doubles (`bash tests/run_v341_effects_test.sh`).
- All 20 category vector path sets rendered using a separate SVG renderer for path sanity/visual inspection. This is not Android vector inflation testing.
- Android Gradle build attempted: `./gradlew --no-daemon :app:assembleFossDebug`; blocked by `UnknownHostException: services.gradle.org` while retrieving Gradle 8.13. APK, AAPT linking, Kotlin Android compilation, actual device rendering and playback are not verified here.

## Limitations

The new search/header/category glass is a lightweight translucent gradient and reflection treatment, not live backdrop blur or optical refraction on each tile. Artwork is static original vector illustration, not artist photographs or animated video. The dock retains the actual sampled-backdrop rendering from V34.1; device performance and light/dark appearance require testing. No Compose migration or third-party asset dependency was introduced. Android 8+ compatibility is retained in configuration.

## Device QA

1. Extract the project to a new folder, open it in Android Studio, select JDK 17 and SDK 36. Build `:app:assembleFossDebug` (Windows: `.\gradlew.bat :app:assembleFossDebug`). Install version 35.0 over the previous debug app using the same signing identity; do not uninstall if preserving local data matters.
2. On Search, verify 20 illustrated tiles, correct labels/colors and tap-to-search. Test keyboard action, clear, history and search suggestions. Check compact width and increased system font size.
3. Play a song, switch tabs and open the full player. Check previous/next/play/pause, progress, background playback, audio effects, and that the old buffer exception does not recur. Repeat with a local audio file.
4. Scroll behind the mini-player and navigation, compare light/dark and reduced-effects modes, test rotation and keyboard insets. Verify that bottom cards remain reachable.
