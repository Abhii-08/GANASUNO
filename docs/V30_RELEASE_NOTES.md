# GANASUNO 30.0 — UI navigation and integration pass

## What changed
- Five persistent navigation destinations: Home, Search, Library, Player (full-screen dialog), Settings. Home retains existing search-backed feed and offline fallback. Search has a visible submit action; Library has visible collection rows, playlist names and track counts. Bottom navigation remains outside the mini-player.
- Settings now has 70 searchable, grouped source routes instead of requiring the user to navigate through release-number menus. Existing dialog actions are reused; `All original tools` retains a fallback. Some nested feature dialogs still exist. No backend or API credentials were invented.
- Song rows have an explicit 3-dot button wired to the same per-song action handler as long-press. Now Playing exposes artwork queue, lyrics, waveform, effects, style and song actions; artwork horizontal swipe changes tracks. Selection, return navigation, stale search result cancellation and duplicate startup crash prompt were improved.
- `V30_UI_ROUTE_AUDIT.csv` maps all 200 requested entries to source navigation or OS/desktop/build-time entry points, when available. This is an inventory, not proof that 145 features work. The V29 status counts remain 145 source-implemented/existing, 23 partial, 32 unimplemented; no feature is promoted in V30.

## Actual validation in this environment
- Kotlin PSI parse: 106 application Kotlin files, zero syntax errors. This does not type-check Android or dependency APIs.
- XML parsing: 35 XML files; 13 new binding IDs checked.
- 5 source UI wiring tests passed; 70 grouped Settings routes inventoried.
- 38 Python server tests, 13 desktop regression tests, 29 transition/DSP assertions passed.
- APK build attempted with `./gradlew --offline :app:assembleFossDebug :app:assembleFullDebug`. Failed before configuration: Gradle 8.13 distribution unavailable, `UnknownHostException: services.gradle.org`. No Android SDK/emulator installed here. APK, device UI and live provider API testing were not performed. No claim of end-to-end completion.

## Windows setup and device acceptance
1. Extract V30 into a new directory; open the folder containing `settings.gradle.kts` in Android Studio. Choose JDK 17, install API 36, sync with an internet connection. Select `fossDebug` or `fullDebug` in Build Variants.
2. Run `./gradlew.bat assembleFossDebug` in PowerShell (use `assembleFullDebug` for Full). Launch the app on the emulator and verify App info shows version 30.0. If an installation conflict occurs, export your library before removing a previous installation; do not delete data just to upgrade.
3. After the app is installed and permission dialogs are dismissed, optionally run `python tools/android_ui_smoke.py` from the project root with ADB on PATH. If ADB is not on PATH, supply `--adb "C:\Users\YourName\AppData\Local\Android\Sdk\platform-tools\adb.exe"`. The script tests five navigation controls, Library/Search routes and crossfade search-to-dialog navigation without changing playback options. It was not run here.
4. Import an owned local MP3: Library → Import audio. Tap it, check mini-player, Player, previous/next and seeking. Use song ⋮ to favourite/add to playlist, and verify in Library. Test queue reorder, lyrics editor, player styles, crossfade with two local tracks, and Settings search. Verify background notification/restore after reopen.
5. Test online search separately; public catalogue, AI, accounts and rooms require valid provider credentials/authorized data or configured trusted server. See the CSV for each route and earlier limitations. Test desktop player separately on each target OS. Home-screen widgets are added from the Android launcher, not a settings toggle.

The original V29 ZIP remains unchanged. Existing status classifications are carried forward and not independently re-certified. The redesign is a functional Material XML navigation pass, not a complete visual recreation of Spotify/Apple Music or a verified 145-feature release.
