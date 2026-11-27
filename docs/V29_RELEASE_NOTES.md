# GANASUNO 29.0 — Android transitions

Three existing partial features now have Android source implementations (#20, #22, #23). The feature report is a **source-level classification**, not a verified working-app count. 145 source-implemented/existing, 23 partial and 32 missing, as carried forward from V28. Prior feature classifications have not been independently audited.

## Usage
Settings → Playback controls → Crossfade and DJ transitions. Choose duration 0–15 seconds; default is off. Keep same album continuous defaults to on; DJ sweeps default to off. Use two seekable audio tracks, normal playback speed/pitch, skip silence off and no active sleep timer or room session. The next item is prepared near the end of the current item, then two Media3 players overlap with equal-power gains. The primary media-session player retains the queue and takes over the incoming track at an estimated position. Buffering and device output changes can cause an audible gap, jump or failed handoff. Non-seekable/live tracks, manual seeks, pauses and unsupported situations fall back to ordinary playback. Additional decoding, network data and battery use may occur. Do not treat the experimental option as production-ready until device tested.

DJ transitions use a per-deck first-order low/high-pass filter on supported PCM16 mono/stereo audio, not beat matching or AutoMix. Same-album detection uses album + artist metadata (or saved album tags); compilations with inconsistent artists may not match. This option prevents overlap; it does not guarantee gapless playback. The native desktop player and browser player remain available.

## Validation
- 29 standalone Kotlin transition/DSP assertions compiled and passed (gain power, cutoff trends, album comparison, eligibility, and synthesized signal attenuation).
- 104 Android Kotlin files parsed with Kotlin PSI (syntax only, not Android type checking).
- 32 XML files parsed; 38 Python backend and 13 desktop regression tests passed.
- Android SDK and device unavailable: APK compilation, actual dual-player audio handoff, audio focus/routing, DJ rendering and live online playback have not been verified.

## Device acceptance checklist
Build both variants with JDK 17 and SDK 36: `gradlew.bat assembleFossDebug assembleFullDebug --stacktrace`. Test 0, 1, 5 and 15 seconds; two local tracks and supported seekable online tracks; short tracks; shuffle and repeat; matching and unmatched album tags; next/previous, seek and queue editing during overlap; pause, Bluetooth disconnect, call/audio focus loss, buffering, sleep timer, room sync and service exit. Listen for double playback, clipping, unexpected skips, silence and incoming-track position jumps. Test default off and settings backup/restore. If issues occur, disable crossfade and report logs.
