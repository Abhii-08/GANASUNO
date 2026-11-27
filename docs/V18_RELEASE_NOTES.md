# GANASUNO 18.0 — partial-feature improvements
Base: GANASUNO 17.0, preserved unchanged. Six previously partial features improved (#11, #48, #122, #149, #177, #179). Three promoted to source-implemented (#11, #122, #179); three remain partial. Matrix: 114 implemented/existing source, 36 partial, 50 missing. These classifications inherit the previous release's matrix and are not a full runtime audit.

- **Endless queue (#11):** Local tracks now extend a local queue without calling online search. Artist/album/related-artist matching, saved album tags, URI access checks, duplicate and explicit-track filtering. Online queues use saved/remote online candidates. Similarity is heuristic, and stops when candidates are exhausted or 500 queued items are reached. The service will not insert unavailable local URIs.
- **Room waveform (#48):** Listen Together > Shared timestamp comments > Waveform and timestamp markers. Cached numeric summary or explicitly opt in to decoding; marker rows seek only when following is off. Private room comments remain temporary. Unsupported tracks may not decode.
- **Favourite collections (#122):** Library > Browse artists and albums > select album > Add to favourite albums; playlist menu > Add to favourite playlists. Library > Saved collections opens local favourites or online links/podcast feeds. Favourites are library references and backed up with existing metadata, not extra audio copies; album groups require saved metadata. Playlist favourites follow rename/delete.
- **Listen Together (#149):** Host speed 0.5–2× included in server state. Client projects position with bounded latency estimate, uses tighter paused seek threshold and pauses on polling failure while following. Updated server recommended; legacy server ignores speed. Approximate, foreground-only polling, not sample-accurate.
- **Desktop (#177/#179):** Optional pywebview window for the existing offline player, with mini/full window resizing and Quit. Install via `python -m pip install -r desktop/requirements.txt`; then `python desktop/launcher.py`. Browser-only mode still works. Not a separately native playback engine or packaged installer. Browser/WebView codec and persistence support vary.

## Verification actually run
- 23 standalone Kotlin assertions compiled and passed (selection, position and marker logic, album identity).
- Kotlin PSI parse checks; 31 XML resources parsed.
- Python room/playlist backend: 31 unit tests including 6 new speed tests; desktop window-control mock: 3 tests.
- Desktop JS syntax and 6 existing grouping assertions.
- No Android SDK build, emulator, live stream/room integration or native WebView GUI run in this environment. Source-level counts are not device-tested feature counts.
