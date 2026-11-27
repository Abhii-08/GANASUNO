# GANASUNO 31.0 — reference-inspired Search and player dock

Based on the user-supplied mobile screenshot: a large Search heading, compact rounded search field, frequent search cards, two-column coloured genre tiles, a multicolour player dock and three bottom navigation destinations.

## Changes
- Search landing page is rendered by `SearchBrowseView.kt`. Twelve category tiles execute existing music search. Three most frequent retained queries are ordered by a local count; ties preserve recency. Previous history without counts starts at one. Search history clear also removes counts. Real saved/listened artwork is shown when the search exactly matches a saved song or artist, otherwise a neutral music placeholder appears. No fictitious artist/history records or copied reference artwork are included.
- The existing search result list, playback queue, song action menu, advanced catalogue, lyrics, account integrations and grouped settings remain available.
- Bottom destinations: Listen Now (Home), Search, Library. Settings moved to the upper right; tap playing artwork/title/artist to open the fullscreen player. Previous, play/pause, next and seek remain in the mini-player. Video moved from the mini-player to the fullscreen player.
- Black surface and pink/red accent in dark theme. Dark is the default for new installs; existing explicit light/system/palette preferences remain intact. Dock is a static multicolour gradient, not live blur or sampled artwork.
- Version 31.0, versionCode 34. Base package, SDK, JDK, build variants and 200-feature source classifications unchanged.
- Search query artwork and category results are dependent on available local metadata and network access. Category tiles run searches, not curated official editorial charts.

## Checks
28 Python project tests (including 5 new static UI-wiring tests), 38 Python backend tests, 39 main XML files parsed, Kotlin PSI syntax inspection. Static checks do not verify Android type linking, layout rendering or playback. Android SDK/Gradle distribution unavailable in this environment; install/run verification is still required.

## Open on Windows
Extract to a new folder, open the GANASUNO-31.0 root containing settings.gradle.kts in Android Studio, select JDK 17, use API 36 SDK, sync and build `gradlew.bat :app:assembleFossDebug`. Run on the emulator and tap Search. If previously saved theme is light, switch Settings > Theme to Dark. Play a track to see the full dock. Do not overwrite an open old project.
