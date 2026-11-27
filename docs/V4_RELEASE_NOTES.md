# GANASUNO 4.0 — incremental feature expansion

This is not the completed 200-feature target. Current source-only totals: 50 implemented/existing, 66 partial, 84 not implemented. See FEATURE_STATUS_200.csv for each requested feature's current source-only status. No Android build or device run was possible here. The original 3.0 ZIP is unchanged.

## Where to use the new functionality
- Settings > Advanced tools > Audio effects: preamp attenuation/boost (boost uses hardware LoudnessEnhancer) and hardware auxiliary reverb presets/send. Device support and actual audio effect behavior must be tested. Sleep fade now respects the attenuation setting. Android dual-deck crossfade, delay and 8D audio are not implemented.
- Settings > Advanced tools > Player appearance: Classic, Expressive, Gradient and Minimal visual variants of the existing dialog, not exact reproductions of the requested player UIs.
- Song long press > Follow/unfollow artist or Private song note. Followed artists are stored locally; there is no external subscription sync. Library > Followed artists opens the list.
- Settings > Advanced tools > Import converted playlist: paste YouTube watch URLs or bare IDs (up to 500 unique). Only video IDs are imported, titles are placeholders. Export shares links, not a hosted playlist.
- Lyrics > Add or edit local LRC supports enhanced word timestamps, e.g. `[00:01.00]<00:01.00>Hello <00:01.70>world`. More lyrics tools include fullscreen text, local rating, opt-in AI translation and select-lines-to-share PNG.
- Listening analytics: heuristic listening type and PNG receipt sharing. AI listening recap and AI playlist suggestions are in Advanced tools. AI provider selection includes OpenAI, Gemini and custom HTTPS OpenAI-compatible endpoint. Enter a valid API model and key per request; no key is persisted by this code. Requests send selected text/aggregates to the chosen provider and may incur charges. Custom endpoint must be trusted. API/network execution was not tested.
- Advanced tools > Stop playback on exit: stop service upon removal from Recents, or keep playing.
- Advanced tools > Update checks: optional stable/nightly channel, manual or daily-on-launch check of a user-provided HTTPS manifest; see example. No server or automatic APK installation bundled. Release links require user action.
- Diagnostics: filter existing in-memory playback error codes and share filtered entries.
- Desktop companion: open desktop/index.html or Open-Windows.bat. Local audio playback, queue editing, search, mini view, seek, shuffle/repeat, speed, volume and overlapping crossfade. Browser-based preview only, not a native desktop app or online playback. No cross-device sync.

## Remaining limitations
External account services, Spotify Canvas, full Android DSP/crossfade, Google Cast, Android Auto, recognition, provider-specific charts/comments, secure synchronized rooms, native desktop apps, secure cross-device sync, translation coverage, crash reporting, and many other exact requirements remain pending. No fake credentials or completed-feature flags. Online source availability, ad-free playback and premium quality are not guaranteed. Authorized file-only download behavior retained. Source rights must be verified before redistribution; see THIRD_PARTY.md.

## Verification
32 Android Kotlin files parsed by compiler PSI with zero syntax errors (not Android type checking). 8 portable playlist/enhanced LRC standalone checks passed; 5 insights checks and 3 original LRC checks also passed. Desktop JavaScript parsed by Node. 24 XML files well formed. Gradle dependency resolution, compilation, emulator, APIs, device effects and browser audio execution not tested. Run `gradlew.bat assembleDebug --stacktrace` on Windows with JDK 17, Android SDK 36 and internet.
