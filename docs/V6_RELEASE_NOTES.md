# GANASUNO 6.0 — software audio analysis and EQ upgrade

Continuation of the supplied GANASUNO 5.0 Android Studio source, not a fully completed 200-feature release. Same application ID, min API 26, compile/target API 36, JDK 17. Existing external streaming integration unchanged.

## Where to find the new features
- Settings → More features (v6) → 10-band software EQ. Independent software peaking filters (31 Hz to 16 kHz), persistent gains, Flat/Bass boost/Vocal clarity/V shape presets, and negative headroom. Switch off hardware EQ if stacking is unwanted. Operates only on PCM16 mono/stereo in the main Media3 player, not separate video playback. Upper bands omitted if above the safe frequency for the sample rate. Import a trusted GraphicEQ text export, interpolated to ten bands; this is not exact AutoEq parametric conversion and no headphone model search is included. Settings transfer includes these options.
- Library → Offline audio → long-press song → Analyze audio and waveform; also available for current local track under More features and Now Playing. Android MediaExtractor/MediaCodec decodes local user-authorized files; waveform represents decoded samples, not fabricated bars. Analysis limits: 15-minute files, two-minute processing deadline, supported PCM16/float output. Tempo and major/minor key are estimated from the first 90 seconds using onset correlation and pitch-class energy; half/double-tempo errors and uncertain keys are possible. Waveform tap and separate slider seek the current track; use Play this track to activate it. Private timestamp notes and quick reactions persist in the local library. No social comment upload. Codec/sample rate/channels and source bitrate (if available) are shown.
- Settings → More features (v6) → Listening fingerprint (30-day analytics). Five defined local metrics are shown; discovery compares with the preceding period, not lifetime novelty. No fabricated data or play counts.
- Song actions → Set release year; imported local audio can use embedded year metadata. Settings → More features (v6) → Music by decade. Unknown dates remain unknown. Decade totals use the actual local listening-time history.
- Lyrics → More lyrics tools → Translate with AI / Open saved translation. Translation text is saved locally (first 4000 characters) and can be rated helpful/not helpful or cleared. Existing user-supplied API key flow applies. No provider feedback or credentials saved.
- Desktop companion: check DJ filter sweep with nonzero crossfade. Web Audio low-pass on outgoing deck and high-pass on incoming deck; reset when stopped/completed. Browser only; not Android AutoMix.

## Verification
- Kotlin PSI syntax parsing: 43 app Kotlin files, no syntax errors (not Android type checking).
- Standalone JVM V6CoreTest: 12 checks passed (flat EQ, headroom, band boost, stereo separation, profile interpolation and validation, silence, 120 BPM synthetic rhythm, normalized waveform and duration, C-major synthetic chord, fingerprint calculation and empty data). Test inputs are synthetic, not real-music benchmark measurements.
- Existing Insights regression test: 5 checks passed after adding the fingerprint dependency.
- XML well-formedness: 26 manifest/resource files; Node syntax for both desktop JS files.
- Android SDK/Gradle APK compilation, emulator, codec/device compatibility and live external APIs are not verified in this environment. Prior version test reports are retained as historical reports, not fresh tests of every feature. Source-only status is in FEATURE_STATUS_200.csv.

Run standalone test at project root:
```
P=app/src/main/java/com/abhi/ganasuno
kotlinc "$P/player/TenBandDsp.kt" "$P/data/AudioAnalysis.kt" "$P/data/Fingerprint.kt" "$P/data/Song.kt" tests/stubs/LibraryStore.kt tests/V6CoreTest.kt -include-runtime -d v6-test.jar
java -jar v6-test.jar
```

Still unfinished: account sync, Cast, Android Auto, native desktop, song recognition, automatic Android crossfade, Listen Together, external service integrations and others in the catalogue. No keys or server credentials bundled. Original app license remains unverified; review source attribution before distribution.
