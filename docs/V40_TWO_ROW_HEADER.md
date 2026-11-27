# GANASUNO 40.0 – Two-Row Liquid Glass Header

Base: V39.0. Approved design: one taller glass card contains the playback source and the existing `Listen Now · GANASUNO` subtitle on both faces. Only Home / Listen Now landing binds the card. The existing More and profile buttons remain outside the rotating area.

## Implementation
- Added an ID to the existing weighted vertical header container; no reparenting or new nested layout. Both existing text views remain in that container.
- Applied shared GlassFeatureStyle material and ripple to the parent, with a 24dp radius, 94dp minimum height and 12dp horizontal / 10dp vertical padding. The card can grow with system font size; subtitle wraps on very narrow screens.
- Native title auto-sizing (16–28sp, one line with existing ellipsis), with a stable font-scaled title row; secondary text 12sp with adaptive silver/dark grey.
- The parent rotates on its Y axis, with the title switching at the edge-on midpoint. Subtitle remains the same on both faces. Existing 750ms/6s/3s timing, tap/swipe, lifecycle, current queue source, greetings and reduced-motion behavior preserved.
- On leaving Home, restore original parent minimum height, gravity, padding, drawables, click/focus state, camera and label size/layout/visibility/accessibility properties. Other screens use their original headings.
- No changes to playback processor, queue persistence, artwork, search cards, library, settings or dock. Version code 102, version name 40.0. Same application ID/signing setup. If installed code exceeds 102, increase code before installing, rather than uninstalling.

## Changed files
- app/src/main/res/layout/activity_main.xml (one container ID)
- app/src/main/java/com/abhi/ganasuno/MainActivity.kt (constructor wiring)
- app/src/main/java/com/abhi/ganasuno/ui/AnimatedPlaylistGreeting.kt (two-row parent animation and restoration)
- app/build.gradle.kts (version)
- tests/test_primary_color_fix.py, tests/test_v37_playlist_greeting.py, tests/test_v38_home_header.py, tests/test_v39_glass_header_search.py (updated contracts)
- tests/test_v40_two_row_header.py (five new source/layout checks)
- README.md, this document

## Validation
- 77 Python project checks passed; source contracts, not on-device instrumentation.
- 38 server unit tests passed.
- 135 greeting and rotation JVM assertions passed.
- 22 EffectsProcessor JVM assertions passed with test doubles.
- 117 Kotlin files parsed, zero syntax errors; not a type-check.
- 95 XML files parsed; not Android resource linking.
- Gradle assembleFossDebug attempted, blocked before compilation by UnknownHostException: services.gradle.org. No APK generated. Device appearance, actual text fit, animation and playback remain to be verified on Android.
- Shared layered glass drawable, not a new live backdrop blur or native Apple framework.

## Device QA
Install over existing app with the same signing key. Keep existing data. On Home, check both text lines, 6s source/3s greeting cycle, manual tap and swipe, long names, light/dark, reduced effects and enlarged system fonts. Switch to Search/Library/Settings and ensure header reverts to original layout. Check ongoing playback across navigation. Confirm More/profile remain independent buttons.
