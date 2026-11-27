# V39.0 – Header & Search Liquid Glass

Base: released V38.0 Header Flip Update. Two separate UI changes:

1. Home/Listen Now: the existing sectionTitle TextView receives the Library's layered glass drawable and rounded ripple while bound to AnimatedPlaylistGreeting. The entire title view including its glass background rotates together. Original title text styling, position, max-width, greeting timings, source metadata and lifecycle logic remain intact. A 10dp horizontal text inset is added; no extra vertical padding or layout XML changes. Original background, foreground and padding are restored on unbind. Both themes are supported through GlassFeatureStyle; this header uses lightweight layered material, not a new live backdrop blur or optical refraction.
2. Search landing: the seven existing Browse by type / More ways to explore shortcuts use the same GlassFeatureCard renderer as Library and Settings, with rose music, purple video, neutral search, blue globe, teal people, lavender radio and gold compass icons. Two new vector resources (video and radio); existing route callbacks, category artwork, history, search field, mini player and bottom navigation are unchanged. Existing categories remain above these rows in scroll content.

Version code 101, version name 39.0, package and signing configuration unchanged. Code 101 is chosen to exceed the suggested local version code 100 from the previous update; if an even higher version is installed, increment again before installing. Do not uninstall or clear data. Source distribution does not include an APK.

## Changed files
- app/src/main/java/com/abhi/ganasuno/MainActivity.kt
- app/src/main/java/com/abhi/ganasuno/ui/AnimatedPlaylistGreeting.kt
- app/src/main/java/com/abhi/ganasuno/ui/GlassFeatureCard.kt
- app/src/main/res/drawable/glass_feature_video.xml (new)
- app/src/main/res/drawable/glass_feature_radio.xml (new)
- app/build.gradle.kts
- tests/test_primary_color_fix.py (version expectation)
- tests/test_v39_glass_header_search.py (new)
- README.md, this document

## Validation
- 72 Python project checks passed (source contracts, not Android runtime UI tests).
- 38 backend unit tests passed.
- 135 greeting/rotation JVM assertions passed.
- 22 audio processor JVM assertions passed against test doubles.
- 117 Kotlin files parsed, zero syntax errors (not a type-check).
- 95 XML files parsed (not Android resource linking).
- All playback source, queue data source, XML layouts and existing vector artwork unchanged.
- Gradle assembleFossDebug attempted, blocked before compilation by UnknownHostException: services.gradle.org. APK and emulator behavior unverified.

## Device checks
Extract the new project, Sync, clean and assembleFossDebug, then install with the existing signing key. On Listen Now, play a category or playlist and return to Home. Verify the glass title rotates at the existing 6-second/3-second intervals, tap flip, and reduced-effects behavior. Switch to Search and scroll to Browse by type / More ways to explore; verify all seven cards open their original destinations, and Search categories remain intact. Verify Library, Settings, song playback and both themes.
