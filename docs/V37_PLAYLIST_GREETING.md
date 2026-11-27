> Historical V37 release notes. The named-track-list animation described below has been superseded by the Home-only implementation in [V38](V38_HOME_HEADER_FLIP.md).

# V37 — Animated playlist greeting

## Scope and target selection
The V36 source has a static Lofi genre card and a quick-search chip, not a dedicated currently-playing playlist glass widget. The target is therefore the existing `sectionTitle` view on named track-list screens (playlist, collection, category results). No card artwork, colour, background, XML layout or navigation components have been redesigned. The existing title is not itself an artwork-bearing glass card. A full-card flip of a different component would require identifying that component from an actual screen reference.

The heading remains the browsed list name if it differs from the active queue source; it must not imply that another playlist is playing. While the corresponding queue is active, the title flips. Legacy/unnamed queues fall back to Now Playing on named lists containing the current track. Generic Search and unrelated screens remain unchanged. No fake Lofi state is introduced.

## Implementation
- `ui/AnimatedPlaylistGreeting.kt`: native hardware-accelerated Y rotation on existing title, 750 ms eased half-turn with a face swap at the edge, 6 s front / 3 s reverse hold, tap/swipe, native haptic feedback, no simultaneous flips, lifecycle and visibility gates, system animation and TalkBack/reduced-effects checks, original measured height and typography retained, restored layout when unbound.
- `ui/PlaylistGreeting.kt`: English local-hour greetings, alternating short messages, source fallback, timing and rotation math.
- `MainActivity.kt`: attach display to actual current item metadata, category/playlist/collection source tracking on queue creation, metadata and timeline listeners, lifecycle integration. Collection names and user-created playlist names are not hardcoded. Existing queue start, prepare and play calls remain in place; animation has no player calls.
- `ui/SearchBrowseView.kt`: category query-to-name lookup only, original art/rendering untouched.
- `data/QueueStore.kt`: optional per-track playback source serialized alongside existing fields, old JSON remains readable.
- `player/PlaybackService.kt`: carry source metadata to auto-extended tracks; no audio processing changes.
- `app/build.gradle.kts`: version 37.0 / 42. Two new tests and one updated version check.

## Verification
- 135 JVM greeting/angle/timing assertions; 22 PCM regression assertions with test doubles.
- 62 Python project checks (static source/desktop contracts); 38 backend tests.
- Kotlin PSI parsing: 117 source files, no syntax errors. Not equivalent to Android type checking.
- All original XML layouts, drawable resources and audio effects processor remain identical to V36.
- Build attempt blocked at Gradle 8.13 download (`UnknownHostException: services.gradle.org`). Android compilation, emulator gestures, rendering, actual streaming playback and 60 FPS not verified. No APK produced.
- Native optical refraction, moving specular shader and physical spring are not implemented on the title; this is an eased perspective rotation, not a new material. Long text is ellipsized within the original title's measured line budget.

## Device QA
Open a custom playlist or a named Search category (e.g. Lofi). Play a track. Wait 6 s: heading rotates to local-time greeting, remains 3 s, rotates back. Tap/swipe heading; rapidly tap and verify no overlapping animations. Switch playlist and play; check real source name. Navigate away or open a dialog; verify the flip pauses and playback continues. Enable reduced effects or system animator duration scale 0: source stays visible. Test long names and light/dark themes, restart app without clearing data, reopen the active playlist and check source restored. Verify playback, next/previous, background and lock-screen playback on device.
