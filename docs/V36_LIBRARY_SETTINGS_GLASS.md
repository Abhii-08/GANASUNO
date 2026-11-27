# GANASUNO 36.0 — Library and Settings Liquid Glass

Base: GANASUNO 35.0 Search Liquid Glass. Android 8+ / Kotlin XML / JDK 17, no dependency upgrades. Package, storage keys, player source, streaming logic and search grid are unchanged.

## Changes
- `MainActivity.kt`: Library header subtitle, ambient background for Library/Settings, card renderer with existing action callbacks, glass filter input and contextual choose/ask dialog styling. Merged duplicate Liked Songs and Downloaded Music shortcuts into Favourites and Offline Audio; all distinct destinations (including Albums, Artists, Recently Played, Most Played, List/Grid, History, playlists) remain. Offline counts are saved/imported library entries, not a separate download-only metric. All 72 Settings ToolRow definitions are unchanged.
- `ui/GlassFeatureCard.kt`: reusable layered translucent material, adaptive palette, meaningful vector icons, rounded icon backgrounds, silver chevrons, accessible title/description, minimum 76dp row height, native ripple and reduced-motion-aware press feedback. Cards expand with text.
- `res/drawable/glass_feature_*.xml`: 27 original local outline vector resources (including chevron). Reference colors: rose #A43F65, blue #3E639B, purple #7452A3, teal #2C797C, gold #98743F, lavender #725CA5, ocean #326A98, grey #636870.
- `ui/SettingsDialog.kt`, `res/layout/dialog_settings.xml`, `res/values/themes.xml`: neutral radio and toggle colors, grouped glass panels, rounded dialog backgrounds; existing preference and timer callbacks untouched.
- `res/layout/activity_main.xml`: 14sp subtitle; existing circular profile, More button, floating mini-player, navigation and inset handling retained.
- `app/build.gradle.kts`: version 36.0 / code 41; `tests/test_primary_color_fix.py`, `tests/test_v36_feature_glass.py` updated/added.

## Verification
- 58 Python source/contract tests passed; 38 server tests passed.
- 22 PCM processor regression assertions passed using a JVM Media3 test double.
- 115 Kotlin source files parsed with no PSI syntax errors (not type checking).
- 93 main XML files parsed (not Android resource linking).
- All player Kotlin files and the 72 settings definitions were compared to V35 and are identical.
- Gradle `:app:assembleFossDebug` attempted; wrapper distribution download failed with UnknownHostException for services.gradle.org before compilation. No APK produced; emulator/device tests pending.

## Limits and manual QA
Cards use lightweight layered glass gradients over a shared static ambient background, not individually sampled real-time backdrop blur or optical refraction. Existing dock uses sampled backdrop blur and Android-version fallback. No new AGSL shader or collapsing header, custom bottom sheets, exhaustive submenu redesign, live artwork ambient animation, or performance benchmark. Core audio settings radios and switches and common choose/ask dialogs are styled; other legacy feature dialogs/sliders remain functional source routes without a full visual rewrite. Existing light theme preference is respected. No new fake settings added. Verify font scaling, TalkBack, dark/light switching, filters, each card destination, settings persistence, import/playlist counts, dock overlap, real playback, and performance on your device. For an upgrade, keep the existing app installed if signatures match; uninstalling or clearing data can remove library settings and local files.
