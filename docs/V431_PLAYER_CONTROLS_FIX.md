# GANASUNO 43.1 — Player controls visibility fix

Reported on 27 November 2026: Classic player dialog showed artwork, metadata, seek bar and Close, but no playback buttons. V43 source still defined the action rows, so they were not intentionally deleted. The screenshot is consistent with a constrained dialog layout; the exact on-device measurement behavior has not been reproduced here.

## Changes
- `MainActivity.kt`: separate the scrolling artwork/metadata region from the three explicitly measured action rows in the player dialog. Bound the dialog height and scale artwork against available height. Keep Previous, Play/Pause, Next, Favourite, Queue, Lyrics, Shuffle, Repeat and More immediately accessible in normal portrait mode. Compact/large-font layouts keep transport pinned and place secondary controls before artwork in scroll content. Use glass backgrounds and neutral controls, with 48dp minimum button height.
- More retains V43's advanced menu and the four Library groups are unchanged. Selected favourite stays baby pink (#F4C2C2); unselected is grey. Pause label also matches requested buffering state.
- `app/build.gradle.kts`: version 43.1, code 107.
- `tests/test_v431_player_layout.py`: five source checks; update prior version/menu-label assertions.

## Verification
103 Python source checks, 38 backend tests, 22 PCM processor JVM assertions, 118 Kotlin source syntax parses (not type checking), 100 XML parses. Gradle attempt blocked by UnknownHostException services.gradle.org before compilation. Actual Android rendering, APK and device interactions remain unverified.

## Device QA
Build `:app:assembleFossDebug` with JDK 17. Open a song from the mini-player; verify all three rows and seek bar are reachable, test play/pause, previous/next, favourite state across songs, Queue and Lyrics, Shuffle and Repeat, and More submenus. Test all four player styles, full-screen toggle, rotation, large font, and both themes. Verify Library still has Collection, Playlists, Listening, Storage. No reinstall or data clearing needed when signing key matches and installed versionCode is lower.
