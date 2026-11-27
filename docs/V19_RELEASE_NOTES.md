# GANASUNO 19.0 — privacy editions and audio analysis

Base: GANASUNO 18.0, unchanged. Six previously partial entries upgraded (#4, #47, #57, #186, #189, #190). One promoted to source-implemented (#189, separate edition functionality); five remain partial. Inherited matrix: 115 implemented/existing source, 35 partial, 50 missing. This is not a full runtime audit.

- **#189:** `foss` and `full` Gradle product flavors. FOSS-named/no-telemetry edition does not compile in the crash submission transport; Full includes it. Neither edition sends automatic crash telemetry. Both use the same application ID so cannot be installed side-by-side. Source licensing and third-party redistribution obligations have not been established; the flavor name is not a licensing claim.
- **#190:** Settings > More features > Developer diagnostics > Previous crash report. Review, keep, delete, or share a restricted local summary. Full can also explicitly send a sanitized message event to a trusted Sentry project with a user-entered DSN. No DSN stored, no automatic collection, no tracking identifiers, no background retry. HTTPS only, redirects disabled. Sentry can see the network address. Transport follows the documented envelope format (https://develop.sentry.dev/sdk/foundations/envelopes/). Not the full Sentry Android SDK; live ingest unverified.
- **#47/#57:** Song actions > Analyze audio and waveform, or More features > Current track waveform and music tags. Explicit confirmation for network decode; supported seekable streams only, up to 15 minutes. Numeric summary cached briefly; saved manual BPM/key/scale tags stored with library metadata and distinctly labelled as user annotations. Automatic estimates use the first 90 seconds and only major/minor keys. Unsupported audio remains unavailable.
- **#4:** Authorized direct audio save-and-play displays approximate file bitrate, using reported metadata or file size divided by duration. No source bitrate guarantee; download only audio the user owns or has permission to save.
- **#186:** Both Arabic and Hebrew resource files now cover all 40 declared base strings. Programmatically constructed English dialogs remain untranslated. RTL visual testing is pending.

## Actual checks
- 25 standalone Kotlin assertions (DSN validation, safe crash summaries, bitrate arithmetic, tempo and key validation).
- Kotlin PSI parser: 75 source files across both flavors, zero syntax errors. This does not establish Android type correctness or Gradle compatibility.
- 31 XML files parsed; Arabic/Hebrew base string key coverage verified.
- 31 Python backend unit tests passed. No changes to existing desktop or backend features.
- Android SDK, APK build, emulator, online decoding, Sentry live delivery and RTL visual tests not run.

## Build
In Android Studio choose `fossDebug` (recommended) or `fullDebug` in Build Variants. JDK 17, SDK 36, minSdk 26. Command on Windows: `gradlew.bat assembleFossDebug assembleFullDebug --stacktrace`. APK outputs are `app/build/outputs/apk/foss/debug/app-foss-debug.apk` and `app/build/outputs/apk/full/debug/app-full-debug.apk` after a successful build.
