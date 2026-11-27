# V42.0 — Simplified navigation and settings

Based on the V41.1 complete project. Version code 105. No playback, persistence, artwork, layout or theme source changes.

- Library landing: 18 fixed cards plus individual playlist shortcuts become 11 fixed cards. Playlists remain inside the playlist hub, including details, creation, management, import, export and sharing.
- Offline Audio now opens a single menu with the saved library, local import, authorized direct audio (optional SHA-256), download preferences, deletion and integrity verification. The former direct-download route uses the existing validated optional-checksum implementation.
- Artists and Albums includes both saved collections and their search. Favourite Collections and Followed Artists remain distinct features.
- History and analytics contains recent tracks, most-listened duration, most-played counts and track list, recap, analytics/date ranges and decade information. The old listening fingerprint shortcut called the same 30-day analytics function.
- Manage library contains layout, sorting and bulk actions. The duplicate More button is hidden on the Library landing; it remains available for Library song lists.
- Backup and restore groups metadata, portable settings, audio archives and automatic snapshots.
- Search: 7 extra shortcuts become 6. The standard search field already searches songs. Videos, suggestions, online catalogue, MusicBrainz, live stations and mood discovery remain. Category cards untouched.
- Settings: 72 fixed cards become 53. Library/playlist/offline/history/backup actions move to their owning hubs. General catalogue and mood discovery live in Search; current-track metadata remains a distinct Settings action. The broad duplicate legacy tools entry is replaced by developer information. Sharing saved online playlist links remains available in the playlist hub; support links remain under release notes. Legacy implementation methods are retained. Contextual shortcuts in playback, song menus, Home discovery and nested menus may still overlap intentionally. This is not a claim of exhaustive whole-app deduplication.

QA: 93 Python source/contract checks, 38 backend tests, 118 Kotlin files parsed with zero syntax errors, XML parsed, unchanged playback/data/resource files compared to V41.1. Previous source tests were updated where navigation paths or version assertions intentionally changed; six new route checks were added. Full Gradle build attempted but distribution download failed; APK, on-device action dispatch and UI behavior unverified.

Device QA: install over V41.1 with the same signing key, open Library and verify every hub, open a song list and test More, change layout, create and open a playlist, verify offline import and authorized save, check analytics, export settings, and confirm Search and Settings still work. Verify playback and mini-player behavior before publishing.
