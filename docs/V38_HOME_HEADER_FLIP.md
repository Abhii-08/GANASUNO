# GANASUNO 38.0 — Home Header Flip Update

## Scope
V36 UI and playback behavior are the baseline. The compatible V37 per-item source metadata and animation logic are reused, but the named-list heading animation is removed. The exact existing `sectionTitle` TextView at the top of the Home / Listen Now landing page (formerly “Good morning”) is now the sole animated component. Search, Library, playlist and Settings headings stay static. No changes to XML, artwork, colors, background, dock or typography resources. On Home only the title uses a one-line ellipsis to keep the height stable; the existing maxLines is restored on leaving Home. Long messages may be truncated on narrow screens or with large accessibility font sizes. The original heading is plain text, not an artwork-bearing glass panel; this update does not add a new panel or optical shader.

## Behavior
- Front: current playing item's actual source name from `MediaMetadata.extras`; paused queues retain their name. Empty/legacy/unnamed queue displays Now Playing, not an inferred genre.
- Back: local-device-hour greeting (5–11 Good Morning, 12–16 Good Afternoon, 17–20 Good Evening, otherwise Good Night), alternating with the 15 English messages already supplied by the user.
- 6 s front, 750 ms eased Y-axis turn with text change at edge-on midpoint, 3 s reverse, 750 ms return. No mirrored text. Manual tap or horizontal swipe, rapid-tap guard and native haptic feedback.
- Metadata, timeline and current-item changes refresh the Home front. Selecting a category without playing it does not change the active source. Category clicks/known queries, custom playlists, named collections, mixes and shelves pass source labels when starting a queue. Unknown sources fall back to Now Playing. Per-item source is stored in the existing optional queue field; old queue JSON remains compatible. Auto-extension and manual additions carry the current queue source.
- Pause while not foreground/visible/focused or when the system disables animations, TalkBack touch exploration or Reduced glass effects are enabled. Return to Home to resume. No playback calls in the animation component.

## Changed files relative to V36
- `app/src/main/java/com/abhi/ganasuno/MainActivity.kt`: Home-only binding, queue source metadata, listeners/lifecycle.
- `.../ui/AnimatedPlaylistGreeting.kt`, `.../ui/PlaylistGreeting.kt`: reusable flip and greeting logic (new relative to V36).
- `.../ui/SearchBrowseView.kt`: exact category query/name mapping; no changes to art/rendering.
- `.../data/QueueStore.kt`: optional backward-compatible source field.
- `.../player/PlaybackService.kt`: metadata only for auto-added tracks; PCM processor untouched.
- `app/build.gradle.kts`: 38.0 / versionCode 43.
- `tests/V37PlaylistGreetingTest.kt`, `tests/test_v37_playlist_greeting.py`, `tests/test_v38_home_header.py`, `tests/test_primary_color_fix.py`, README and release notes.

## Actual checks
67 Python project checks, 38 server tests, 135 greeting/rotation JVM assertions, 22 PCM processor JVM assertions with Media3 test doubles; Kotlin PSI parser: 117 files, 0 syntax errors. Android resource files are byte-identical to V36. Syntax/static checks are not Android type checking or runtime verification. Attempted `:app:assembleFossDebug`; Gradle 8.13 download failed at `UnknownHostException: services.gradle.org`. APK, real device rendering, gestures, playback and performance have not been verified here. No claim of 60 FPS or physical spring/optical refraction.

## Device acceptance
1. Open V38 project in Android Studio with existing JDK 17 and SDK 36; sync and run `:app:clean :app:assembleFossDebug`. Keep app data.
2. Ensure full glass effects, system animator duration scale not zero, TalkBack touch exploration off for animation test.
3. Open Search → Lofi, play a song, then tap Listen Now. Top header should show Lofi, flip after 6 seconds, show greeting for 3 seconds, then return. Tap the title to flip immediately. More/profile buttons unchanged.
4. Repeat with Punjabi and a custom playlist. Old/unnamed queues should show Now Playing. Search and Library title must not flip.
5. Test navigation during flip, opening a dialog, background/foreground, reduced effects, long names, font scaling, light/dark themes, playback continuity, next/previous and restore after restarting the app.
