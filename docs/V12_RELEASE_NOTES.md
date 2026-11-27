# GANASUNO 12.0 — finish the two remaining entries from batch 3

Base: 11.0. Batch 2 was already marked 10/10 complete. This release implements the two remaining partial entries from batch 3 (37 and 62), rather than redoing batch 2. Provisional catalogue counts: 90 source-marked complete (80 added source + 10 existing source), 60 partial, 50 not implemented. Earlier catalogue rows are carried forward, not independently verified.

## Changes
- #37: Advanced tools → Audio effects → Reverb preset. Seven software profiles (three rooms, two halls, plate, cathedral), feedback combs, all-pass diffusion and adjustable wet mix. Cathedral nominal decay time is six seconds. Integrated into existing Media3 PCM processor after EQ and stereo/delay. Works on supported decoded mono/stereo PCM16, 8–192 kHz; unsupported formats bypass. No hardware reverb dependency. Reverb tail is not extended after the track ends. Acoustic quality, performance and Android playback are not verified.
- #62: Song → Lyrics → Show saved lyrics; More lyrics tools → landscape fullscreen. Shared timed lyric panel uses actual enhanced-LRC start times, explicit end markers when present, and next-word/next-line boundaries to progressively brighten graphemes. If no final boundary is available the final word lights immediately. Plain and line-only lyrics fall back to plain/line display; no invented word timestamps. Updates approximately every 33 ms while the dialog is open; playback seek, pause and timing offset are respected. Tap a timed line to seek. This is an original Apple-inspired karaoke design, not a pixel-identical reproduction. Android UI, multilingual shaping and live provider responses remain unverified.

## Tests performed
- 28 standalone compiled Kotlin assertions for software reverb impulse responses, preset differences, cathedral tail, mix=0, streaming chunk continuity, stereo isolation, silence, word progression, end timestamps, seeks, fallback and UTF-16 surrogate-pair boundaries.
- 25 v11 standalone regression assertions passed.
- All 31 Android XML files well-formed.
- APK compilation, emulator/device testing and live playback not performed; no Android SDK/dependencies available here. Standalone tests are not Android type checking.

Reproduce the new tests from project root (Kotlin CLI and JDK required):
```
P=app/src/main/java/com/abhi/ganasuno
kotlinc "$P/player/SoftwareReverb.kt" "$P/ui/WordLrc.kt" "$P/ui/KaraokeTiming.kt" tests/V12BatchTest.kt -include-runtime -d v12-tests.jar
java -jar v12-tests.jar
```

Device acceptance: Gradle sync with JDK 17 and SDK 36; assembleDebug. Use a local mono and stereo track, switch all reverb presets and dry/wet levels; seek, pause, skip and restart. Verify no audible clipping/stalls and test device CPU use. Save enhanced LRC with explicit word timing; check progressive color, line scroll, pause/resume, backwards seek and landscape. Check plain LRC and no-lyrics fallback. Test long lines, RTL and Indic script. Previous sources unchanged except documented files; original ZIP retained.
