# GANASUNO 43.0 — Grouped Library & Player

Base: complete V42.0 source. Version code 106. No data migrations, player engine or storage changes.

## Library
Four top-level glass cards: Collection, Playlists, Listening, Storage. Each opens a dedicated Library screen, with searchable child controls and an explicit Back to Library button. Android back returns from a song list to the active group, then to the Library root. Collection retains the seven previous collection cards; Playlists exposes all six existing playlist-hub actions; Listening groups count/time rankings, analytics periods and recaps without dropping previous history actions; Storage retains the Offline Audio and Backup and restore menus. Some advanced actions remain in their respective nested menus. Contextual song-list controls are preserved.

## Player
Visible actions: previous/play-next, favourite/queue/lyrics, shuffle/repeat, More options, Close. More options contains Song actions, Audio settings, Advanced playback, Player style, Waveform and music tags, Video, and fullscreen mode. The player-specific Song actions excludes favourite, lyrics, video and waveform because these already have dedicated routes. Advanced playback excludes repeat, shuffle and queue, since they are on the player. General song and settings menus retain their existing capabilities. Favourite uses an outlined neutral-grey vector when not saved and a filled #F4C2C2 vector when saved, reading the actual stored favourite list on opening/track change and using the toggle result immediately after a tap. Existing saved favourites are not cleared.

## Changed production files
- app/src/main/java/com/abhi/ganasuno/MainActivity.kt
- app/src/main/java/com/abhi/ganasuno/ui/GlassFeatureCard.kt
- app/src/main/res/drawable/player_heart_outline.xml (new)
- app/src/main/res/drawable/player_heart_filled.xml (new)
- app/build.gradle.kts

Previous tests whose expected version or UI routes intentionally changed were updated. New tests/test_v43_grouped_library_player.py adds five source wiring checks.

## QA
98 Python project source checks, 38 Python backend tests, 22 EffectsProcessor JVM assertions passed. 118 Android Kotlin files parsed with zero syntax errors. XML parsed, ZIP integrity checked. Android Gradle build attempted but failed downloading Gradle because services.gradle.org could not resolve; no APK or emulator verification here. Test on a phone: all four groups, back from track list, playlist import/export, offline/backup menu routes, favourite on/off and song changes, player More submenus, pause/next/previous, queue/lyrics. Source tests are not Android runtime tests.
