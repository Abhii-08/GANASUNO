# V44.1 – Endless Next

## Changes
- The existing Endless artist mix setting defaults to On (explicit stored Off is respected; turn it On in Settings > Playback > Playback controls > Endless artist mix).
- PlaybackService fetches batches of up to 15 new tracks when 5 or fewer tracks remain in the actual playback order. Fetches run off the main thread, use the existing search service, prefer matching/related artists and fall back to broader search or saved music. Offline queues remain local; live stations and synchronized listening rooms are excluded.
- All in-app Next entry points (mini-player, swipe, long press and Classic Player) use the service's Next handler. At the last item, a pending Next advances once when new tracks arrive; rapid repeated pending taps coalesce. Failed lookups can be retried, with a 15-second automatic retry cooldown. Queues are checked for stale results and permitted tracks.
- The old 500-item stop is removed. Sequential playback keeps up to 50 preceding tracks on refill and protects future entries. Shuffle also prunes eligible older consumed items on large queues. Recent 150 track IDs and active queue IDs are excluded from recommendations; repeats are possible after older IDs age out. Saved playlists and library are unchanged. Repeat modes retain their existing behavior.
- No player DSP, UI layouts, theme or library setting action changes. Standard external media controls are not replaced; the prefetch makes Next available before queue exhaustion when possible, but only in-app Next explicitly requests a refill at the boundary.

## Verification
111 Python project checks, 38 backend tests, 3011 standalone Kotlin policy assertions (1000 simulated skips), Kotlin PSI parsing of 119 source files. These are not live Android playback tests. Android build and actual provider/network playback require device testing. Unlimited continuation is conditional on available fresh tracks; provider failures, offline exhaustion or permission restrictions can stop a queue. Retry by pressing Next again.
