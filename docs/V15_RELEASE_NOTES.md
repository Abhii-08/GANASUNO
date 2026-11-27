# GANASUNO 15.0 — Partial-feature follow-up / Batch 6 start

Base: GANASUNO 14.0. Nine existing partial entries improved: #47, #48, #58, #95, #97, #99, #121, #169, #199. #121 and #169 promoted to source implementation; seven remain partial. Carried-forward matrix: 106 source-implemented (including prior versions), 44 partial, 50 not implemented. These are catalogue source labels, not evidence of a working Android build. The original 90-partial target has 46 promoted entries, 44 remaining. Batch 4 still lacks full online waveform and upstream home fetch limit. Batch 5 still lacks comprehensive provider metadata, verified alternate versions and an official blog. This is not a claim that Batch 6 is complete.

## App controls
- Library > Browse artists and albums: offline catalogue grouping/search, artist detail with saved music and online searches. Albums group by album title AND artist (compilations may be split).
- Library > Shared playlist links, or playlist menu > Create shareable URL: opt-in public ID snapshot, browser view, import and revoke; up to 100 public tracks, local and live entries excluded. No audio uploaded. Imported names are placeholders until metadata is obtained elsewhere. No automatic sync or hosting bundled.
- Settings > More features > Batch 6 tools: current playback format, bundled release notes, waveform summary clearing, catalogue and shared playlist controls.
- Waveform player: numeric online summaries reused for 30 minutes, in memory, at most 12 entries. Analysis still opt-in for new online tracks; unsupported sources, live streams and tracks longer than 15 minutes are not supported. Clear clears stored summaries; an active analysis may finish and repopulate the cache.
- Timestamp markers: tap a note to seek or delete it individually. Private only.
- Advanced tools > AI listening recap: review actual preceding 12-month aggregate (partial first/last calendar months), top tracks/artists/albums, local metrics; explicit opt-in to send, then share or save image. Requires API credentials, no keys stored; prior incomplete history is not reconstructed.

## Server
Run the updated `server/rooms.py` on your own trusted HTTPS server as described in `server/README.md`. Playlist snapshots are unlisted, not private access-controlled; anyone with the link can view them. In-memory data expires after seven days or when the server restarts. Limit 500 snapshots. Use rate limits/access control at a hardened reverse proxy. Revoke token stays local and is not part of the shared link. Do not share a link if its playlist name is sensitive. No actual deployment or Android-server TLS integration verified.

## Verification
- 30 pure Kotlin assertions (`V15BatchTest.kt`), including input validation, grouping/search, stream format mapping and time window for recap.
- 19 Python tests (13 existing room tests + 6 new playlist tests), including local HTTP endpoint test, HTML escaping, revocation and expiry.
- 66 Android Kotlin files parsed by Kotlin PSI (syntax only); 31 XML files parsed; desktop JavaScript syntax checked.
- Android SDK unavailable here; no Gradle build, device/emulator runtime, live music provider or external AI API testing. No guarantee that all previous code compiles. Previous ZIP unchanged.
