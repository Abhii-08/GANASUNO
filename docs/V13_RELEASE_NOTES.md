# GANASUNO 13.0 — batch 4

Base: 12.0. Ten previously partial catalogue entries improved. Five promoted to source-complete under the existing catalogue convention; five remain partial. Earlier rows carried forward without independently auditing them. These are source classifications, not verified Android behavior.

## Changes
- #43, #45, #46: four distinct player appearances, custom accessible native wavy seek control, rounded artwork and animated mesh. Designs are original and inspired by the named players, not exact copies. Android UI tests pending.
- #47: SoundCloud-inspired inline waveform for supported local files, actual decoded peaks and seek/position/markers; online content uses standard seek. Decode limited to tracks of 15 minutes, with existing time budget; stale results discarded. Partial.
- #34: Save/search/apply/delete named imported GraphicEQ profiles, stored privately. Does not fetch AutoEq's remote model catalogue; 10-band approximation. Partial.
- #91: Home result setting stops processing after the selected number of parsed rows; the server controls the response body and result availability. Partial.
- #130: separate memory/disk thumbnail clearing, bounded 32 MiB thumbnail disk cache, offline storage display. Thumbnail network responses capped at 4 MiB and decoded to approximately 1000px maximum dimension. No new media-byte cache. Partial.
- #162: read missing local release-year metadata on demand (up to 500 tracks per scan), retain manual years, capture tags when saving an owned audio copy, aggregate by decade with explicit unknown bucket. No online metadata lookup. Partial.
- #163: five listening metrics based on local samples: proportion of active days, novel tracks versus prior period, effective artist diversity from entropy, top artist time share, top track time share. No prior history means discovery unavailable.
- #166: local listener archetype rules: Devotee (concentration >=60% and <=3 artists); Deep Diver (replay >=45% or <=5 tracks); Omnivore (>=15 artists, diversity >=55%, concentration <=25%); Explorer (discovery >=60%, >=10 tracks); otherwise Regular. Original heuristic rather than a claim of exact SimpMusic scoring.

## Tests performed
- 34 compiled standalone Kotlin assertions for profile search/name validation, year parsing, decade totals, five metrics, five listener categories and progress clamping.
- 5 compiled existing insights regression assertions passed.
- 28 compiled existing v12 reverb/lyrics regression assertions passed.
- 59 Kotlin files parsed with Kotlin compiler PSI (zero syntax errors); 31 XML files parsed successfully. Syntax checking is not Android type checking.
- No Android SDK available, so Gradle build, APK, emulator, live providers and UI interactions have not been verified. Existing features are carried forward, not re-tested exhaustively.

Reproduce new tests from the project root:
```
P=app/src/main/java/com/abhi/ganasuno
kotlinc "$P/data/Batch13.kt" "$P/data/Song.kt" tests/stubs/LibraryStore.kt tests/V13BatchTest.kt -include-runtime -d v13-tests.jar
java -jar v13-tests.jar
```

## Android acceptance checklist
1. Open project with JDK 17 and SDK 36; run `gradlew.bat assembleDebug`.
2. Play a local track, switch each appearance via Advanced tools > Player appearance; check seek, transport, lyric and queue actions. Switch tracks rapidly and ensure no stale waveform; test online fallback. Test dark/light and RTL.
3. In 10-band EQ save two named profiles, search, apply and delete; restart to verify persistence.
4. Select the home result limit; verify no more than selected count shown.
5. Open storage overview and separately clear thumbnail RAM/disk; confirm owned audio stays intact. Test offline thumbnail retrieval and disk size bound.
6. Scan missing local year tags and confirm manual tags remain unchanged. Check decade unknown bucket and analytics with controlled listening data.
