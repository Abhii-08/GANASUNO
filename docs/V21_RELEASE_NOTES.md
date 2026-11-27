# GANASUNO 21.0 — source upgrade

Requested target: complete 20 of the 25 remaining partial features from V20. Actual result: 9 feature areas improved, 3 advance to source-level implementation for the documented scope and 6 remain partial. Previous classifications are inherited, not independently re-audited. 128 implemented/existing source, 22 partial, 50 not implemented. The target of 20 was **not achieved**. No Android build or real device testing was possible in this environment.

| PDF ID | Result | Scope |
|---|---|---|
| 47 | Partial | Actual waveform drag preview and seek on release, marker taps distinct from dragging; live/unsupported tracks still fall back to slider. |
| 54 | Partial | User-imported animated WebP and GIF, API 28+, no automatic provider art. |
| 60 | Partial | MusicBrainz recording candidates with user review; no verified direct audio mapping. |
| 85 | Source | Recent albums, singles and EPs, artist-specific or global metadata search, date filter and bounded results. |
| 95 | Partial | Artist about, recording and release listing, credited appearances, separate artist-name public video search; no verified mapping of channel to artist or comprehensive discography. |
| 97 | Source | Online release editions and multi-disc tracks, local detail screens, public playlist details. Track selection searches the existing player; does not invent a streaming ID. |
| 99 | Source | Central multi-category catalogue menu (songs, artists, releases, playlists and videos); MusicBrainz and official YouTube metadata, not an integrated authenticated YouTube Music library. |
| 194 | Partial | Bounded nested-JAR notice scanning in optional Gradle task; actual dependencies and redistribution licenses unverified. |
| 198 | Partial | Imported-art storage count/size/clear alongside thumbnail cache; Spotify Canvas not implemented. |

## Use
- Settings > More features (v5–21) > MusicBrainz · songs, artists and albums. Supply a real application maintainer email, used only during the current activity session in MusicBrainz User-Agent. It is transmitted to MusicBrainz, not stored by this tool. Public playlists/videos require your YouTube Data API key. The MusicBrainz integration is read-only metadata and uses at most one request per 1.1 seconds within the new client, maximum 50 rows per request, 2 MiB response cap. Existing legacy release-year lookup is independent. Verify provider terms before distributing; provide a proper maintainer contact. No audio rights granted by catalogue data.
- New features (v8) > Animated artwork > Import animated GIF / WebP. Only images you have rights to use. Android 9+, 8 MiB per image, 64 MiB total. Settings > Storage and backups > Offline storage > Thumbnail storage and cache > Manage imported animated covers. Deleting them does not delete audio.
- Waveform view: drag horizontally and release to seek, or tap a marker to inspect where marker action is configured. Accessibility slider remains available. Online analysis may use network data and fail.
- Optional `./gradlew :app:generateDependencyInventory` scans actual runtime archive notices including nested JARs within limits. Rebuild afterward. This is not a legal compliance audit; do not publish until provenance and licenses are checked.

## Verification
- Kotlin PSI: 82 Kotlin files under app/src (including both build variants), zero syntax errors. Not Android type checking or compilation.
- Batch21 core compiled and 35 assertions passed.
- 32 XML documents parsed; desktop JS files syntax checked.
- 37 Python backend tests, 11 desktop JS assertions passed.
- No Android SDK, live MusicBrainz or YouTube API calls, device waveform gestures, WebP decoding, Gradle inventory task or APK verified. The original V20 ZIP is unchanged.

References: https://musicbrainz.org/doc/MusicBrainz_API ; https://musicbrainz.org/doc/MusicBrainz_API/Search ; https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting .
