# GANASUNO 2.0 — incremental Android release

This is a partial implementation of the supplied 200-feature catalogue, not a complete 200-feature product. `FEATURE_STATUS_200.csv` records each request individually; statuses describe code presence, not runtime verification.

## Using the new functionality
- Home, Library and History are chips at the top of the existing screen. Long-press any track for queue, favourites, playlist, sharing and lyrics actions.
- Tap the mini-player artwork/title to open the expanded Now Playing dialog. Swipe the mini artwork to change tracks. Use Settings > Playback controls to set repeat, shuffle, speed, pitch, or edit the queue.
- Library > Import local audio opens Android's document picker. Only files you select are used; persistent content URI permissions allow subsequent playback. The app does not download YouTube media. Removing a library item does not delete the original file. If a file is moved/deleted or restored on another device, reimport it.
- Library > Playlists creates, renames, deletes playlists. Long-press an item to add/remove it. Library search covers local playlists, favourites and imported audio.
- Lyrics: long-press a song or tap Lyrics in Now Playing. Fetch exact title/artist matches from LRCLIB or paste your own LRC/plain text. LRC lines follow the current playback position, with manual timing offset. LRCLIB API: https://lrclib.net/docs . Availability depends on the provider. No lyrics are fetched unless requested.
- Settings > Equalizer uses real Android audio session effects. Band count and availability depend on the device. Device audio effects opens the system panel where supported.
- History samples actual playing state every 5 seconds and periodically saves durations, including in the service. Results are approximate; recent unsaved samples may not appear immediately and history is bounded to 20,000 records. No external tracking SDK was added.
- Library backup exports JSON metadata, history, playlists and saved lyrics. Restore replaces the local library after confirmation. It does not include audio bytes, device permissions, passwords, or app preferences.
- Settings > Theme supports light, dark, system. Discovery settings controls country and displayed home result limit. Video enters picture-in-picture when leaving a playing video on supported devices. Android link routing depends on user/device defaults.

## Verification and limitations
- The previous Android Studio source ZIP is unchanged. This is a separate source release.
- Local JVM LRC parser test passed (see `tests/LrcParserTest.kt`). ZIP CRC and XML parsing checked.
- Android SDK and dependency cache are absent in the authoring environment. No full Gradle build, APK, emulator, device, network playback or hardware audio effects test has been completed. Existing dependency resolution and unofficial online playback may fail. This archive is a development build candidate, not a tested release.
- No fabricated accounts, access tokens, streaming entitlements, remote services or dummy UI for missing features. Desktop targets, commercial integrations, copyrighted media download, cross-device synchronization, advanced DSP and collaborative listening remain out of scope of this increment.
- Original source has no established project-wide license in the supplied ZIP. Do not assume distribution rights; obtain permission and preserve all applicable upstream licenses before redistribution.

## Device smoke test checklist
1. Sync Gradle with JDK 17 and compile SDK 36. Run `gradlew.bat assembleDebug --stacktrace`.
2. Start app on Pixel 4, grant notifications if desired; test search, audio and existing video. Test network errors and offline fallback.
3. Import a local MP3 via Library. Play offline and restart app; verify persistent URI access and queue restoration.
4. Long-press track, favourite, create a playlist, add and remove a track; verify after restart.
5. Queue two or more tracks; move, delete and play next. Change repeat/shuffle, speed/pitch and restart.
6. Start audio; test EQ if available. Try video picture-in-picture.
7. Paste `[00:01.00]First line` and `[00:05.00]Second line` as two lines of local lyrics; play track and check highlight/offset. Try LRCLIB on an exact match.
8. Play for a minute, pause; inspect History analytics. Export backup, modify a playlist, restore backup and verify. Switch theme.

Do not overwrite your working project until you have tested this archive. Report the first actual Gradle error if sync fails.
