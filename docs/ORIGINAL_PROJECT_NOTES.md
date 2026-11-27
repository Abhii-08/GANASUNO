# ganasuno

Chhota music app (Kotlin, Android Views, Media3). Search gaane -> play -> background playback + notification.

- App name: **ganasuno**, package: **com.mix.ganasuno**
- Gradle: **9.6.1** (wrapper included), AGP 9.3.1, JDK 17
- minSdk 26, compileSdk/targetSdk 36
- No update checks, no ads, no login.

## How it works (API info taken from SimpMusic's source)
- Search: YouTube Music innertube `search` endpoint (WEB_REMIX client, "songs" filter) - `data/YtApi.kt`
- Stream URL: BravePipeExtractor (`StreamInfo.getInfo`) - `data/Streams.kt`, HTTP layer in `data/NpDownloader.kt`
- Playback: ExoPlayer in a `MediaSessionService` - `player/PlaybackService.kt`
  (queue items use `ganasuno://play/<videoId>`; the real URL is resolved when ExoPlayer opens the item)

## Code on the Go (phone) - IMPORTANT
This app needs libraries that are NOT in Code on the Go's built-in offline repository
(media3, material, okhttp, the stream extractor). So Gradle must be allowed to download them:

1. Preferences -> **Build and Run** -> **Additional Gradle flags**
2. **Un-tick `--offline`** -> OK
3. Keep internet ON for the first build (a few hundred MB), then build again.

If you see `Could not find ...aar ... localMvnRepository`, `--offline` is still on.

Toolchain used (same as Code on the Go): Gradle 9.6.1, AGP 9.3.1, Kotlin 2.3.21.

## Build
```
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

Or push to GitHub: `.github/workflows/build.yml` builds the APK for you (Actions -> artifact).

## If the build complains about the SDK
Open `app/build.gradle.kts` and lower `compileSdk` / `targetSdk` to the platform you have installed.


## Features added so far (from the SimpMusic feature list)

**List 1 - Playback (done)**
1. Free, ad-free background playback
2. Audio quality selector (Low / Normal / High)
3. Video player up to 1080p with subtitles (CC button) - open it with the video icon in the mini player
4. Live stations (the "Live" chip)
5. Smart shuffle (never the same artist twice in a row)
6. Queue + position saved, resumes when you reopen the app
7. Preferred audio language for dubbed videos (Settings)
8. Sleep timer with fade-out (or stop after this song)
9. Silence skip
10. Volume normalize (gentle +3 dB loudness boost)

Settings are behind the sliders icon at the top right.
