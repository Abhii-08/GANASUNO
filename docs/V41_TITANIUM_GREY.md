# GANASUNO 41.0 — Titanium Grey Edition

Base: GANASUNO 40.0. Android 8+, SDK 36, JDK 17, application ID com.abhi.ganasuno. Version code 103.

## Changes
- Both day and night `brand_primary` resources are exactly `#8E8E93`. Material text action buttons (More, Close, Cancel, Retry, Save, Apply and other theme-driven actions) inherit the new primary colour in the default theme. `colorAccent` and platform accent refer to `colorPrimary`. Filled primary buttons use dark `#161616` foreground for contrast.
- `PremiumUi.kt` shelf See all action now uses the shared primary resource rather than hardcoded red.
- The previous built-in red palette uses Titanium Grey; the stored index 5 and style IDs are retained for existing users. Other optional custom, wallpaper, green, orange and pink palette choices remain available and can override theme-driven actions if selected. Error colours, genre artwork and feature-specific icon colours are not changed. Shared primary-accent consumers, such as the active-song highlight, also inherit the new default grey.
- The shared header Settings button now uses `ic_settings_menu.xml`, a rounded three-line vector. Its separate day/night `glass_settings_field.xml` resources provide a 17dp rounded square with two gradient layers, a subtle edge, and a bounded ripple. The 48dp target and existing Settings click route are retained. Existing search-history glass surfaces remain unchanged. No new live blur on this small button.
- V40 two-row Home flip, player, backdrop dock, category assets and Library cards are unchanged.

## Modified files
- app/build.gradle.kts
- app/src/main/java/com/abhi/ganasuno/MainActivity.kt
- app/src/main/java/com/abhi/ganasuno/ui/PremiumUi.kt
- app/src/main/res/layout/activity_main.xml
- app/src/main/res/values/{colors,palettes,themes}.xml
- app/src/main/res/values-night/{colors,palettes}.xml
- New: app/src/main/res/drawable/ic_settings_menu.xml
- New: app/src/main/res/{drawable,drawable-night}/glass_settings_field.xml
- Three existing static tests updated for expected version/accent; new test_v41_titanium.py.

## Actual checks
- 82 Python project checks passed (static contracts and existing test suite).
- 38 backend tests passed.
- 22 EffectsProcessor JVM assertions passed with Media3 test stubs.
- 117 Kotlin files parsed, zero syntax errors (not Android type checking).
- 98 main-source XML files parsed (not AAPT resource linking).
- 39 protected playback, artwork and UI files byte-identical to V40.
- Android build attempted, blocked by UnknownHostException: services.gradle.org during Gradle wrapper download. No APK generated; installation, resource linking, runtime colours, contrast and playback require device QA. Exact requested grey has lower contrast on very light surfaces, especially at small text sizes; no accessibility conformance claim.

## Device QA
1. Open the extracted GANASUNO-41.0 project with the existing JDK 17 and SDK 36. Sync Gradle; run :app:clean :app:assembleFossDebug.
2. Install as an update with the same application ID/signing key. If device version code is greater than 103, increment above it rather than uninstalling.
3. Use the Default or Titanium Grey palette. Check More, Discover Close, See all, Save, Apply, Retry in both themes. Check three-line Settings icon opens the existing screen. Verify the two-row flip, Search grid, Library, playback and existing user data.
