# GANASUNO 41.1 — Smart loading and mini-player spacing

Base: V41.0 Titanium Grey Edition. Application ID and signing configuration unchanged; versionCode 104, versionName 41.1.

## Root cause and change

V41 rendered the mini-player spinner whenever Media3 was in STATE_BUFFERING. The existing restoreQueue() calls prepare() to preload a saved queue without calling play(), so buffering alone did not mean the user intended to listen. The new pure MiniPlaybackUi policy requires a current item, playWhenReady, STATE_BUFFERING, and no player error. A pause icon appears during requested buffering so that the user can cancel it. The UI observes onPlayWhenReadyChanged as well as existing playback-state events, handles errors immediately, and hides the spinner when the queue/controller is absent or the activity stops. Queue restoration and the playback engine are unchanged. Play requests from notification or external media controls are also recognized through the same Media3 state. Spinner tint is #8E8E93 in both themes.

The mini-player Play and Next controls have a 6dp start margin. The Play margin is removed when Previous is hidden by the existing compact-screen policy; Next uses 4dp on screens below 360dp. Existing 48dp buttons remain unchanged in size. No changes to the full player or navigation.

## Modified files
- MainActivity.kt: state synchronization, tint, adaptive margins.
- ui/MiniPlaybackUi.kt: new pure presentation policy.
- res/layout/activity_main.xml: initial spinner colour and control margins.
- app/build.gradle.kts: version increment.
- tests/V411MiniPlaybackUiTest.kt and tests/test_v411_mini_player.py: new tests.
- Three prior static version assertions updated; README and this report.

## Actual verification
- 87 project Python checks passed (source checks, not Android instrumentation).
- 13 Kotlin/JVM mini-player policy scenarios passed.
- 22 EffectsProcessor JVM regression assertions passed using the existing Media3 test double.
- 38 backend Python tests passed.
- 118 Kotlin source files parsed with zero PSI syntax errors; 98 XML files parsed. This is not a full Android type/resource compilation.
- All existing player/ source files byte-identical to V41.
- Gradle assembleFossDebug attempted; wrapper download blocked by UnknownHostException: services.gradle.org. No APK or emulator verification in this environment.

## Device QA after build
1. Pause a song, close and reopen the app; restored queue should not show a spinning indicator even during preparation.
2. Play a track that requires buffering. The spinner should appear only during requested buffering; Play changes to Pause. Once playback starts, the spinner disappears.
3. Pause during buffering; spinner disappears even if the underlying player continues preparing. Resume and test an actual rebuffer.
4. Test playback errors and empty queue; spinner should disappear. Test notification play/pause as well.
5. Confirm Previous/Play/Next spacing at 380dp and above, and on compact screens. Existing narrow layout may hide Previous. Confirm long titles remain usable, RTL, light/dark themes and no overlapping controls.
6. Recheck normal online/local playback, header flip, navigation and playlists.

Windows: `./gradlew.bat :app:clean :app:assembleFossDebug` (PowerShell also accepts `.\gradlew.bat`). Do not uninstall or clear data; install over the previous build with the same signing key and a lower installed versionCode.
